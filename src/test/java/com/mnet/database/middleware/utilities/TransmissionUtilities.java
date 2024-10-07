package com.mnet.middleware.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.CustomerDBUtilities;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.email.EmailParser;
import com.mnet.framework.email.GmailParser;
import com.mnet.framework.email.OutlookParser;
import com.mnet.framework.middleware.UnixConnector;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.middleware.utilities.PayloadExcelUtilities.PayloadAttribute;
import com.mnet.middleware.utilities.TantoDriver.TantoDriverType;
import com.mnet.middleware.utilities.TantoDriver.TantoTransmissionType;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.patient.Patient;
import com.mnet.pojo.patient.Patient.DeviceType;

public class TransmissionUtilities {

	private TantoDriver driver;
	private static final String SHARED_FOLDER_PATH = FrameworkProperties.getProperty("SHARED_FOLDER_PATH");
	private static final String LOCAL_STORAGE_PATH = FrameworkProperties.getProperty("LOCAL_FOLDER_PATH");
	private static final String TRANSMISSION_PAYLOAD_PATH_UNITY = FrameworkProperties
			.getProperty("TRANSMISSION_PAYLOAD_PATH_UNITY");
	/**
	 * By default, following are the reports that are present forNGQ transmission
	 **/
	private static final List<String> expectedPDFs = new ArrayList<>(Arrays.asList("corvue.pdf",
			"diagnostics_summary.pdf", "directtrend_report_1year_daily.pdf", "egm01.pdf", "egm02.pdf", "egm03.pdf",
			"egm04.pdf", "episodes_summary.pdf", "epsd01.pdf", "epsd02.pdf", "epsd03.pdf", "epsd04.pdf",
			"extended_diagnostics.pdf", "extended_episodes.pdf", "fastpath.pdf", "freeze01.pdf", "heartinfocus.pdf",
			"mri.pdf", "parameters.pdf", "pdfindex.txt", "tests.pdf", "crt_toolkit.pdf"));

	String emailId = EmailParser.getMFAEmailID().toLowerCase();
	private String TRANSMISSION_MAIL_SUBJECT = FrameworkProperties.getProperty("TRANSMISSION_MAIL_IDENTIFIER");

	// To perform transmission, we update the payload excel from resource->data
	// folder and update it.
	private static final String defaultExcelLocation = System.getProperty("user.dir")
			+ FrameworkProperties.getProperty("DEFAULT_EXCEL_LOCATION");

	CustomerDBUtilities custDBUtils;

	protected EmailParser email;

	private FrameworkLog log;
	private DatabaseConnector database;
	private TestReporter report;
	private FileUtilities fileManager;
	private UnixConnector remoteMachine;

	private String defaultTransmissionType = "FUA";
	private String defaultTransmitterModel = "EX1150";
	private String defaultDriverType = "";

	public TransmissionUtilities(FrameworkLog frameworkLog, DatabaseConnector dbConnector, TestReporter testReport,
			FileUtilities FM, UnixConnector RM) {
		// webDriver = driver;
		log = frameworkLog;
		database = dbConnector;
		report = testReport;
		fileManager = FM;
		remoteMachine = RM;
		driver = new TantoDriver(log, remoteMachine, fileManager, database, report);
		custDBUtils = new CustomerDBUtilities(report, database);

		try {
			log.info("Performing setup - Creating the local folder containing the utility if not exists");
			getLatestTransmissionFiles(SHARED_FOLDER_PATH, LOCAL_STORAGE_PATH);
		} catch (Exception e) {
			log.error("Failed to create folder - " + LOCAL_STORAGE_PATH);
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}

		if (emailId.contains("@gmail.com")) {
			email = new GmailParser(log);
		} else {
			email = new OutlookParser(log);
		}
	}

	/**
	 * Cleans the setup once process is complete
	 * 
	 * @param localFolderPath
	 */

