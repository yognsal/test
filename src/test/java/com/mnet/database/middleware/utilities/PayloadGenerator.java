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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.util.SystemOutLogger;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.pojo.patient.Patient.DeviceType;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * Payload generator utility for all types of devices; Unity payload generation
 * calls can be replaced with functions from this file
 * 
 * @author NAIKKX12
 *
 */

public class PayloadGenerator {

	FrameworkLog log;
	PayloadExcelUtilities payloadUtils;
	String excelFile;
	HashMap<String, String> payloadExcel;
	FileUtilities fileUtils;

	private static final String SHARED_FOLDER_PATH = FrameworkProperties.getProperty("SHARED_FOLDER_PATH");
	private static final String SHARED_NGQ_PAYLOAD_PATH = FrameworkProperties.getProperty("SHARED_NGQ_PAYLOAD_PATH");
	private static final String LOCAL_STORAGE_PATH = FrameworkProperties.getProperty("LOCAL_FOLDER_PATH");
	private static final String LOCAL_NGQ_PAYLOAD_PATH = FrameworkProperties.getProperty("LOCAL_NGQ_PAYLOAD_PATH");

	public PayloadGenerator(FrameworkLog log, HashMap<String, String> payloadExcel, String excelFile) {
		this.log = log;
		this.excelFile = excelFile;
		this.payloadExcel = payloadExcel;
		payloadUtils = excelFile == null ? new PayloadExcelUtilities(log) : new PayloadExcelUtilities(excelFile, log);
		fileUtils = new FileUtilities(log);
	}

	public String generateNGQPayload() {
		getPayloadGenerator(DeviceType.NGQ);
		createPayloadExcel();
		replaceUpdatedPayload(DeviceType.NGQ, excelFile);
		payloadGeneration(DeviceType.NGQ);

		return fileUtils.getLastModified(FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_NGQ"));
	}

	/**
	 * Extract the payload which will be in .zip format
	 * @param deviceType - Unity or NGQ (Location of payload varies)
	 */
	public static void extractPayload(DeviceType deviceType, String payloadName) {
		String source = null;
		switch(deviceType) {
			case Unity:
				source = FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_UNITY");
				break;
			case NGQ:
				source = FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_NGQ");
				break;
		}
		source = source+payloadName;
		try {
		    ZipFile zipFile = new ZipFile(source);
		    zipFile.extractAll(FrameworkProperties.getProperty("PAYLOAD_EXTRACT_PATH"));
		} catch (ZipException e) {
		    e.printStackTrace();
		}
	}
	
	/**
	 * Extract the payload which downloaded from azure portal and will be in .zip format
	 */
	public static void extractPayload(String payloadPath) {
		String source = null;
		if (payloadPath != null) {
			source = payloadPath;
		}
		try {
			ZipFile zipFile = new ZipFile(source);
			zipFile.extractAll(FrameworkProperties.getProperty("PAYLOAD_EXTRACT_PATH"));
		} catch (ZipException e) {
			e.printStackTrace();
		}
	}
	
	/**Get the directory of the exptracted payload which was done using extractPayload method**/
	public static File getExtractedPayloadDirectory(String payloadName) {
		String[] deviceInfo = payloadName.split("_");
		String deviceSerial = deviceInfo[0];
		String deviceModel = deviceInfo[1];
		String dateTime = deviceInfo[2]+"_"+deviceInfo[3];
		dateTime = dateTime.replace(".zip", "");
		String extractedPayloadLocation = FrameworkProperties.getProperty("PAYLOAD_EXTRACT_PATH");
		extractedPayloadLocation = extractedPayloadLocation + deviceSerial + "\\" + deviceModel + "\\" + dateTime;
		System.out.println(extractedPayloadLocation);
		File dir = new File(extractedPayloadLocation);
		return dir;
	}
	
	/**List the files inside a folder passed in as parameter
	 * @implNote: This method runs recursively if there are multiple sub folders in the mentioned folder
	 */
	public static Map<String, List<String>> listFiles(File dir, Map<String, List<String>> fileMap) {
        File[] files = dir.listFiles();
        if (files != null) {
            List<String> fileList = new ArrayList<>();
            for (File file : files) {
                if (file.isFile()) {
                    fileList.add(file.getName());
                } else if (file.isDirectory()) {
                    listFiles(file, fileMap);
                }
            }
            String absolutePath = dir.getPath();
            String[] folders = absolutePath.split("\\\\");
            String folderTraversed = folders[folders.length-1];
            fileMap.put(folderTraversed, fileList);
        }
        return fileMap;
    }

	private void createPayloadExcel() {
		payloadUtils.editCell(payloadExcel);
		payloadUtils.writeDataToExcel(excelFile);
	}

	private void replaceUpdatedPayload(DeviceType deviceType, String excelPath) {

		File newFile;

		switch (deviceType) {
		case Unity:
			newFile = new File(LOCAL_STORAGE_PATH + "Unity_Transmission_TestData.xlsx");
			break;
		case NGQ:
			newFile = new File(LOCAL_NGQ_PAYLOAD_PATH + "NGQ_Transmission_TestData.xlsx");
			break;
		default:
			log.error(deviceType.getDeviceType() + " not handled");
			return;
		}

		Path from = Paths.get(excelPath);
		Path to = newFile.toPath();

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
	 * Get the latest payload generator
	 */
	private void getPayloadGenerator(DeviceType deviceType) {
		File payloadExe;
		String sharedFolderPath, localPath;

		switch (deviceType) {
		case Unity:
			payloadExe = new File(LOCAL_STORAGE_PATH + "\\PayLoadGenerationUnity.py");
			sharedFolderPath = SHARED_FOLDER_PATH;
			localPath = LOCAL_STORAGE_PATH;
			break;
		case NGQ:
			payloadExe = new File(LOCAL_NGQ_PAYLOAD_PATH + "\\PayLoadGeneration.py");
			sharedFolderPath = SHARED_NGQ_PAYLOAD_PATH;
			localPath = LOCAL_NGQ_PAYLOAD_PATH;
			break;
		default:
			log.error(deviceType.getDeviceType() + " not handled");
			return;
		}

		if (payloadExe.exists() && !payloadExe.isDirectory()) {
			log.info("File already exists");
		} else {
			log.info("Getting latest files from shared folder");
			ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c",
					"robocopy /E " + sharedFolderPath + " " + localPath);
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
				log.error("Unable to copy files from shared network folder");
				log.printStackTrace(e);
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Runs the payload Generation Python Script
	 */
	private void payloadGeneration(DeviceType deviceType) {
		ProcessBuilder builder;

		switch (deviceType) {
		case Unity:
			builder = new ProcessBuilder("cmd.exe", "/c",
					"cd " + LOCAL_STORAGE_PATH + " && python PayLoadGenerationUnity.py");
			break;
		case NGQ:
			builder = new ProcessBuilder("cmd.exe", "/c",
					"cd " + LOCAL_NGQ_PAYLOAD_PATH + " && python PayLoadGeneration.py");
			break;
		default:
			log.error(deviceType.getDeviceType() + " not handled");
			return;
		}

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
			}
		} catch (IOException e) {
			log.error("Unable to Generate Payload");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
	}

}
