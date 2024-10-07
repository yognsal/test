package com.mnet.middleware.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.APIRequest.APICharset;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.middleware.UnixConnector;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.framework.utilities.XMLData;
import com.mnet.middleware.utilities.TantoDriver.TantoDriverType;
import com.mnet.middleware.utilities.TantoDriver.TantoProfileType;
import com.mnet.pojo.patient.Patient;
import com.mnet.pojo.tanto.Tanto;
import com.mnet.pojo.xml.TantoComProfileRequest;
import com.mnet.pojo.xml.TantoPatientProfileRequest;

import io.restassured.http.ContentType;

public class TantoProfilingUtility {

	private UnixConnector remoteMachine;
	private FileUtilities fileManager;
	private TestReporter report;
	private FrameworkLog log;

	private TantoDriver driver;

	/** String immediately preceding XML content in Tanto driver response */
	private static final String TANTO_RESPONSE_START = "signature\":\"";
	private static final String TANTO_RESPONSE_ENCODED_START = "encodedContent\":\"";
	private static final String TANTO_RESPONSE_START_8X = "resp = [";

	/** String immediately following XML content in Tanto driver response */
	private static final String TANTO_RESPONSE_END = "\"}";
	private static final String TANTO_RESPONSE_ENCODED_END = "\",\"signature";
	private static final String TANTO_RESPONSE_END_8X = "]";

	/**
	 * Host URL for Tanto driver commands. @implNote d2-mr-amq-l522.ad.merlin.net
	 */
	private static final String TANTO_DRIVER_HOST = FrameworkProperties.getProperty("TANTO_DRIVER_HOST");

	/**
	 * Path on remote machine where request file / output of Tanto commands is
	 * copied to.
	 */
	private static final String TANTO_REMOTE_PATH = FrameworkProperties.getProperty("TANTO_REMOTE_PATH");

	/** API used for 9.x Tanto profile decryption */
	private static final String API_DECOMPRESS_PROFILE_URI = FrameworkProperties
			.getProperty("API_DECOMPRESS_PROFILE_URI");
	private static final String API_DECOMPRESS_PROFILE_BEARER = FrameworkProperties
			.getProperty("API_DECOMPRESS_PROFILE_BEARER");

	private String localPath;
	
	private static final String defaultDriverType = "8.x";
	private static final String defaultProfileVersion = "7";
	private static final String defaultTransmitterModel = "EX1150";
	private static final String defaultTransmissionType = "";
	private static final String defaultTransmitterModelExtension = "";
	private static final String defaultTransmitterSWVersion = "EX2000 v8.8 PR_8.85";
	private static final String defaultTimeOfFollowup = "";

	public TantoProfilingUtility(FrameworkLog logger, UnixConnector remoteMachine, 
			FileUtilities fileManager, TestReporter reporter) {
		this.remoteMachine = remoteMachine;
		this.fileManager = fileManager;
		report = reporter;
		log = logger;
		driver = new TantoDriver(log, this.remoteMachine, this.fileManager, report);
		localPath = log.getLogDirectory() + File.separator;

	}

