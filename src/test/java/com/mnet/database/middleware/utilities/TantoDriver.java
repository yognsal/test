package com.mnet.middleware.utilities;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;

import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.APIRequest.APICharset;
import com.mnet.framework.api.APIResponse;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.middleware.UnixCommand;
import com.mnet.framework.middleware.UnixConnector;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.framework.utilities.Timeout;
import com.mnet.framework.utilities.XMLData;
import com.mnet.pojo.xml.TantoComProfileError;
import com.mnet.pojo.xml.TantoComProfileRequest;
import com.mnet.pojo.xml.TantoComProfileResponse;
import com.mnet.pojo.xml.TantoPatientProfileError;
import com.mnet.pojo.xml.TantoPatientProfileRequest;
import com.mnet.pojo.xml.TantoPatientProfileResponse;
import com.mnet.pojo.xml.tanto.Switch;

import io.restassured.http.ContentType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

/***
 * Simulates Tanto driver requests for profile exchange / transmission uploads.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class TantoDriver {
	
	private FrameworkLog log;
	private UnixConnector remote;
	private FileUtilities fileUtility;
	private TestReporter report;
	private DatabaseConnector database;
	
	@Setter
	/**Set to false to disable ddt_version_id update / database validation*/
	private boolean databaseSupport;
	
	/**Path on remote machine where request file / output of Tanto commands is copied to.*/
	private static final String TANTO_REMOTE_PATH = FrameworkProperties.getProperty("TANTO_REMOTE_PATH");
	
	/**Path on remote machine where Tanto 8.x driver resides. @implNote /var/tanto/bin*/
	private static final String TANTO_8X_DRIVER_PATH = FrameworkProperties.getProperty("TANTO_8X_DRIVER_PATH");
	
	/**Path on remote machine where Tanto 9.x driver resides. @implNote /var/tanto9x/bin*/
	private static final String TANTO_9X_DRIVER_PATH = FrameworkProperties.getProperty("TANTO_9X_DRIVER_PATH");
	
	/**Primary host URL for Tanto driver commands.*/
	private static final String TANTO_DRIVER_HOST_PRIMARY = FrameworkProperties.getProperty("TANTO_DRIVER_HOST_PRIMARY");
	/**Primary host URL for Tanto driver commands.*/
	private static final String TANTO_DRIVER_HOST_SECONDARY = FrameworkProperties.getProperty("TANTO_DRIVER_HOST_SECONDARY");
	/**Port for accessing ActiveMQ web console*/
	private static final String AMQ_WEB_PORT = FrameworkProperties.getProperty("AMQ_WEB_PORT");
	
	/**Port for Tanto driver commands. @implNote 1883*/
	private static final String TANTO_DRIVER_PORT = FrameworkProperties.getProperty("TANTO_DRIVER_PORT");
	
	/**Determines whether root privileges are required to run Tanto driver commands in environment.*/
	private static final boolean TANTO_RUN_AS_ROOT = Boolean.parseBoolean(FrameworkProperties.getProperty("TANTO_RUN_AS_ROOT"));
	
	/**Local path where transmission files are retrieved from (payload generation utility or equivalent)*/
	private static final String TRANSMISSION_PAYLOAD_PATH_UNITY = FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_UNITY");
	private static final String TRANSMISSION_PAYLOAD_PATH_MED = FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_MED");
	
	/**API used for 9.x Tanto profile decompression*/
	private static final String API_DECOMPRESS_PROFILE_URI = FrameworkProperties.getProperty("API_DECOMPRESS_PROFILE_URI");
	private static final String API_DECOMPRESS_PROFILE_BEARER = FrameworkProperties.getProperty("API_DECOMPRESS_PROFILE_BEARER");
	
	/**Relative path on remote for tanto.conf*/
	private static final String TANTO_CONF_REMOTE_DIR = FrameworkProperties.getProperty("TANTO_CONF_REMOTE_DIR");
	/**Default tanto.conf file name*/
	private static final String TANTO_CONF_DEFAULT_FILE = FrameworkProperties.getProperty("TANTO_CONF_DEFAULT_FILE");
	/**Local path for tanto.conf template (8.x driver)*/
	private static final String TANTO_CONF_LOCAL_PATH_8X = FrameworkProperties.getProperty("TANTO_CONF_LOCAL_PATH_8X");
	/**Local path for tanto.conf template (9.x driver)*/
	private static final String TANTO_CONF_LOCAL_PATH_9X = FrameworkProperties.getProperty("TANTO_CONF_LOCAL_PATH_9X");
	/**Minimum allowable segment size for Tanto configuration (~1000 segments)*/
	private static final Integer TANTO_MIN_SEGMENT_SIZE = Integer.parseInt(FrameworkProperties.getProperty("TANTO_MIN_SEGMENT_SIZE"));
	/**Placeholder value in tanto.conf template for segment size*/
	private static final String REPLACE_SEGMENT_SIZE = "<segment_size>";
	
	/**Timeout (in ms) to wait for ETL to finish processing transmission before querying database.*/
	private static final long ETL_PROCESSING_TIMEOUT = Long.parseLong(FrameworkProperties.getProperty("ETL_PROCESSING_TIMEOUT"));
	/**Thread sleep interval while waiting for ETL processing completion*/
	private static final long ETL_SLEEP_INTERVAL = 4000L;
	
	/**String immediately preceding XML content in Tanto driver response*/
	private static final String TANTO_RESPONSE_START_8X = "resp = [";
	private static final String TANTO_RESPONSE_START_9X = "encodedContent\":\"";
	
	/**String immediately following XML content in Tanto driver response*/
	private static final String TANTO_RESPONSE_END_8X = "resp_len";
	private static final String TANTO_RESPONSE_END_9X = "\",\"signature";
	
	/**Placeholder value in SQL query for device_serial_num*/
	private static final String REPLACE_DEVICE_SERIAL = "<device_serial_num>";
	/**Placeholder value in SQL query for ddt_version_id*/
	private static final String REPLACE_DDT_VERSION = "<ddt_version_id>";
	/**Placeholder value in SQL query for customer_application_id*/
	private static final String REPLACE_CUSTOMER_APPLICATION = "<customer_application_id>";
	
	/**SQL query to fetch ddt_version_id and customer_application_id for given device serial*/
	private static final String SQL_DDT_LOOKUP = 
			"select ddt_version_id, ca.customer_application_id from customers.customer_application ca " +
			"join patients.customer_application_patient cap on ca.customer_application_id = cap.customer_application_id " +
			"join patients.patient_device pd on cap.patient_id = pd.patient_id " +
			"where pd.device_serial_num = '" + REPLACE_DEVICE_SERIAL + "'";
	/**SQL query to set ddt_version_id for given customer_applicaton_id*/
	private static final String SQL_DDT_UPDATE = 
			"update customers.customer_application set ddt_version_id = '" + REPLACE_DDT_VERSION + "'" +
			"where customer_application_id = '" + REPLACE_CUSTOMER_APPLICATION + "'";
	
	private static final String UNITY_DDT_VERSION = "31";
	private static final String MED_DDT_VERSION = "32";
	
	private String localPath;
	private String tantoDriverHost;
	
	@NoArgsConstructor @AllArgsConstructor
	public enum TantoProfileType {
		/**IMD-based ComProfile request (contains device model & serial)*/
		ComProfile_IMD("ComProfile"),
		/**Transmitter-based ComProfile request (omits device model & serial)*/
		ComProfile_Transmitter("ComProfile"),
		IProfile,
		PProfile,
		TProfile,
		invalid;
		
		private String profileName;
		
		@Override
		public String toString() {
			return (this.profileName == null) ? this.name() : this.profileName;
		}
	}
	
	@AllArgsConstructor
	public enum TantoDriverType {
		DRIVER_8X("8.x"),
		DRIVER_9X("9.x");
		
		private String name;
		
		/**
		 * Returns a string representative of this enum object, as used in test data.
		 * @return "8.x", "9.x"
		 **/
		@Override
		public String toString() {
			return this.name;
		}
	}
	
	/**Represents PayloadProfile attribute in Tanto response XML.*/
	public enum TantoPayloadProfileType {
		COMPROFILE("ComProfile"),
		SYSTEM_DATA("SystemData"),
		FOLLOW_UP("Follow-up"),
		DEVICE_CHECK("Device_Check"),
		ALERT_CONTROLS("Alert_Controls"),
		GDC("GDC"),
		MAINTENANCE("Maintenance"),
		MED("MED"),
		UNPAIRED("Unpaired"),
		SPARE("Spare");
		
		private final String name;
		
		private TantoPayloadProfileType(String attributeName) {
			name = attributeName;
		}
		
		/**Returns a string representative of this enum object, as used in XML declaration.*/
		@Override
		public String toString() {
			return this.name;
		}
	}
	
	/**Represents nested attributes available under PayloadProfile in Tanto response XML.*/
	public enum TantoAttributeCategory {
		SYSTEM_INFORMATION("SystemInformation"),
		GENERATE_SCHEDULE("GenerateSchedule"),
		UPLOAD_SCHEDULE("UploadSchedule");
		
		private final String name;
		
		private TantoAttributeCategory(String attributeName) {
			name = attributeName;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
	}
	
	/**Represents transmission type used for Tanto command.
	 * @implNote DevMED is sent as MED. Distinction is for setting ddt_version_id at runtime.*/
	public enum TantoTransmissionType {
		FUA,
		FUP,
		FUP_ER,
		MED,
		DevMED,
		Maintenance,
		BVVI,
		LOCK,
		FUD,
		FUU,
		invalid
	}
	
	/***
	 * Convenience constructor for Tanto profile requests.
	 * Excludes database setup and validation used for transmissions.
	 */
	public TantoDriver(FrameworkLog frameworkLog, UnixConnector remoteMachine, 
			FileUtilities fileManager, TestReporter testReporter) {
		this(frameworkLog, remoteMachine, fileManager, null, testReporter);
		
		databaseSupport = false;
	}
	
	/**
	 * Initializes Tanto driver utility for both Tanto profiles and transmissions.
	 * Supports 9.x profile response decoding + database setup and validation used for transmissions.
	 */
	public TantoDriver(FrameworkLog frameworkLog, UnixConnector remoteMachine, FileUtilities fileManager, 
			DatabaseConnector databaseConnector, TestReporter testReporter) {
		log = frameworkLog;
		remote = remoteMachine;
		fileUtility = fileManager;
		database = databaseConnector;
		report = testReporter;
		
		databaseSupport = true;
		localPath = log.getLogDirectory() + File.separator;
		tantoDriverHost = getHost();
	}
	
	/**
	 * Sends transmitter-based profile request to Tanto driver.
	 * @implNote Request XMLData should be of type TantoPatientProfile or TantoComProfile.
	 * @return XMLData object representing the XML response.
	 */
	public XMLData sendProfile(TantoProfileType profileType, TantoDriverType driverType, XMLData request,
			String transmitterModel, String transmitterSerial) {
		return sendProfile(profileType, driverType, request, transmitterModel, transmitterSerial, null, null);
	}
	
	/**
	 * Sends IMD-based profile (or 9.x) request to Tanto driver.
	 * @implNote Request XMLData should be of type TantoPatientProfile or TantoComProfile.
	 * @return XMLData object representing the XML response.
	 */
	public XMLData sendProfile(TantoProfileType profileType, TantoDriverType driverType, XMLData request,
			String transmitterModel, String transmitterSerial, String deviceModel, String deviceSerial) {
		validateProfile(profileType, driverType, request);
		
		String driverCommand = "./reqresp " + tantoDriverHost + " " + TANTO_DRIVER_PORT;
		
		String timestamp = FrameworkLog.getCurrentTimestamp();
		String requestXMLFile = "RequestXML_" + timestamp + ".xml";
		String responseContentFile = "ResponseContent_" + timestamp + ".txt";
		String responseXMLFile = "ResponseXML_" + timestamp + ".xml";
		
		String topic = "";
		
		// Handle transmitter-based profile
		if (profileType == TantoProfileType.TProfile || profileType == TantoProfileType.ComProfile_Transmitter) {
			topic = getTransmitterProfileTopic(profileType, driverType, transmitterSerial, transmitterModel);
		} else {
			topic = getIMDProfileTopic(profileType, driverType, deviceSerial, deviceModel, transmitterSerial, transmitterModel);
		}
		
		// " > " redirect driver output to file
		driverCommand += " " + topic + " " + TANTO_REMOTE_PATH + requestXMLFile + " " + TANTO_CONF_REMOTE_DIR + TANTO_CONF_DEFAULT_FILE 
				+ " > " + TANTO_REMOTE_PATH + responseContentFile;
		
		report.logStep(TestStep.builder().message("Generating Tanto request XML at path: " + localPath + requestXMLFile).build());
		fileUtility.generateXML(request, localPath + requestXMLFile);
		
		copyPayloadAndRunRemoteCommands(localPath, requestXMLFile, 
				TANTO_REMOTE_PATH, responseContentFile, 
				new UnixCommand(driverCommand, getDriverDirectory(driverType), TANTO_RUN_AS_ROOT));
		
		String fileContent = fileUtility.getFileContent(localPath + responseContentFile);
		String decodedFileContent = fileContent;
		
		report.logStep(TestStep.builder().message("Tanto driver response: <textarea>" + fileContent + " </textarea>").build());
		
		if ((driverType == TantoDriverType.DRIVER_9X) && !isComProfile(profileType)) {
			report.logStep(TestStep.builder().message("Decrypting and decoding Tanto driver response...").build());
			String encodedContent = fileUtility.getContentBetweenBounds(fileContent, TANTO_RESPONSE_START_9X, TANTO_RESPONSE_END_9X);
			
			report.logStep(TestStep.builder().message("Decrypting encrypted 9.x response via API: <textarea>" + encodedContent + "</textarea>").build());
			
			APIRequest apiRequest = new APIRequest(API_DECOMPRESS_PROFILE_URI, 
									Map.of("Authorization", API_DECOMPRESS_PROFILE_BEARER),
									encodedContent, ContentType.TEXT, APICharset.NONE);
			log.info(apiRequest.asLoggableString());
			
			String decryptedContent = RestAPIManager.post(apiRequest).toString();
			
			report.logStep(TestStep.builder().message("Decryption API response: <textarea>" + decryptedContent + "</textarea>").build());
			
			decodedFileContent = new String(Base64.getUrlDecoder().decode(decryptedContent));
			
			report.logStep(TestStep.builder().message("Decoded Tanto driver response: <textarea>" + decodedFileContent + " </textarea>").build());
		}
		
		Class<? extends XMLData> profileResponseType;
		
		if (decodedFileContent.contains("ComProfileError")) {
			profileResponseType = TantoComProfileError.class;
		} else if (decodedFileContent.contains("PatientProfileRequestFailure")) {
			profileResponseType = TantoPatientProfileError.class;
		} else {
			profileResponseType = (isComProfile(profileType)) ? TantoComProfileResponse.class : TantoPatientProfileResponse.class;
		}
	
		XMLData profileResponse = null;
		
		if ((driverType == TantoDriverType.DRIVER_9X) && !isComProfile(profileType)) {
			report.logStep(TestStep.builder().message("Extracting XML from decoded content to: " + localPath + responseXMLFile).build());
			
			profileResponse = fileUtility.extractXMLFromContent(decodedFileContent, localPath + responseXMLFile, profileResponseType);
		} else {
			report.logStep(TestStep.builder().message("Extracting XML from driver response to: " + localPath + responseXMLFile).build());
			
			profileResponse = fileUtility.extractXMLFromContent(fileContent, localPath + responseXMLFile, 
					TANTO_RESPONSE_START_8X, TANTO_RESPONSE_END_8X, profileResponseType);
		}
		
		if ((profileResponseType == TantoComProfileError.class) || (profileResponseType == TantoPatientProfileError.class)) {
			profileResponse.setFailure(true);
		}
		
		return profileResponse;
	}
		
	/**
	 * Sends IMD-based profile (or 9.x) request to Tanto driver and returns the response as a string-
	 */
	public String getResponseContent(TantoProfileType profileType, TantoDriverType driverType, XMLData request,
			String transmitterModel, String transmitterSerial, String deviceModel, String deviceSerial) {
		validateProfile(profileType, driverType, request);

		String driverCommand = "./reqresp " + tantoDriverHost + " " + TANTO_DRIVER_PORT;

		String timestamp = FrameworkLog.getCurrentTimestamp();
		String requestXMLFile = "RequestXML_" + timestamp + ".xml";
		String responseContentFile = "ResponseContent_" + timestamp + ".txt";

		String topic = "";

		// Handle transmitter-based profile
		if (profileType == TantoProfileType.TProfile || profileType == TantoProfileType.ComProfile_Transmitter) {
			topic = getTransmitterProfileTopic(profileType, driverType, transmitterSerial, transmitterModel);
		} else {
			topic = getIMDProfileTopic(profileType, driverType, deviceSerial, deviceModel, transmitterSerial,
					transmitterModel);
		}

		// " > " redirect driver output to file
		driverCommand += " " + topic + " " + TANTO_REMOTE_PATH + requestXMLFile + " tanto.conf > " + TANTO_REMOTE_PATH
				+ responseContentFile;

		report.logStep(TestStep.builder().message("Generating Tanto request XML at path: " + localPath + requestXMLFile).build());
		fileUtility.generateXML(request, localPath + requestXMLFile);

		copyPayloadAndRunRemoteCommands(localPath, requestXMLFile, TANTO_REMOTE_PATH, responseContentFile,
				new UnixCommand(driverCommand, getDriverDirectory(driverType), TANTO_RUN_AS_ROOT));

		String fileContent = fileUtility.getFileContent(localPath + responseContentFile);

		report.logStep(TestStep.builder().message("Tanto driver response: <textarea>" + fileContent + " </textarea>").build());

		return fileContent;
	}
	
	/**
	 * Convenience function for sending transmissions via Tanto driver (8.x request).
	 * For 9.x requests, use {@link sendTransmission(TantoDriverType, TantoTransmissionType, String, String, String, String, String)}
	 * @param fileName File name (including extension). File should be located at TRANSMISSION_PAYLOAD_PATH_<deviceType> in application properties.
	 * @return If database processing is enabled, returns true if ETL processes transmission within ETL_PROCESSING_TIMEOUT in application properties.
	 * If database processing is disabled, returns true when Tanto driver command finishes execution.
	 **/
	public boolean sendTransmission(String fileName, TantoTransmissionType transmissionType, String deviceModel, String deviceSerial) {
		return sendTransmission(fileName, TantoDriverType.DRIVER_8X, transmissionType, null, null, deviceModel, deviceSerial);
	}
	
	/**
	 * Sends a transmission via Tanto driver (8.x or 9.x request).
	 * For segmented transmissions, use {@link sendTransmission(TantoDriverType, TantoTransmissionType, String, String, String, String, String, Integer)}
	 * @param fileName File name (including extension). File should be located at TRANSMISSION_PAYLOAD_PATH_deviceType in application properties.
	 * @return If database processing is enabled, returns true if ETL processes transmission within ETL_PROCESSING_TIMEOUT in application properties.
	 * If database processing is disabled, returns true when Tanto driver command finishes execution.
	 **/
	public boolean sendTransmission(String fileName, TantoDriverType driverType, TantoTransmissionType transmissionType,
			String transmitterModel, String transmitterSerial, String deviceModel, String deviceSerial) {
		return sendTransmission(fileName, driverType, transmissionType, transmitterModel, transmitterSerial, deviceModel, deviceSerial, null);
	}
	
	/**
	 * Sends a transmission via Tanto driver (8.x or 9.x request).
	 * @param fileName File name (including extension). File should be located at TRANSMISSION_PAYLOAD_PATH_deviceType in application properties.
	 * @param segmentSize Segment size in tanto.conf (if null, uses default value)
	 * @return If database processing is enabled, returns true if ETL processes transmission within ETL_PROCESSING_TIMEOUT in application properties.
	 * If database processing is disabled, returns true when Tanto driver command finishes execution.
	 **/
	public boolean sendTransmission(String fileName, TantoDriverType driverType, TantoTransmissionType transmissionType,
			String transmitterModel, String transmitterSerial, String deviceModel, String deviceSerial, Integer segmentSize) {
		if (databaseSupport && !isStateTransmission(transmissionType)) {
			setDDTVersion(transmissionType, deviceSerial);
		}
		
		String driverCommand = "";
		String transmitterExtension = "";
		
		if (driverType == TantoDriverType.DRIVER_9X) {
			transmitterExtension = " " + transmitterModel + " " + transmitterSerial;
		}
		
		String topic = tantoDriverHost + " " + TANTO_DRIVER_PORT
				+ transmitterExtension + " " + deviceModel + " " + deviceSerial;
		
		if (transmissionType == TantoTransmissionType.BVVI || transmissionType == TantoTransmissionType.LOCK) {
			driverCommand = "./sendstat " + topic + " " + TANTO_CONF_REMOTE_DIR + TANTO_CONF_DEFAULT_FILE + " " + transmissionType + " " + TANTO_REMOTE_PATH + fileName;
		} else {
			String telemetryType = (transmissionType == TantoTransmissionType.DevMED) ? "MED" : transmissionType.toString();
			
			if (segmentSize == null) {
				driverCommand = "./sendtelm " + topic + " " + TANTO_REMOTE_PATH + fileName + " " + telemetryType + " " + TANTO_CONF_REMOTE_DIR + TANTO_CONF_DEFAULT_FILE;
			} else {
				driverCommand = "./sendtelm " + topic + " " + TANTO_REMOTE_PATH + fileName + " " + telemetryType + " " + getTantoSegmentedConfig(segmentSize, driverType);
			}
		}
		
		String responseContentFile = "ResponseContent_" + FrameworkLog.getCurrentTimestamp() + ".txt";
		
		driverCommand += " > " + TANTO_REMOTE_PATH + responseContentFile; // redirect driver output to file
		
		String payloadSource = ((transmissionType == TantoTransmissionType.MED) 
				? TRANSMISSION_PAYLOAD_PATH_MED : TRANSMISSION_PAYLOAD_PATH_UNITY) + fileName;
		
		report.logStep(TestStep.builder().message("Fetching payload " + payloadSource + " to " + localPath).build());
		fileUtility.copyFileToPath(payloadSource, localPath);
		
		int initialTransmissionCount = -2;
		
		if (databaseSupport) {
			initialTransmissionCount = getTransmissionCount(deviceSerial, deviceModel, transmissionType);
			report.logStep(TestStep.builder().message("Transmission count prior to upload: " + Integer.toString(initialTransmissionCount)).build());
		}
			
		copyPayloadAndRunRemoteCommands(localPath, fileName, TANTO_REMOTE_PATH, 
				responseContentFile, new UnixCommand(driverCommand, getDriverDirectory(driverType), TANTO_RUN_AS_ROOT));
		
		String fileContent = fileUtility.getFileContent(localPath + responseContentFile);
		
		report.logStep(TestStep.builder().message("Tanto driver response: <textarea>" + fileContent + "</textarea>").build());
		
		if (databaseSupport) {
			return isTransmissionProcessed(initialTransmissionCount, deviceSerial, deviceModel, transmissionType);
		}
		
		return true;
	}
	
	/**
	 * Retrieve XML element value (switches) from profile response. Can parse ComProfileError, PatientProfileRequestFailure as well.
	 * For XML attributes (GS_DateOfEvent, etc), use getAttribute(XMLData, TantoPayloadProfileType, String) instead.
	 * If element is not found, returns null.
	 * @param profileResponse XML data object representing profile response.
	 * @param subProfile Applicable subprofile (PayloadProfile) in Tanto Profile response (or ComProfile).
	 * @param elementName Element (switch) to be searched in profileResponse. Name is case-sensitive.
	 **/
	public String getXMLElement(XMLData profileResponse, TantoPayloadProfileType subProfile, String elementName) {
		if (profileResponse instanceof TantoPatientProfileResponse) {
			return getTantoProfileSwitchValue((TantoPatientProfileResponse) profileResponse, subProfile, elementName);
		} else {
			return findGetterAndInvoke(profileResponse, elementName);
		}
	}
	
	/**
	 * Retrieve XML attribute values (GS_DateOfEvent, etc) from Tanto patient profile response.
	 * For XML element values (switches) / ComProfile, use getElement(XMLData, TantoSubProfileType, String) instead.
	 * If element is not found or is not applicable to the designated attribute, returns null.
	 * @param profileResponse XML data object representing Tanto patient profile response.
	 * @param subProfile Applicable subprofile (PayloadProfile) attribute in Tanto patient profile response.
	 * @param attributeCategory Applicate attribute (SystemInformation, GenerateSchedule, etc) in Tanto patient profile response.
	 * @param elementName Element (switch) to be searched in profileResponse.
	 */
	public String getXMLAttribute(XMLData profileResponse, 
			TantoPayloadProfileType subProfile, TantoAttributeCategory attributeCategory, String attributeName) {
		if (!(profileResponse instanceof TantoPatientProfileResponse)) {
			return null;
		}
		
		TantoPatientProfileResponse patientProfileResponse = (TantoPatientProfileResponse) profileResponse;
		
		XMLData parentElement = null;
		
		switch (attributeCategory) {
			case SYSTEM_INFORMATION:
				parentElement = patientProfileResponse.getSystemData().getSystemInformation();
				break;
			case GENERATE_SCHEDULE:
				parentElement = patientProfileResponse.getSubprofileOfType(subProfile.toString()).getGenerateSchedule();
				break;
			case UPLOAD_SCHEDULE:
				parentElement = patientProfileResponse.getSubprofileOfType(subProfile.toString()).getUploadSchedule();
				break;
		}
		
		if (parentElement == null) {
			return null;
		}
		
		return findGetterAndInvoke(parentElement, attributeName);
	}
	
	/*
	 * Helper functions 
	 */
	
	/**
	 * @param payloadPath Path in local where payload / response is copied from / to (payload should already be copied to this path).
	 * @param payloadFileName Name of payload (with extension) on local.
	 * @param responsePathOnRemote File path on remote machine where driver response is saved (provided command should redirect output using > or >>)
	 * @param responseFileName Desired file name (with extension) of driver response (on local and remote).
	 * @param driverCommands List of driver commands to be run on remote machine.
	 */
	private void copyPayloadAndRunRemoteCommands(String payloadPath, String payloadFileName, 
			String responsePathOnRemote, String responseFileName, UnixCommand... driverCommands) {
		String allCommands = "";
		
		for (UnixCommand command : driverCommands) {
			allCommands += "\n" + command.getLoggableCommand();
		}
		
		report.logStep(TestStep.builder().message("Copying payload from local: " + payloadPath + payloadFileName + " to remote: " + TANTO_REMOTE_PATH).build());
		remote.copyFileToRemote(payloadPath + payloadFileName, TANTO_REMOTE_PATH);
		
		report.logStep(TestStep.builder().message("Running Tanto driver commands: <textarea>" + allCommands + " </textarea>").build());
		remote.queueCommands(driverCommands);
		remote.executeShellCommands();

		report.logStep(TestStep.builder().message("Copying driver response from remote to: " + payloadPath + responseFileName).build());
		remote.copyFileToLocal(TANTO_REMOTE_PATH + responseFileName, payloadPath + responseFileName);
	}
	
	/**
	 * Updates segment size in Tanto configuration file, uploads it to the remote destination, and returns the relative path (on remote) of the modified tanto.conf file.
	 * Throws an exception if the segment size would cause the number of segments to exceed an acceptable number (~1000).
	 */
	private String getTantoSegmentedConfig(int segmentSize, TantoDriverType driverType) {
		if (segmentSize < TANTO_MIN_SEGMENT_SIZE) {
			throw new RuntimeException("Segment size is too small - segment size should exceed " + TANTO_MIN_SEGMENT_SIZE);
		}
		
		String fileContent = fileUtility.getFileContent((driverType == TantoDriverType.DRIVER_9X) ? TANTO_CONF_LOCAL_PATH_9X : TANTO_CONF_LOCAL_PATH_8X);
		fileContent = fileContent.replace(REPLACE_SEGMENT_SIZE, Integer.toString(segmentSize));
				
		String fileName = "tanto_seg_" + FrameworkLog.getCurrentTimestamp() + ".conf";
		String localLogPath = log.getLogDirectory() + fileName;
		
		report.logStep(TestStep.builder().message("Setting Tanto segment size to: " + Integer.toString(segmentSize)).build());
		fileUtility.writeToFile(localLogPath, fileContent);
		
		String remotePath = (driverType == TantoDriverType.DRIVER_9X) ? TANTO_9X_DRIVER_PATH : TANTO_8X_DRIVER_PATH;
		remotePath += TANTO_CONF_REMOTE_DIR;
		
		report.logStep(TestStep.builder().message("Copying Tanto configuration to remote at: " + remotePath + fileName).build());
		remote.copyFileToRemote(localLogPath, remotePath);
		
		return TANTO_CONF_REMOTE_DIR + fileName;
	}
	
	private void validateProfile(TantoProfileType profileType, TantoDriverType driverType, XMLData request) {
		if (profileType == null || driverType == null) {
			String err = "Cannot process profile request - missing profile type or driver type";
			log.error(err);
			throw new RuntimeException(err);
		}
		
		if (!(request instanceof TantoPatientProfileRequest || request instanceof TantoComProfileRequest)) {
			String err = "Invalid XML data type - TantoPatientProfile or TantoComProfile expected";
			log.error(err);
			throw new RuntimeException(err);
		}
	}
	
	/**Sets ddt_version_id for patient's clinic based on transmission type.*/
	private void setDDTVersion(TantoTransmissionType transmissionType, String deviceSerial) {
		validateDatabase();
		
		String ddtLookupQuery = SQL_DDT_LOOKUP.replace(REPLACE_DEVICE_SERIAL, deviceSerial);
		
		String ddtVersion = (transmissionType == TantoTransmissionType.MED) ? MED_DDT_VERSION : UNITY_DDT_VERSION;
		
		report.logStep(TestStep.builder().message("Running database lookup query: <textarea>" + ddtLookupQuery + " </textarea>").build());
		
		List<String> queryResult = database.executeQuery(ddtLookupQuery).getFirstRow();
		
		String currentDDTVersion = queryResult.get(0);
		
		if (currentDDTVersion.equals(ddtVersion)) {
			report.logStep(TestStep.builder().message("Clinic ddt_version_id is already set to: " + ddtVersion).build());
			return;
		}
		
		String customerApplicationId = queryResult.get(1);
		
		report.logStep(TestStep.builder().message("Setting clinic ddt_version_id = " + ddtVersion).build());
		String ddtUpdateQuery = 
				SQL_DDT_UPDATE.replace(REPLACE_DDT_VERSION, ddtVersion).replace(REPLACE_CUSTOMER_APPLICATION, customerApplicationId);
		
		report.logStep(TestStep.builder().message("Running database update statement: <textarea>" + ddtUpdateQuery + "</textarea>").build());
		database.executeUpdate(ddtUpdateQuery);
	}
	
	private boolean isStateTransmission(TantoTransmissionType transmissionType) {
		switch (transmissionType) {
			case Maintenance:
			case BVVI:
			case LOCK:
				return true;
			default:
				return false;
		}
	}
	
	/**
	 * Returns true only if ETL processes transmission within ETL_PROCESSING_TIMEOUT in application properties.
	 * Fails if DatabaseConnector was not initialized in TantoDriver constructor.
	 */
	private boolean isTransmissionProcessed(int initialCount, String deviceSerial, String deviceModel, TantoTransmissionType transmissionType) {
		validateDatabase();
		
		long startTime = System.nanoTime();
		
		int currentCount = initialCount;
				
		while (currentCount == initialCount) {
			Timeout.waitForTimeout(log, ETL_SLEEP_INTERVAL);
			currentCount = getTransmissionCount(deviceSerial, deviceModel, transmissionType);
			
			if ((CommonUtils.millisFromTime(startTime)) > ETL_PROCESSING_TIMEOUT) {
				break;
			}
		}
		
		report.logStep(TestStep.builder().message("Transmission count after upload: " + Integer.toString(currentCount)).build());
		
		return (currentCount > initialCount) ? true : false;
	}
	
	private boolean isComProfile(TantoProfileType profileType) {
		return (profileType == TantoProfileType.ComProfile_IMD 
				|| profileType == TantoProfileType.ComProfile_Transmitter) ? true : false;
	}
	
	private int getTransmissionCount(String deviceSerial, String deviceModel, TantoTransmissionType transmissionType) {
		String countQuery = "select count(*) from transmissions.<table> " +
				" where device_serial_num = '" + deviceSerial + "'" + 
				" and device_model_num = '" + deviceModel + "'";
		
		if (transmissionType == TantoTransmissionType.Maintenance) {
			countQuery = countQuery.replace("<table>", "transmitter_maintenance_blob");
		} else {
			if(FrameworkProperties.getApplicationVersion().equals("d4")) {
				countQuery = countQuery.replace("<table>", "transmission_device");
			}else {
				countQuery = countQuery.replace("<table>", "transmission");
			}
		}
		
		return Integer.parseInt(database.executeQuery(countQuery).getFirstCellValue());
	}
	
	private void validateDatabase() {
		if (database == null) {
			String err = "Database not initialized for Tanto driver - use appropriate constructor";
			log.error(err);
			throw new RuntimeException(err);
		}
	}
		
	/**Determines active AMQ master broker (primary or secondary)*/
	private String getHost() {
		APIResponse checkPrimary = RestAPIManager.get(
					new APIRequest("http://" + TANTO_DRIVER_HOST_PRIMARY + ":" + AMQ_WEB_PORT, 
							Map.of(), Map.of()));
		
		if (checkPrimary.getStatusCode() == HttpStatus.SC_OK) {
			return TANTO_DRIVER_HOST_PRIMARY;
		}
		
		APIResponse checkSecondary = RestAPIManager.get(
				new APIRequest("http://" + TANTO_DRIVER_HOST_SECONDARY + ":" + AMQ_WEB_PORT, 
						Map.of(), Map.of()));
		
		if (checkSecondary.getStatusCode() == HttpStatus.SC_OK) {
			return TANTO_DRIVER_HOST_SECONDARY;
		} else {
			if (checkPrimary.getFailureException() != null) {
				log.printStackTrace(checkPrimary.getFailureException());
			}
			
			if (checkSecondary.getFailureException() != null) {
				log.printStackTrace(checkSecondary.getFailureException());
			}
			
			throw new RuntimeException("Primary / secondary AMQ brokers are not accessible");
		}
	}
	
	private String getDriverDirectory(TantoDriverType driverType) {
		return (driverType == TantoDriverType.DRIVER_8X) ? TANTO_8X_DRIVER_PATH : TANTO_9X_DRIVER_PATH;
	}
	
	private String getTransmitterProfileTopic(TantoProfileType profileType, TantoDriverType driverType, String transmitterSerial, String transmitterModel) {
		switch (driverType) {
			default:
			case DRIVER_8X:
				return transmitterModel + " " + transmitterSerial + " 0 " + profileType.toString();
			case DRIVER_9X:
				return transmitterModel + " " + transmitterSerial + " " + profileType.toString();
		}
	}
	
	private String getIMDProfileTopic(TantoProfileType profileType, TantoDriverType driverType, String deviceSerial, String deviceModel, String transmitterSerial, String transmitterModel) {
		switch (driverType) {
			default:
			case DRIVER_8X:
				return deviceModel + " " + deviceSerial + " 0 " + profileType.toString();
			case DRIVER_9X:
				if (isComProfile(profileType)) {
					return deviceModel + " " + deviceSerial + " " + profileType.toString();
				} else {
					return transmitterModel + " " + transmitterSerial + " " + deviceModel + " " + deviceSerial + " " + profileType;
				}
		}
	}
	
	private String getTantoProfileSwitchValue(TantoPatientProfileResponse profileResponse, TantoPayloadProfileType subProfile, String elementName) {
		List<Switch> switches;
		
		switch(subProfile) {
			case SYSTEM_DATA:
				switches = profileResponse.getSystemData().getControls().getAllSwitches();
				break;
			default:
				switches = profileResponse.getSubprofileOfType(subProfile.toString()).getControls().getAllSwitches();
				break;
		}
		
		if (switches == null) {
			String err = "Invalid PayloadProfile for Tanto patient profile: " + subProfile.toString();
			log.error(err);
			throw new RuntimeException(err);
		}
		
		for (Switch switchInstance : switches) {
			if (switchInstance.getName().equalsIgnoreCase(elementName)) {
				return switchInstance.getValue();
			}
		}
		
		return null;
	}
	
	/**
	 * @param parentElement XMLData class from which getElementName() is extracted.
	 * @param xmlField Name of field on which getter is to be invoked. Field name is case-sensitive.
	 * @return Value of field represented by getElementName().
	 */
	private String findGetterAndInvoke(XMLData parentElement, String xmlField) {
		Method method;
		Class<? extends XMLData> responseType = parentElement.getClass();
		
		String methodName = "get" + xmlField;
		
		try {
			method = responseType.getMethod(methodName);
		} catch (NoSuchMethodException nsme){
			String err = "No methods found in : " + responseType + " with signature " + methodName + "()";
			log.error(err);
			log.printStackTrace(nsme);
			throw new RuntimeException(err);
		} 
		
		try {
			return (String) method.invoke(responseType.cast(parentElement));
		} catch (InvocationTargetException | IllegalAccessException ie) {
			String err = "Invalid class or access modifier for class: " + responseType + " method: " + method;
			log.error(err);
			log.printStackTrace(ie);
			throw new RuntimeException(ie);
		}
	}

}