	public void cleanLocalFolder(String localFolderPath) {
		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "rmdir " + localFolderPath + " /S /Q");
		builder.redirectErrorStream(true);
		try {
			Process p = builder.start();
		} catch (IOException e) {
			log.error("Failed to delete folder");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}

	}

	/**
	 * Gets the latests code to generate transmission payload from shared folder
	 * 
	 * @param sharedFolderPath
	 * @param localFolderPath
	 */

	public void getLatestTransmissionFiles(String sharedFolderPath, String localFolderPath) {
		File f = new File(localFolderPath + "\\PayLoadGenerationUnity.py");
		if (f.exists() && !f.isDirectory()) {
			log.info("File already exists");
		} else {
			log.info("Getting latest files from shared folder");
			ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c",
					"robocopy /E " + sharedFolderPath + " " + localFolderPath);
			builder.redirectErrorStream(true);
			Process process;
			try {
				process = builder.start();
				BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
				String line;
				while (true) {
					line = r.readLine();
					if (line == null) {
						break;
					}
					log.info(line);
					System.out.println(line);
				}
			} catch (IOException e) {
				log.error("Unable to copy files from shared netwrok folder");
				log.printStackTrace(e);
				throw new RuntimeException(e);
			}
		}

	}

	/**
	 * Replace the excel in paylod folder with the updated excel
	 * 
	 * @param excelPath
	 */

	public void replaceWithUpdatedExcel(String excelPath) {
		File replacePath = new File(LOCAL_STORAGE_PATH + "Unity_Transmission_TestData.xlsx");
		Path from = Paths.get(excelPath);
		Path to = replacePath.toPath();

		try {
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
			log.info("Replaced existing excel with updated one");
		} catch (IOException e) {
			log.error("Failed to copy file");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates teh local folder destination
	 * 
	 * @param localFolderPath
	 */

	public void createLocalDestination(String localFolderPath) {
		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "mkdir " + localFolderPath);
		builder.redirectErrorStream(true);
		try {
			Process p = builder.start();
		} catch (IOException e) {
			log.error("Failed to create folder");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets the latest file from the directory
	 * 
	 * @param directoryFilePath
	 * @return
	 */

	public static String getLastModified(String directoryFilePath) {
		File directory = new File(directoryFilePath);
		File[] files = directory.listFiles(File::isFile);
		long lastModifiedTime = Long.MIN_VALUE;
		File chosenFile = null;

		if (files != null) {
			for (File file : files) {
				if (file.lastModified() > lastModifiedTime) {
					chosenFile = file;
					lastModifiedTime = file.lastModified();
				}
			}
		}
		Path path = chosenFile.toPath();
		Path fileName = path.getFileName();
		return fileName.toString();
	}

	/**
	 * Get the device info
	 * 
	 * @param directoryFilePath
	 * @return
	 */

	public String[] deviceInfo(String directoryFilePath) {
		String[] info = getLastModified(directoryFilePath).split("_");
		return info;
	}

	/**
	 * Runs the payload Generation Python Script
	 * 
	 * @param localFolderPath
	 */

	public void payloadGeneration(String localFolderPath) {
		System.out.println(localFolderPath);
		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c",
				"cd " + localFolderPath + " && python PayLoadGenerationUnity.py");
		builder.redirectErrorStream(true);
		Process p;
		try {
			p = builder.start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while (true) {
				line = r.readLine();
				if (line == null) {
					break;
				}
				log.info(line);
				System.out.println(line);
			}
		} catch (IOException e) {
			log.error("Unable to Generate Payload");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}

	}

	/**
	 * Generates Payload
	 */

	public void performTransmission() {

		log.info("Performing Payload Generation");
		payloadGeneration(LOCAL_STORAGE_PATH);
		log.info("Deleting the local folder");
		cleanLocalFolder(LOCAL_STORAGE_PATH);
	}

	/**
	 * Verify whether the transmission mail is successfully received or not. Verify
	 * CODE - 233 (Delivered)
	 * 
	 * @param deviceSerial
	 * @param deviceModel
	 * @param updatedDate
	 * @param updatedTime
	 * @return
	 */

	public boolean transmissionMailVerification(String deviceSerial, String deviceModel, String updatedDate,
			String updatedTime, int emailCount) {
		String emailContent = email.getEmailWithText(TRANSMISSION_MAIL_SUBJECT, emailCount).getMailBody();
		log.info(emailContent);
		if (emailContent != null) {
			if (emailContent.contains(deviceSerial) && emailContent.contains(deviceModel)
					&& emailContent.contains(updatedDate) && emailContent.contains(updatedTime)) {
				System.out.println("Updated Time - " + updatedTime);
				return true;
			} else {
				log.error("Following details did not matched:\nDevice Serial: " + deviceSerial + "\nDevice Model: "
						+ deviceModel + "\nDate: " + updatedDate + "\nTime: " + updatedTime);
				return false;
			}
		} else {
			log.error("Email Content was null");
			return false;
		}
	}

	/**
	 * get email subject content
	 */
	public String getEmailSubjectContent(String deviceSerial, String deviceModel, String updatedDate,
			String updatedTime, int emailCount) {
		return email.getEmailWithText(TRANSMISSION_MAIL_SUBJECT, emailCount).getSubject();
	}

	/**
	 * Method to perform transmission as per the details provided.
	 * 
	 * @implNote This method uses a default payload excel file to perform
	 *           tranmsission with only Customer ID, Device Model, Device Serial and
	 *           Transmitter serial updated. If you wan to perform transmission with
	 *           custom values, please use transmissionWithUserDefinedPayload
	 *           method.
	 */

	public boolean performTransmissionForPatient(Patient patient, Customer customer, String useDatabase,
			String driverType, String transmissionType, String transmitterModel) {

		String custID = custDBUtils.getCustomer(customer.getCustomerName()).get("customer_id");

		HashMap<String, String> payloadDataToUpdate = new HashMap<String, String>();
		payloadDataToUpdate.put(PayloadAttribute.CUSTOMERID.getRowName(), custID);
		payloadDataToUpdate.put(PayloadAttribute.DEVICEMODEL.getRowName(), patient.getDeviceModelNum());
		payloadDataToUpdate.put(PayloadAttribute.DEVICESERIAL.getRowName(), patient.getDeviceSerialNum());
		payloadDataToUpdate.put(PayloadAttribute.TRANSMIITERSERIAL.getRowName(), patient.getTransmitterSerialNum());
		payloadDataToUpdate.put(PayloadAttribute.SESSIONTIMEZONE.getRowName(), "Asia/Calcutta");

		return transmissionWithUserDefinedPayload(patient, driverType, transmissionType, transmitterModel,
				payloadDataToUpdate);
	}

	/**
	 * Perform transmission with user defined values.
	 * 
	 * @param payloadExcel - Enter user defined data into the excel and send it as a
	 *                     paramter
	 */
	public boolean transmissionWithUserDefinedPayload(Patient patient, String driverType, String transmissionType,
			String transmitterModel, HashMap<String, String> payloadExcel) {

		PayloadExcelUtilities excelEdit = new PayloadExcelUtilities(log);
		excelEdit.editCell(payloadExcel);
		excelEdit.writeDataToExcel(defaultExcelLocation);

		return performTransmission(patient, "y", driverType, transmissionType, transmitterModel);
	}

	/**
	 * Function to perform transmission if you dont want to make any changes to
	 * existing payload or dont want DB validation
	 **/
	public boolean performTransmission(Patient patient, String useDatabase, String driverType, String transmissionType,
			String transmitterModel) {

		transmissionType = transmissionType == null ? defaultTransmissionType : transmissionType;
		transmitterModel = transmitterModel == null ? defaultTransmitterModel : transmitterModel;
		driverType = driverType == null ? defaultDriverType : driverType;

		replaceWithUpdatedExcel(defaultExcelLocation);
		payloadGeneration(LOCAL_STORAGE_PATH);
		log.info("Payload file generated - " + getLastModified(TRANSMISSION_PAYLOAD_PATH_UNITY));
		String payloadFileName = getLastModified(TRANSMISSION_PAYLOAD_PATH_UNITY).toString();
		boolean transmissionProcessed = false;
		String segmentSize = "";

		boolean databaseSupport = useDatabase.equalsIgnoreCase("y");
		driver.setDatabaseSupport(databaseSupport);

		for (int i = 0; i < 3; i++) {
			TantoDriverType tantoDriverType = (driverType.equalsIgnoreCase("9.x") ? TantoDriverType.DRIVER_9X
					: TantoDriverType.DRIVER_8X);
			TantoTransmissionType tantoTransmissionType = Enum.valueOf(TantoTransmissionType.class, transmissionType);

			String requestDetails = "\n Driver type: " + driverType + "\n Transmission type: " + transmissionType
					+ "\n Device model: " + patient.getDeviceModelNum() + "\n Device serial: "
					+ patient.getDeviceSerialNum();

			if (tantoDriverType == tantoDriverType.DRIVER_9X) {
				requestDetails += "\n Transmitter model: " + transmitterModel + "\n Transmitter serial: "
						+ patient.getTransmitterSerialNum();
			}

			requestDetails += "\n Payload file: " + payloadFileName;

			report.logStep(ReportLevel.INFO, "Sending Tanto transmission with the following parameters: <textarea>"
					+ requestDetails + " </textarea>");

			if (tantoDriverType == TantoDriverType.DRIVER_9X) {
				transmissionProcessed = driver.sendTransmission(payloadFileName, tantoDriverType, tantoTransmissionType,
						transmitterModel, patient.getTransmitterSerialNum(), patient.getDeviceModelNum(),
						patient.getDeviceSerialNum());
			} else {
				transmissionProcessed = driver.sendTransmission(payloadFileName, tantoTransmissionType,
						patient.getDeviceModelNum(), patient.getDeviceSerialNum());
			}
			if (transmissionProcessed) {
				return transmissionProcessed;
			}
		}
		return transmissionProcessed;

	}

	/**
	 * Method to get the payload content in a map where key is the name of the
	 * folder and value is the list of files inside that folder.
	 */
	public static Map<String, List<String>> getPayloadContent(String payloadLocation, String payloadName,
			String customPayloadPath) {
		if (customPayloadPath != null) {
			PayloadGenerator.extractPayload(customPayloadPath);
		} else {
			PayloadGenerator.extractPayload(DeviceType.NGQ, payloadLocation);
		}
		File dir = PayloadGenerator.getExtractedPayloadDirectory(payloadName);
		Map<String, List<String>> fileMap = new HashMap<>();
		PayloadGenerator.listFiles(dir, fileMap);
		return fileMap;
	}

	/** Method to validate payload content **/
	public static boolean validatePayloadContent(String payloadLocation, String payloadName, String customPayloadPath,
			FrameworkLog log) {
		Map<String, List<String>> fileMap = getPayloadContent(payloadLocation, payloadName, customPayloadPath);
		List<String> actualPDFs = fileMap.get("pdf");
		List<String> extFiles = fileMap.get("ext");
		boolean result = false;
		for (String extFile : extFiles) {
			if (extFile.contains("gdf")) {
				result = true;
			}
			if (extFile.contains("obx")) {
				result = true;
			}
		}

		return CommonUtils.compareList(true, expectedPDFs, actualPDFs, true, log) && result;
	}

}