	/**
	 * Return the profile response for verification purpose.
	 */
	@Deprecated
	public XMLData tantoProfileResponse(String driverType, String profileType, String profileVersion,
			String deviceModel, String deviceSerial, String transmitterModel, String transmitterSerial,
			String transmitterSWVersion, String transmissionType, String timeOfFollowup,
			String transmitterModelExtension) {
		
		driverType = driverType == null ? defaultDriverType : driverType;
		profileVersion = profileVersion == null ? defaultProfileVersion : profileVersion;
		transmitterModel = transmitterModel == null ? defaultTransmitterModel : transmitterModel;
		transmitterSWVersion = transmitterSWVersion == null ? defaultTransmitterSWVersion : transmitterSWVersion;
		transmissionType = transmissionType == null ? defaultTransmissionType : transmissionType;
		timeOfFollowup = timeOfFollowup == null ? defaultTimeOfFollowup : timeOfFollowup;
		transmitterModelExtension = transmitterModelExtension == null ? defaultTransmitterModelExtension
				: transmitterModelExtension;

		TantoDriverType tantoDriverType = (driverType.equalsIgnoreCase("9.x")) ? TantoDriverType.DRIVER_9X
				: TantoDriverType.DRIVER_8X;
		TantoProfileType tantoProfileType = Enum.valueOf(TantoProfileType.class, profileType);

		if (tantoProfileType == null) {
			report.logStep(ReportLevel.FAIL, "Invalid profile type: " + profileType);
		}

		String profileRequestDetails = "\n Driver type: " + driverType + "\n Profile type: " + profileType
				+ "\n Profile version: " + profileVersion + "\n Transmitter model: " + transmitterModel
				+ "\n Transmitter serial: " + transmitterSerial;

		if (!StringUtils.isEmpty(deviceModel)) {
			profileRequestDetails += "\n Device model: " + deviceModel + "\n Device serial: " + deviceSerial;
		}

		if (!StringUtils.isEmpty(transmissionType)) {
			profileRequestDetails += "\n Transmission type: " + transmissionType + "\n TimeOfFollowup: "
					+ timeOfFollowup;
		}

		if (tantoDriverType == TantoDriverType.DRIVER_9X) {
			profileRequestDetails += "\n TransmitterScriptSWVersion: 1 \n TransmitterScriptContentVersion: 0"
					+ "\n TransmitterModelExtension: " + transmitterModelExtension;
		}

		report.logStep(ReportLevel.INFO,
				"Sending Tanto patient profile request with the following parameters: <textarea>"
						+ profileRequestDetails + "</textarea>");

		XMLData profileRequest, profileResponse;

		if (tantoProfileType == TantoProfileType.ComProfile_IMD) {
			profileRequest = new TantoComProfileRequest(deviceModel, deviceSerial, transmitterSWVersion,
					profileVersion);
		} else if (tantoProfileType == TantoProfileType.ComProfile_Transmitter) {
			profileRequest = new TantoComProfileRequest(transmitterSWVersion, profileVersion);
		} else if (tantoDriverType == TantoDriverType.DRIVER_9X) {
			profileRequest = new TantoPatientProfileRequest(transmitterModel, transmitterSerial, transmissionType,
					timeOfFollowup, transmitterSWVersion, profileVersion, "1", "0", transmitterModelExtension);
		} else {
			profileRequest = new TantoPatientProfileRequest(transmitterModel, transmitterSerial, transmitterSWVersion,
					profileVersion);
		}

		if (StringUtils.isEmpty(deviceModel)) {
			profileResponse = driver.sendProfile(tantoProfileType, tantoDriverType, profileRequest, transmitterModel,
					transmitterSerial);
		} else {
			profileResponse = driver.sendProfile(tantoProfileType, tantoDriverType, profileRequest, transmitterModel,
					transmitterSerial, deviceModel, deviceSerial);
		}

		return profileResponse;
	}

