package com.mnet.mobility.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bouncycastle.util.Arrays;

import com.jcraft.jsch.Logger;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.pojo.mobility.PatientAppIdentity;
import com.mnet.pojo.mobility.PatientAppKeys;
import com.mnet.pojo.mobility.PhoneData;

import lombok.Getter;

/**
 * Provides convenience functions for common mobility operations.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class MobilityUtilities {

	private static FileUtilities fileManager;
	
	private static final Encoder ENCODER = Base64.getEncoder();
	private static final Decoder DECODER = Base64.getDecoder();
	
	private static final String MOBILITY_PROPERTIES = FrameworkProperties.getProperty("NGQ_MOBILITY_PROPERTIES_FILE");
	
	private static final String LOG_DIR = FrameworkProperties.getProperty("LOG_DIR");
	private static final String JSON_PATH = FrameworkProperties.getSystemProperty("user.dir") + File.separator
			+ FrameworkProperties.getProperty("JSON_PATH");
	
	private static final String ONPREM_TOOLS_BASE_URI = getMobilityProperty("ONPREM_TOOLS_BASE_URI");
	private static final String ORBIT_BASE_URI = getMobilityProperty("ORBIT_BASE_URI");
	private static final String ORBIT_SECURE_BASE_URI = getMobilityProperty("ORBIT_SECURE_BASE_URI");
	private static final String ORBIT_TOOLS_BASE_URI = getMobilityProperty("ORBIT_TOOLS_BASE_URI");
	
	protected static final String ONPREM_BEARER_TOKEN = getMobilityProperty("DAPI_BEARER_TOKEN");
	protected static final String ORBIT_AZUREDATA_BEARER_TOKEN = getMobilityProperty("ORBIT_APIM_AZUREDATA_BEARER_TOKEN");
	protected static final String ORBIT_VALIDATE_BEARER_TOKEN = getMobilityProperty("ORBIT_APIM_VALIDATE_BEARER_TOKEN");
	protected static final String ORBIT_BONDING_BEARER_TOKEN = getMobilityProperty("ORBIT_APIM_BONDING_LOG_BEARER_TOKEN");
	protected static final String ORBIT_TOOLS_SUBSCRIPTION_KEY = getMobilityProperty("ORBIT_TOOLS_SUBSCRIPTION_KEY");
	
	protected static final Map<String, Object> ONPREM_AUTH_HEADER = Map.of("Authorization", "Bearer " + ONPREM_BEARER_TOKEN);
	protected static final Map<String, Object> ORBIT_AZUREDATA_AUTH_HEADER = Map.of("Authorization", "Bearer " + ORBIT_AZUREDATA_BEARER_TOKEN);
	protected static final Map<String, Object> ORBIT_VALIDATE_AUTH_HEADER = Map.of("Authorization", "Bearer " + ORBIT_VALIDATE_BEARER_TOKEN);
	protected static final Map<String, Object> ORBIT_BONDING_AUTH_HEADER = Map.of("Authorization", "Bearer " + ORBIT_BONDING_BEARER_TOKEN);
	
	protected static final String IMD_IDENTITY_URL = ONPREM_TOOLS_BASE_URI + getMobilityProperty("IMD_IDENTITY_ENDPOINT");
	protected static final String CSR_REQUEST_URL = ONPREM_TOOLS_BASE_URI + getMobilityProperty("CSR_REQUEST_ENDPOINT");
	protected static final String AZURE_DATA_URL = ORBIT_BASE_URI + getMobilityProperty("AZURE_DATA_ENDPOINT");
	protected static final String VALIDATE_URL = ORBIT_BASE_URI + getMobilityProperty("VALIDATE_ENDPOINT");
	protected static final String ACTIVATION_URL = ORBIT_BASE_URI + getMobilityProperty("ACTIVATION_ENDPOINT");
	protected static final String PROVISION_URL = ORBIT_TOOLS_BASE_URI + getMobilityProperty("PROVISION_ENDPOINT");
	protected static final String ALE_SIGNATURE_URL = ONPREM_TOOLS_BASE_URI + getMobilityProperty("ALE_SIGNATURE_ENDPOINT");
	protected static final String D2C_MESSAGE_URL = ORBIT_TOOLS_BASE_URI + getMobilityProperty("D2C_MESSAGE_ENDPOINT");
	protected static final String LAST_ACTIVITY_URL = ORBIT_TOOLS_BASE_URI + getMobilityProperty("LAST_ACTIVITY_ENDPOINT");
	protected static final String SESSION_RECORD_URL = ORBIT_SECURE_BASE_URI + getMobilityProperty("SESSION_RECORD_ENDPOINT");
	protected static final String TRANSMISSION_ENCRYPT_URL = ONPREM_TOOLS_BASE_URI + getMobilityProperty("TRANSMISSION_ENCRYPT_ENDPOINT");
	protected static final String PRE_BONDING_URL = ORBIT_BASE_URI + getMobilityProperty("PRE_BONDING_ENDPOINT");
	protected static final String POST_BONDING_URL = ORBIT_SECURE_BASE_URI + getMobilityProperty("POST_BONDING_ENDPOINT");
	protected static final String CERTIFICATE_REFRESH_URL = ORBIT_SECURE_BASE_URI + getMobilityProperty("CERTIFICATE_REFRESH_ENDPOINT");
	protected static final String ENCRYPTED_SNEARKERNET_TRANSMISSION_URL = "http://" + FrameworkProperties.getProperty("CHS_HOST")+ ":" + 
	FrameworkProperties.getProperty("CHS_PORT") + getMobilityProperty("ENCRYPTED_SNEAKERNET_PAYLOAD");
	
	protected static final String MERLINPROGRAMMER_PRIVATE_KEY = getMobilityProperty("MERLINPROGRAMMER_PRIVATE_KEY");
	protected static final String MERLINPROGRAMMER_IDENTITY_RECORD = getMobilityProperty("MERLINPROGRAMMER_IDENTITY_RECORD");
	public static final long TOLERANCE_PROFILE_ALE = Long.parseLong(getMobilityProperty("PROFILE_TOLERANCE_ALE"));
	
	/**Represents an implanted device type which can have an associated patient app instance.*/
	public enum MobilityDeviceType {
		NGQ("2501"),
		ICM("2235");
		
		/**lookup.code.code_qualfier = 'Application_Type_Cd'*/
		@Getter
		private String applicationTypeCode;
		
		private MobilityDeviceType(String code) {
			applicationTypeCode = code;
		}
	}
	
	/**Represents supported operating systems for the patient app instance.*/
	public enum MobilityOS {
		ANDROID("Android", "2232"),
		IOS("IOS", "2233");
		
		private String name;
		/**lookup.code.code_qualfier = 'Platform_Cd'*/
		@Getter
		private String platformCode;
		
		private MobilityOS(String osName, String code) {
			name = osName;
			platformCode = code;
		}
		
		@Override
		public String toString() {
			return name;
		}
		
		/**Retrieves app model number corresponding to implanted device type for the OS.*/
		public String getAppModel(MobilityDeviceType deviceType) {
			switch(deviceType) {
				case NGQ:
					return (this == MobilityOS.ANDROID) ? "APP1004" : "APP1005";
				case ICM: // APP1000 / APP1001
				default:
					throw new RuntimeException("Unsupported device type: " + deviceType);
			}
		}
	}
	
	/**Represents a workflow profile type supported by a Patient App instance.*/
	@Getter
	public enum ProfileType {
		/**100 System Profile*/
		SYSTEM(100),
		/**200 Workflow Profile*/
		SCHEDULED_FOLLOW_UP(200),
		/**210 Workflow Profile*/
		SCHEDULED_DEVICE_CHECK(210),
		/**230 Workflow Profile*/
		DIRECT_ALERTS_PROGRAMMING(230),
		/**260 Workflow Profile*/
		PATIENT_INITIATED_FOLLOW_UP(260),
		/**270 Workflow Profile*/
		MERLIN_ENHANCED_DIAGNOSTICS(270),
		/**280 Workflow Profile*/
		DUAL_PATIENT_NOTIFIER(280),
		/**840 Instruction Profile*/
		PARAMETER_SYNC(840);
		
		private int profileCode;
		
		private ProfileType(int code) {
			profileCode = code;
		}
	}
	
	public MobilityUtilities(FileUtilities fileManager) {
		if (MobilityUtilities.fileManager == null) {
			MobilityUtilities.fileManager = fileManager;
		}
	}
	
	/**
	 * Replaces placeholder values in JSON file.
	 * @param fileName Name of JSON file (in src/test/resources/json) to be parsed.
	 * @param replaceValues Replaces placeholder values in file delimited by &lt;VALUE&gt; in sequence.
	 */
	public static final String updateJSON(String fileName, Map<String, Object> replaceValues) {
		if (!fileName.contains(".json")) {
			fileName += ".json";
		}
		
		String body = fileManager.getFileContent(JSON_PATH + fileName);

		Set<String> replaceKeys = replaceValues.keySet();
		
		for (String key : replaceKeys) {
			body = body.replace("<" + key + ">", String.valueOf(replaceValues.get(key)));
		}

		return body;
	}
	
	/**
	 * Saves data associated with patient app instance to properties file.
	 * @return Fully qualified path where properti"AZUes file is stored
	 */
	public static final String toPropertiesFile(FrameworkLog log, PatientApp patientApp) {
		String azureId = patientApp.getAzureId();
		String deviceSerial = patientApp.getDeviceSerial();
		PatientAppKeys keys = patientApp.getKeys();
		PatientAppIdentity identity = patientApp.getIdentity();
		PhoneData phoneData = patientApp.getPhoneData();
		String fileName = azureId + "_" + deviceSerial + "_" + FrameworkLog.getCurrentTimestamp() + "_regbond.properties";
		
		Properties appProperties = new Properties();
		appProperties.setProperty("AZURE_ID", azureId);
		appProperties.setProperty("ABBREVIATED_MODEL_NUM", patientApp.getAbbreviatedModelNum());
		appProperties.setProperty("DEVICE_SERIAL", deviceSerial);
		appProperties.setProperty("PUBLIC_KEY_ENCODED", keys.getPublicKeyEncoded());
		appProperties.setProperty("PRIVATE_KEY_URL_ENCODED", keys.getPrivateKeyURLEncoded());
		appProperties.setProperty("USER_RECORD_ID", String.valueOf(identity.getUserRecordId()));
		appProperties.setProperty("PATIENT_APP_IDENTITY_RECORD", identity.getPatientAppIdentityRecord());
		appProperties.setProperty("APP_VERSION", phoneData.getAppVer());
		appProperties.setProperty("OS", phoneData.getOs());
		appProperties.setProperty("OS_VERSION", phoneData.getOsVer());
		appProperties.setProperty("MAKE", phoneData.getMake());
		appProperties.setProperty("MODEL", phoneData.getModel());
		appProperties.setProperty("LOCALE", phoneData.getLocale());
		appProperties.setProperty("MTX", phoneData.getMtx());
		appProperties.setProperty("IMEI_NUMBER", phoneData.getImeiNumber());
		appProperties.setProperty("PACKAGE_ID", phoneData.getPackageId());
		appProperties.setProperty("SERVER_REGION", phoneData.getServerRegion());
		appProperties.setProperty("REGISTRATION_CODE", patientApp.getRegistrationCode());
		
		String testDirectoryPath = log.getLogDirectory() + fileName;
		
		fileManager.createPropertiesFile(testDirectoryPath, appProperties);
		fileManager.copyFileToPath(testDirectoryPath, LOG_DIR);
		
		return LOG_DIR + fileName;
	}
	
	/**
	 * Retrieves property value from designated mobility properties file. 
	 */
	public static final String getMobilityProperty(String propertyName) {
		return FrameworkProperties.getProperty(propertyName, MOBILITY_PROPERTIES);
	}
	
	/**Returns argument as a base64 encoded string.*/
	public static final String base64Encode(String content) {
		return ENCODER.encodeToString(content.getBytes());
	}
	
	/**Interprets a complex file (.zip, etc) as a base64 encoded string.*/
	public static final String base64EncodeFile(String filePath) {
		return ENCODER.encodeToString(fileManager.getFileBytes(filePath));
	}
	
	/**Returns a base64 encoded string as UTF-8 plaintext.*/
	public static final String base64Decode(String content) {
		return new String(DECODER.decode(content), StandardCharsets.UTF_8);
	}
	
	/**Generates an unique UUID (Version 4).*/
	public static final String getUUID() {
		return UUID.randomUUID().toString();
	}
	
	/** Generate bonding logs API payload file */
	public static final String getBondingLogFile(MITETest currentTest, int fileSizeInMb) {
			String destinationPath = currentTest.getLog().getLogDirectory() + "bondinglogs.txt";
			String destinationPathZip = currentTest.getLog().getLogDirectory() + "bondinglogs.zip";			
			char[] chars = fileSizeInMb <10 ? new char[1024 * 10] : new char[1024 * 1000];			
			Arrays.fill(chars, 'f');
			String tempString = new String(chars);

			try {
				PrintWriter out = new PrintWriter(destinationPath);
				for (int i = 0; i <= fileSizeInMb; i++) {
					out.println(tempString);
				}
				out.close();
			} catch (FileNotFoundException e) {
				currentTest.getReport().logStep(TestStep.builder().message("Log file not found.").build());
			}

			try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(destinationPathZip))) {
				File fileToZip = new File(destinationPath);
				zipOut.setMethod(ZipOutputStream.DEFLATED);
				zipOut.setLevel(0); // level 0 means you do not want to compress the file
				zipOut.putNextEntry(new ZipEntry(fileToZip.getName()));
				Files.copy(fileToZip.toPath(), zipOut);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return destinationPathZip;
	}	
}