	/**
	 * Use this utility method to get tanto profile response by accepting all
	 * mandatory and optional parameters.
	 */
	public XMLData tantoProfileResponse(Patient patient, Tanto tanto) {

		TantoDriverType tantoDriverType = (tanto.getDriverType().equalsIgnoreCase("9.x")) ? TantoDriverType.DRIVER_9X
				: TantoDriverType.DRIVER_8X;
		TantoProfileType tantoProfileType = Enum.valueOf(TantoProfileType.class, tanto.getProfileType());

		if (tantoProfileType == null) {
			report.logStep(ReportLevel.FAIL, "Invalid profile type: " + tanto.getProfileType());
		}

		String profileRequestDetails = "\n Driver type: " + tanto.getDriverType() + "\n Profile type: " + tanto.getProfileType()
				+ "\n Profile version: " + tanto.getProfileVersion() + "\n Transmitter model: "
				+ patient.getTransmitterModel().getTransModel().split("-")[0] + "\n Transmitter serial: "
				+ patient.getTransmitterSerialNum();

		if (!StringUtils.isEmpty(patient.getDeviceModelNum())) {
			profileRequestDetails += "\n Device model: " + patient.getDeviceModelNum() + "\n Device serial: "
					+ patient.getDeviceSerialNum();
		}

		if (!StringUtils.isEmpty(tanto.getTransmissionType())) {
			profileRequestDetails += "\n Transmission type: " + tanto.getTransmissionType() + "\n TimeOfFollowup: "
					+ tanto.getTimeOfFollowup();
		}

		if (tantoDriverType == TantoDriverType.DRIVER_9X) {
			profileRequestDetails += "\n TransmitterScriptSWVersion: 1 \n TransmitterScriptContentVersion: 0"
					+ "\n TransmitterModelExtension: " + tanto.getTransmitterModelExtension();
		}

		report.logStep(ReportLevel.INFO,
				"Sending Tanto patient profile request with the following parameters: <textarea>"
						+ profileRequestDetails + "</textarea>");

		XMLData profileRequest, profileResponse;

		if (tantoProfileType == TantoProfileType.ComProfile_IMD) {
			profileRequest = new TantoComProfileRequest(patient.getDeviceModelNum(), patient.getDeviceSerialNum(),
					tanto.getTransmitterSWVersion(), tanto.getProfileVersion());
		} else if (tantoProfileType == TantoProfileType.ComProfile_Transmitter) {
			profileRequest = new TantoComProfileRequest(tanto.getTransmitterSWVersion(), tanto.getProfileVersion());
		} else {
			profileRequest = new TantoPatientProfileRequest(patient.getTransmitterModel().getTransModel().split("-")[0],
					patient.getTransmitterSerialNum(), tanto.getTransmissionType(), tanto.getTimeOfFollowup(), tanto.getTransmitterSWVersion(),
					tanto.getProfileVersion(), tanto.getTransmitterScriptSWVersion(), tanto.getTransmitterScriptContentVersion(),
					tanto.getTransmitterModelExtension());
		}

		if (StringUtils.isEmpty(patient.getDeviceModelNum())) {
			profileResponse = driver.sendProfile(tantoProfileType, tantoDriverType, profileRequest,
					patient.getTransmitterModel().getTransModel().split("-")[0], patient.getTransmitterSerialNum());
		} else {
			profileResponse = driver.sendProfile(tantoProfileType, tantoDriverType, profileRequest,
					patient.getTransmitterModel().getTransModel().split("-")[0], patient.getTransmitterSerialNum(),
					patient.getDeviceModelNum(), patient.getDeviceSerialNum());
		}

		return profileResponse;
	}

	/**
	 * Return the profile response for verification purpose.
	 */

	public String getTantoResponseContent(String driverType, String profileType, String profileVersion,
			String deviceModel, String deviceSerial, String transmitterModel, String transmitterSerial,
			String transmitterSWVersion, String transmissionType, String timeOfFollowup,
			String transmitterModelExtension) {

		TantoDriverType tantoDriverType = (driverType.equalsIgnoreCase("9.x")) ? TantoDriverType.DRIVER_9X
				: TantoDriverType.DRIVER_8X;
		TantoProfileType tantoProfileType = Enum.valueOf(TantoProfileType.class, profileType);

		if (tantoProfileType == null) {
			report.logStep(ReportLevel.FAIL, "Invalid profile type: " + profileType);
		}

		String profileRequestDetails = "\n Driver type: " + driverType + "\n Profile type: " + profileType
				+ "\n Profile version: " + profileVersion + "\n Transmitter model: " + transmitterModel
				+ "\n Transmitter serial: " + transmitterSerial;

		if (!StringUtils.isEmpty(deviceModel)) {
			profileRequestDetails += "\n Device model: " + deviceModel + "\n Device serial: " + deviceSerial;
		}

		if (!StringUtils.isEmpty(transmissionType)) {
			profileRequestDetails += "\n Transmission type: " + transmissionType + "\n TimeOfFollowup: "
					+ timeOfFollowup;
		}

		if (tantoDriverType == TantoDriverType.DRIVER_9X) {
			profileRequestDetails += "\n TransmitterScriptSWVersion: 1 \n TransmitterScriptContentVersion: 0"
					+ "\n TransmitterModelExtension: " + transmitterModelExtension;
		}

		report.logStep(ReportLevel.INFO,
				"Sending Tanto patient profile request with the following parameters: <textarea>"
						+ profileRequestDetails + "</textarea>");

		XMLData profileRequest;

		if (tantoProfileType == TantoProfileType.ComProfile_IMD) {
			profileRequest = new TantoComProfileRequest(deviceModel, deviceSerial, transmitterSWVersion,
					profileVersion);
		} else if (tantoProfileType == TantoProfileType.ComProfile_Transmitter) {
			profileRequest = new TantoComProfileRequest(transmitterSWVersion, profileVersion);
		} else if (tantoDriverType == TantoDriverType.DRIVER_9X) {
			profileRequest = new TantoPatientProfileRequest(transmitterModel, transmitterSerial, transmissionType,
					timeOfFollowup, transmitterSWVersion, profileVersion, "1", "0", transmitterModelExtension);
		} else {
			profileRequest = new TantoPatientProfileRequest(transmitterModel, transmitterSerial, transmitterSWVersion,
					profileVersion);
		}

		return driver.getResponseContent(tantoProfileType, tantoDriverType, profileRequest, transmitterModel,
				transmitterSerial, deviceModel, deviceSerial);
	}

	/**
	 * Converts XML response to String
	 */
	public String convertResponseXMLToString(String xmlFilePath) {
		BufferedReader br;
		String content = null;
		try {
			br = new BufferedReader(new FileReader(localPath + xmlFilePath));
			content = br.lines().collect(Collectors.joining("\n"));
			br.close();
		} catch (IOException e) {
			String err = "File not found with file-name or I/O error: " + xmlFilePath;
			log.error(err);
			log.printStackTrace(e);
			throw new RuntimeException(err);
		}
		return content;

	}

	/**
	 * Takes the encoded response (for 9.x Driver) and return decoded response. This
	 * function is particularly for testing negative scenario where error in
	 * response is expected
	 */
	public String getDecodedResponse(String responseContentFilename) {
		String fileContent = fileManager.getFileContent(localPath + responseContentFilename);
		String encodedContent = fileManager.getContentBetweenBounds(fileContent, TANTO_RESPONSE_ENCODED_START,
				TANTO_RESPONSE_ENCODED_END);
		report.logStep(ReportLevel.INFO, encodedContent);
		if (encodedContent.isEmpty()) {
			return null;
		}
		
		APIRequest request = new APIRequest(API_DECOMPRESS_PROFILE_URI,
				Map.of("Authorization", API_DECOMPRESS_PROFILE_BEARER), encodedContent, ContentType.TEXT, APICharset.NONE);
		String decryptedContent = RestAPIManager.post(request).toString();

		report.logStep(ReportLevel.INFO, "Decryption API response: <textarea>" + decryptedContent + "</textarea>");

		String decodedFileContent = new String(Base64.getUrlDecoder().decode(decryptedContent));
		return decodedFileContent;
	}

	/**
	 * Check if response is signed or not (for 9.x Profile)
	 */

	public boolean isResponseSigned(String responseContentFilename) {
		String fileContent = fileManager.getFileContent(localPath + responseContentFilename);
		String encodedContent;

		if (!fileContent.contains(TANTO_RESPONSE_START)) {
			return false;
		}
		encodedContent = fileManager.getContentBetweenBounds(fileContent, TANTO_RESPONSE_START, TANTO_RESPONSE_END);
		report.logStep(ReportLevel.INFO, TANTO_RESPONSE_START + " " + encodedContent);
		if (encodedContent.isEmpty()) {
			return false;
		}
		return true;

	}

	/**
	 * Get the Patient Profile section from ResponseContent.txt file
	 */
	public String getPatientProfile(String responseContent) {
		String fileContent = fileManager.getFileContent(localPath + responseContent);
		String patientProfile = fileManager.getContentBetweenBounds(fileContent, "<GetPatientProfile>",
				"</GetPatientProfile>");
		report.logStep(ReportLevel.INFO, "Patient Profile Request: <textarea>" + patientProfile + "</textarea>");
		return patientProfile;
	}

	/**
	 * Replace substring with the replacement passed as parameter
	 */
	public String replaceString(String response, String firstDelimeter, String lastDelimeter, String replacement) {
		int p1 = response.indexOf(firstDelimeter);
		int p2 = response.indexOf(lastDelimeter, p1);

		if (p1 >= 0 && p2 > p1) {
			String res = response.substring(0, p1 + firstDelimeter.length()) + replacement + response.substring(p2);
			return res;
		}
		log.error("Replacement Failed");
		return null;
	}
}
