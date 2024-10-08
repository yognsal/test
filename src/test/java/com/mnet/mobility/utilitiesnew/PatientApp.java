package com.mnet.mobility.utilities;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.apache.http.HttpStatus;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.database.utilities.PatientAppDBUtilities.IOTMessageColumn;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.APIRequest.APICharset;
import com.mnet.framework.api.APIResponse;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.middleware.UnixConnector;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.framework.utilities.DateUtility.DateTimeFormat;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.framework.utilities.Timeout;
import com.mnet.mobility.utilities.Keyfactor.CertificateTemplate;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityDeviceType;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.validation.AppLifeEventValidation;
import com.mnet.mobility.utilities.validation.BondingValidation;
import com.mnet.mobility.utilities.validation.DeviceProvisioningValidation;
import com.mnet.mobility.utilities.validation.SessionRecordValidation;
import com.mnet.pojo.mobility.AppLifeEvent;
import com.mnet.pojo.mobility.AppLifeEvent.AppLifeEventResult;
import com.mnet.pojo.mobility.EncryptedPayload;
import com.mnet.pojo.mobility.EncryptedPayload.TelemetryType;
import com.mnet.pojo.mobility.PatientAppCertificate;
import com.mnet.pojo.mobility.PatientAppIdentity;
import com.mnet.pojo.mobility.PatientAppKeys;
import com.mnet.pojo.mobility.PhoneData;
import com.mnet.pojo.mobility.ale.IMDBondingStatus;
import com.mnet.pojo.mobility.ale.PatientAppIdentityRefresh;
import com.mnet.pojo.mobility.ale.PatientAppRelationshipRefresh;
import com.mnet.pojo.mobility.ale.PhoneProfileChange;
import com.mnet.pojo.mobility.ale.ProfileReceipt;
import com.mnet.pojo.mobility.ale.SessionRecordPayload;

import io.restassured.http.ContentType;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a simulated (version 2.0) Patient App instance.
 * 
 * @author NAIKKX12, Arya Biswas
 * @version Fall 2023
 */
public abstract class PatientApp implements DeviceProvisioningValidation, AppLifeEventValidation, BondingValidation, SessionRecordValidation {
	
	public static final long API_MOBILITY_TIMEOUT = Long.parseLong(FrameworkProperties.getProperty("API_MOBILITY_TIMEOUT"));
	
	private FrameworkLog log;
	private FileUtilities fileManager;

	/**Determines whether detailed functional validation of APIs should be performed after the corresponding calls.*/
	@Setter
	private boolean apiValidation;
	
	@Getter
	private MITETest currentTest;
	/**Unique UUID associated with patient app instance*/
	@Getter
	private String azureId;
	/**Consolidated model number of patient's implanted device as identified in devices.device_product*/
	@Getter @Setter
	private String abbreviatedModelNum;
	/**Serial number of patient's implanted device*/
	@Getter
	private String deviceSerial;
	/**Identity associated with patient app instance (public cert / user_record_id)*/
	@Getter
	private PatientAppIdentity identity;
	/**Metadata associated with user's device (provisioning payload)*/
	@Getter
	private PhoneData phoneData;
	
	/**App keys associated with app installation (CSR request)*/
	@Getter
	private PatientAppKeys keys;
	
	@Getter//(AccessLevel.PROTECTED)
	private String registrationCode;
	
	private String userDeviceInfo, bondingUserDeviceInfo;
	
	protected MobilityUtilities mobility;
	protected TestReporter report;
	
	@Getter
	protected MobilityDeviceType deviceType;
	
	protected MobilityOS os;
	
	/**Enum to pass contact type key for activation code API*/
	public enum ContactTypeKey{
		EMAIL("Email"), PRIMARY_PHONE("PrimaryPhone");
		
		private String type;
		
		ContactTypeKey(String type){
			this.type = type;
		}
		
		public String getType() {
			return this.type;
		}
	}
	
	/**Represents a new patient app instance*/
	protected PatientApp(MITETest test, MobilityDeviceType mobilityDeviceType, MobilityOS mobilityOS) {
		currentTest = test;
		report = test.getReport();
		log = test.getLog();
		fileManager = test.getFileManager();
		
		mobility = new MobilityUtilities(fileManager);
		
		deviceType = mobilityDeviceType;
		os = mobilityOS;
	}
	
	/**
	 * Represents an already existing patient app instance.
	 * @param patientAppKeys Patient app keys as generated by CSR request.
	 * @param patientAppIdentity Patient app identity as generated by validate call.
	 * @param phoneData Phone data associated with provisioning payload.
	 */
	protected PatientApp(MITETest currentTest, MobilityDeviceType mobilityDeviceType, MobilityOS mobilityOS,
			String azureId, String abbreviatedModelNum, String deviceSerial, String registrationCode,
			PatientAppKeys patientAppKeys, PatientAppIdentity patientAppIdentity, PhoneData phoneData) {
		this(currentTest, mobilityDeviceType, mobilityOS);
		
		this.azureId = azureId;
		this.abbreviatedModelNum = abbreviatedModelNum;
		this.deviceSerial = deviceSerial;
		this.registrationCode = registrationCode;
		this.identity = patientAppIdentity;
		this.phoneData = phoneData;
		this.keys = patientAppKeys;
	}
		
	/**
	 * Represents an already existing patient app instance, derived from properties file.
	 * @param appProperties Fully qualified file path of app properties file generated using {@link MobilityUtilities#toPropertiesFile(FrameworkLog, PatientApp)}
	 */
	protected PatientApp(MITETest currentTest, MobilityDeviceType mobilityDeviceType, MobilityOS mobilityOS, String appProperties) {
		this(currentTest, mobilityDeviceType, mobilityOS,
			FrameworkProperties.getProperty("AZURE_ID", appProperties, FrameworkLog.LOG_DIR),
			FrameworkProperties.getProperty("ABBREVIATED_MODEL_NUM", appProperties, FrameworkLog.LOG_DIR),
			FrameworkProperties.getProperty("DEVICE_SERIAL", appProperties, FrameworkLog.LOG_DIR),
			FrameworkProperties.getProperty("REGISTRATION_CODE", appProperties, FrameworkLog.LOG_DIR),
			new PatientAppKeys(
					FrameworkProperties.getProperty("PUBLIC_KEY_ENCODED", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("PRIVATE_KEY_URL_ENCODED", appProperties, FrameworkLog.LOG_DIR)),
			new PatientAppIdentity(
					FrameworkProperties.getProperty("PATIENT_APP_IDENTITY_RECORD", appProperties, FrameworkLog.LOG_DIR),
					Long.parseLong(FrameworkProperties.getProperty("USER_RECORD_ID", appProperties, FrameworkLog.LOG_DIR))),
			PhoneData.of(mobilityDeviceType, mobilityOS,
					FrameworkProperties.getProperty("APP_VERSION", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("OS_VERSION", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("MAKE", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("MODEL", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("LOCALE", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("MTX", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("IMEI_NUMBER", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("PACKAGE_ID", appProperties, FrameworkLog.LOG_DIR),
					FrameworkProperties.getProperty("SERVER_REGION", appProperties, FrameworkLog.LOG_DIR)));
	}
	
	/**
	 * Retrieves DIM major version applicable to the patient app instance.
	 */
	public String getDIMMajorVersion() {
		return MobilityUtilities.getMobilityProperty("DIM_MAJOR_VERSION");
	}
	
	/**
	 * Convenience function to perform first-time Registration and Bonding.
	 * @return azureId associated with the patient app instance.
	 */
	public String firstTimeRegAndBond(String deviceSerial, String dateOfBirth) {
		if (azureId != null) {
			throw new RuntimeException("This does not represent a first-time NGQ2.0 Reg&Bond. Use individual endpoints to simulate reprovisioning / activation workflows.");
		}
		
		requestAzureData(deviceSerial);
		validatePatient(deviceSerial, dateOfBirth);
		provision();
		sendD2CMessage(new IMDBondingStatus(this, AppLifeEventResult.SUCCESS));
		
		MobilityUtilities.toPropertiesFile(log, this);
		
		return azureId;
	}
	
	/**
	 * Fetches global parameters from the server and provisions a unique UUID (azureId), if applicable.
	 * PhoneData is derived from mobility properties file.
	 */
	public APIResponse requestAzureData(String deviceSerial) {
		return requestAzureData(deviceSerial, PhoneData.of(deviceType, os));
	}
	
	/**
	 * Fetches global parameters from the server and provisions a unique UUID (azureId), if applicable.
	 * @param phoneData Custom PhoneData as provided in the azureData request body.
	 */
	public APIResponse requestAzureData(String deviceSerial, PhoneData devicePhoneData) {
		if (deviceSerial != this.deviceSerial) {
			createIMDIdentity(deviceSerial);
			this.deviceSerial = deviceSerial;
		}
		
		Map<String, Object> queryParams = new HashMap<>();
		queryParams.put("deviceType", deviceType.toString());
		queryParams.put("packageId", MobilityUtilities.getMobilityProperty("PACKAGE_ID"));
		queryParams.put("deviceserial", deviceSerial);
		
		if (azureId != null) {
			queryParams.put("azureId", azureId);
		}
		
		APIRequest request = new APIRequest(MobilityUtilities.AZURE_DATA_URL, queryParams, MobilityUtilities.ORBIT_AZUREDATA_AUTH_HEADER, devicePhoneData.asJSON(), ContentType.JSON, APICharset.NONE);
		report.logStep(TestStep.builder().message("AzureData API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());
		
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		if(response.getStringFromJsonPath("appConfig.azureId") != null) {
			azureId = response.getStringFromJsonPath("appConfig.azureId");
		}
		phoneData = devicePhoneData;
		
		if (apiValidation) {
			report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK, true, 
					TestStep.builder().message("Pre-provision Azure Data API request completed").build());
			
			// TODO: Validate response JSON against schema
		}

		return response;
	}

	/**
	 * Simulates validation of a patient app user providing their device serial and date of birth.
	 */
	public APIResponse validatePatient(String deviceSerial, String dateOfBirth) {
		return validatePatient(deviceSerial, dateOfBirth, null);
	}
	
	/**
	 * Simulates validation of a patient app user providing their activation code.
	 */
	public APIResponse validatePatient(String deviceSerial, String dateOfBirth, Integer activationCode) {
		if (keys == null) {
			makeCSRRequest();
		}

		Map<String, Object> queryParams;
		if (activationCode == null) {
			queryParams = Map.of("deviceserial", deviceSerial, "dob", dateOfBirth, "deviceType", os.toString());
		} else {
			queryParams = Map.of("deviceserial", deviceSerial, "dob", dateOfBirth, "deviceType", os.toString(),
					"activationCode", activationCode);
		}

		registrationCode = CommonUtils.randomAlphanumericString(10);

		Map<String, Object> header = new HashMap<>(MobilityUtilities.ORBIT_VALIDATE_AUTH_HEADER);
		header.put("userDeviceInfo", getUserDeviceInfo());

		APIRequest request = new APIRequest(MobilityUtilities.VALIDATE_URL, queryParams, header);
		report.logStep(TestStep.builder().reportLevel(ReportLevel.INFO).message("Validate API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());

		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);

		if (response.getStringFromJsonPath("patientAppIdentityRecord") != null
				&& response.getNumberFromJsonPath("userRecordId") != null) {
			identity = new PatientAppIdentity(response.getStringFromJsonPath("patientAppIdentityRecord"),
					response.getNumberFromJsonPath("userRecordId").longValue());
		}
		if (response.getStringFromJsonPath("abbreviatedModelNumber") != null) {
			abbreviatedModelNum = response.getStringFromJsonPath("abbreviatedModelNumber");
		}

		if (apiValidation) {
			report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK && identity != null, true,
					TestStep.builder().message("Pre-provision Validate API ran successfully").build());

			// TODO: Check user_record, user_device, Keyfactor certs
			List<Map<String, String>> userRecordData = PatientAppDBUtilities.getUserRecordData(this.getCurrentTest(),
					String.valueOf(this.getIdentity().getUserRecordId()));
			report.assertCondition(userRecordData.size() > 0, true, TestStep.builder().message("User record for " + 
					this.getIdentity().getUserRecordId() + " is created successfully").build());
			report.assertCondition(
					userRecordData.get(0).get("tc_flg").equals("t"), true,
					TestStep.builder().message("Privacy acceptance flag is set to True (tc_flg=1)").build());
			
			List<Map<String, String>> dbContent = PatientAppDBUtilities.getPPortalUserDevice(this.getCurrentTest(), 
					"user_record_id", String.valueOf(this.getIdentity().getUserRecordId()));
			report.assertCondition(dbContent.size() > 0, true, TestStep.builder().message
					("Record found in pportal.user_device for " + this.getIdentity().getUserRecordId()).build());
			report.assertCondition(dbContent.get(0).get("os_name").equals(this.getPhoneData().getOs()) && dbContent.get(0).get("app_model").
					equals(this.getPhoneData().getAppModel()) && dbContent.get(0).get("manufacturer").equals(this.getPhoneData().getMake())
					&& dbContent.get(0).get("app_version").equals(this.getPhoneData().getAppVer()), true, 
					TestStep.builder().message("Device info matches with database").build());
			
			Keyfactor certificateStore = new Keyfactor(log);		
			report.assertCondition(certificateStore.hasActivePatientAppCertificates(azureId), true, 
					TestStep.builder().message("Certificates are generated in keyfactor").build());
		}

		return response;

	}
	
	/**
	 * Simulates activation code api to send the activation code based on contact type and user record id.
	 */
	public APIResponse activationCode(long userRecordID, ContactTypeKey contactTypeKey) {
		
		Map<String, Object> queryParams = Map.of("contactTypeKey", contactTypeKey.getType(),
				"userRecordId", userRecordID);
		
		Map<String, Object> header = new HashMap<>(MobilityUtilities.ORBIT_VALIDATE_AUTH_HEADER);
		header.put("userDeviceInfo", userDeviceInfo);
		
		APIRequest request = new APIRequest(MobilityUtilities.ACTIVATION_URL, queryParams, header);
		report.logStep(TestStep.builder().reportLevel(ReportLevel.INFO).message("Activation Code API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());
		
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		if (apiValidation) {
			boolean result = activationResponseValuesInDB(this).getCodeValue().equals(response.getStringFromJsonPath("acValidationResponse.code"));
			report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK && result, true, TestStep.builder().message("Activation code API ran successfully").build());
		}
		
		return response;
		
	}

	/**
	 * Provisions a patient app instance to the region-specific IoT hub.
	 */
	public APIResponse provision() {
		Map<String, Object> queryParams = Map.of("azure-id", azureId,
											"global-endpoint", MobilityUtilities.getMobilityProperty("GLOBAL_ENDPOINT"),
											"id-scope", MobilityUtilities.getMobilityProperty("ID_SCOPE"));

		Map<String, Object> header = new HashMap<String, Object>(Map.of("Content-Type", "application/x-www-form-urlencoded"));
		
		if (!StringUtils.isEmpty(MobilityUtilities.ORBIT_TOOLS_SUBSCRIPTION_KEY)) {
			header.put("Ocp-Apim-Subscription-Key", MobilityUtilities.ORBIT_TOOLS_SUBSCRIPTION_KEY);
		}
		
		Map<String, Object> body = Map.of("public-pem", identity.getPublicPem(),
									"private-key", "-----BEGIN PRIVATE KEY-----\n" + keys.getPrivateKeyEncoded() + "\n-----END PRIVATE KEY-----",
									"provisioning-payload", phoneData.asJSON());
		
		APIRequest request = new APIRequest(MobilityUtilities.PROVISION_URL, queryParams, header, body, ContentType.URLENC, APICharset.NONE);
		report.logStep(TestStep.builder()
				.message("Provision API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());
		
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);

		if (apiValidation) {
			report.assertCondition((response.getStatusCode() == HttpStatus.SC_OK) && response.getBooleanFromJsonPath("provisioned"), true, 
					TestStep.builder().message("Provision API request completed").build());
			
			report.assertCondition(isDeviceProvisioned(this), true, 
					TestStep.builder().message("Device ID " + azureId + " is provisioned to IoT hub " + getAzureIotHubPropertyValue()).build());
			
			// TODO: Validate device twin
//			report.assertCondition(validateDeviceTwin(this), true, 
//					TestStep.builder().message("Device twin is populated with applicable defaults from global parameters: <textarea>" + getDeviceTwinContents(this) + "</textarea>").build());
		}
		
		return response;
	}

	/**
	 * Signs and sends an ALE as a device-to-cloud message.
	 * For sessionRecordPayload ALEs, use {@link #sessionRecordUpload(String, String, String, String, String, String)} instead.
	 */
	public APIResponse sendD2CMessage(AppLifeEvent appLifeEvent) {
		if (appLifeEvent instanceof SessionRecordPayload) {
			throw new RuntimeException("sessionRecordPayload ALE cannot be sent via d2c message - use /ngq-transmission-service/session-record instead.");
		}
		
		int initialTransmissionCount = Integer.MAX_VALUE;
		
		if (appLifeEvent.isBVVI()) {
			initialTransmissionCount = getTransmissionCount(currentTest, deviceSerial, TelemetryType.BVVI);
		}
		
		Map<String, Object> queryParams = Map.of("device-id", azureId,
									"iothub-uri", MobilityUtilities.getMobilityProperty("IOT_HUB_URI"));
		
		Map<String, Object> header = new HashMap<String, Object>(Map.of("Content-Type", "application/x-www-form-urlencoded"));
			
		if (!StringUtils.isEmpty(MobilityUtilities.ORBIT_TOOLS_SUBSCRIPTION_KEY)) {
			header.put("Ocp-Apim-Subscription-Key", MobilityUtilities.ORBIT_TOOLS_SUBSCRIPTION_KEY);
		}
		String cloudVerificationId;
		
		if((appLifeEvent instanceof IMDBondingStatus)) {
			cloudVerificationId = "null";
		}else if(appLifeEvent instanceof ProfileReceipt) {
			cloudVerificationId = "\""+PatientAppDBUtilities.getIOTMessage(this.getCurrentTest(), azureId, null).get(0).get(IOTMessageColumn.CLOUD_VERIFICATION_ID.getIOTMessageColumnName())+"\"";
		}else {
			cloudVerificationId = "\"" + MobilityUtilities.getUUID() + "\"";
		}
		
		String signature = signALE(appLifeEvent).getStringFromJsonPath("signature");
		
		String message = MobilityUtilities.updateJSON(MobilityUtilities.getMobilityProperty("D2C_MESSAGE_JSON"), 
				Map.of("d2c_msg_type", appLifeEvent.getType(),
					"encoded_content", appLifeEvent.getEncodedContent(),
					"signature", signature,
					"cloud_verification_id", cloudVerificationId));

		Map<String, Object> body = Map.of("public-pem", identity.getPublicPem(),
								"private-key", "-----BEGIN PRIVATE KEY-----\n" + keys.getPrivateKeyEncoded() + "\n-----END PRIVATE KEY-----",
								"message", message);
		
		APIRequest request = new APIRequest(MobilityUtilities.D2C_MESSAGE_URL, queryParams, header, body, ContentType.URLENC, APICharset.NONE);
		report.logStep(TestStep.builder()
				.message(appLifeEvent.getType() + " ALE (d2c message) API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>")
				.build());
		
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		if (appLifeEvent instanceof IMDBondingStatus && appLifeEvent.getResult().isBondLost()) {
			identity = null; // reset on deprovisioning
		}
		
		if (appLifeEvent instanceof PhoneProfileChange && (appLifeEvent.getResult() == AppLifeEventResult.SUCCESS)) {
			phoneData = ((PhoneProfileChange) appLifeEvent).getPhoneData();
			userDeviceInfo = null; // reset since phoneData has changed
		}
		
		if (apiValidation) {
			report.assertCondition((response.getStatusCode() == HttpStatus.SC_OK) && response.getBooleanFromJsonPath("sent"), true, 
					TestStep.builder().message("D2C message API request completed").build());
					
			validateAppLifeEvent(this, appLifeEvent);
			
			if (appLifeEvent instanceof IMDBondingStatus && (appLifeEvent.getResult() == AppLifeEventResult.SUCCESS)) {
				report.logStep(TestStep.builder().message("Waiting for mobility / MW services to populate backend...").build());
				Timeout.waitForTimeout(log, API_MOBILITY_TIMEOUT);
				
				validateBonding(this);
			}
			
			if (appLifeEvent instanceof PhoneProfileChange) {
				// TODO: Validation for user_device
			}
			
			if (appLifeEvent.isBVVI()) {
				validateSessionRecordCount(currentTest, deviceSerial, TelemetryType.BVVI, initialTransmissionCount);
			}
		}

		return response;
	}
	
	/**
	 * @param payloadLabel Telemetry type of the transmission (FUA, FUP, FUD, etc).
	 * @param workflowId Workflow profile code associated with the session record.
	 * @param filePath Fully qualified path of the unencrypted transmission payload.
	 */
	public APIResponse sessionRecordUpload(TelemetryType payloadLabel, int workflowId, String filePath) {
		if (payloadLabel == TelemetryType.BVVI) {
			throw new RuntimeException("BVVI transmissions can only be generated via workflow status ALE with Service App (19) state");
		}
		
		APIResponse encryptionResponse = encryptTransmission(filePath);
		
		int initialTransmissionCount = (apiValidation) ? getTransmissionCount(currentTest, deviceSerial, payloadLabel) : 0;
		
		/*
		 * Original file has format of deviceSerial_deviceModel_timestamp.zip
		 * Payload file name has format of deviceSerial_epochTime.zip
		 */
		long timestamp = System.currentTimeMillis();
		String payloadFileName = deviceSerial + "_" + timestamp + ".zip";
		EncryptedPayload transmission = new EncryptedPayload(
				payloadLabel, workflowId, payloadFileName, 
				encryptionResponse.getStringFromJsonPath("transmissionKey"),
				encryptionResponse.getStringFromJsonPath("randomSeed"),
				encryptionResponse.getStringFromJsonPath("payloadHashValue"),
				encryptionResponse.getStringFromJsonPath("encryptedSessionRecord"));
		
		String recordId = MobilityUtilities.getUUID();

		SessionRecordPayload payload = new SessionRecordPayload(this, transmission, recordId, timestamp);
		
		String body = MobilityUtilities.updateJSON(MobilityUtilities.getMobilityProperty("FILE_UPLOAD_JSON"), 
				Map.of("encoded_content", payload.getEncodedContent(),
					"signature", signALE(payload).getStringFromJsonPath("signature"),
					"record_id", recordId));
		
		Map<String, Object> headers = Map.of("blobFileName", azureId + "_" + recordId + "_" + timestamp,
				"userDeviceInfo", getUserDeviceInfo());

		APIRequest request = new APIRequest(MobilityUtilities.SESSION_RECORD_URL, headers, body, ContentType.JSON, APICharset.NONE);
		report.logStep(TestStep.builder().message("Session record API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());
				
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		if (apiValidation) {
			report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK, true, 
					TestStep.builder().message("Session record API request completed").build());
			
			validateSessionRecordCount(currentTest, deviceSerial, payloadLabel, initialTransmissionCount);
			
			validatePayloadInAzure(this.getCurrentTest(), filePath.substring(filePath.lastIndexOf("\\") + 1));
		}
		
		return response;
	}
	
	/** @param filename payload key value to upload (assumed to be in Data folder) */
	public APIResponse preBondingLogs(String filePath) {	
		return sendBondingLogs(filePath, MobilityUtilities.PRE_BONDING_URL);
	}
	
	/** @param filename payload key value to upload (assumed to be in Data folder) */
	public APIResponse postBondingLogs(String filePath) {
		return sendBondingLogs(filePath, MobilityUtilities.POST_BONDING_URL);
	}
	
	/** Patient App credential certificate refresh */
	public APIResponse patientAppCredentialRefresh() {
		PatientAppRelationshipRefresh relationshipRefresh = new PatientAppRelationshipRefresh(this, System.currentTimeMillis());
		return certificateRefresh(relationshipRefresh);
	}
	
	/** Patient App Identity certificate refresh */
	public APIResponse patientAppIdentityRefresh() {		
		PatientAppIdentityRefresh certificateRefresh = new PatientAppIdentityRefresh(this, System.currentTimeMillis());
		return certificateRefresh(certificateRefresh);
	}
	
	/** Last Activity API call and validation if required */
	public APIResponse lastActivity(String revisedLastCommDate) {
		
		String lastCommunicationDate = getLastCommDateFromDB(this);
		report.logStep(TestStep.builder().message("Current last communication date in database: " + lastCommunicationDate).build());
		report.logStep(TestStep.builder().message("Last communication date to update: " + revisedLastCommDate).build());
		
		Map<String, Object> queryParams = Map.of("device-id", this.getAzureId(),
				"iothub-uri", MobilityUtilities.getMobilityProperty("IOT_HUB_URI"),
				"dtm", System.currentTimeMillis());

		Map<String, Object> header = new HashMap<>(MobilityUtilities.ONPREM_AUTH_HEADER);
		header.put("Content-Type", "application/x-www-form-urlencoded");

		if (!StringUtils.isEmpty(MobilityUtilities.ORBIT_TOOLS_SUBSCRIPTION_KEY)) {
			header.put("Ocp-Apim-Subscription-Key", MobilityUtilities.ORBIT_TOOLS_SUBSCRIPTION_KEY);
		}

		Map<String, Object> body = Map.of("public-pem", this.getIdentity().getPublicPem(), 
				"private-key", "-----BEGIN PRIVATE KEY-----\n" + this.getKeys().getPrivateKeyEncoded() 
				+ "\n-----END PRIVATE KEY-----", "lastCommunicationDtm",
				DateUtility.changeDateToEpoch(revisedLastCommDate, DateTimeFormat.PAYLOAD, log) + 19800000l);

		APIRequest request = new APIRequest(MobilityUtilities.LAST_ACTIVITY_URL, queryParams, header, body,
				ContentType.URLENC, APICharset.NONE);
		report.logStep(TestStep.builder()
				.message("Last Activity API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());

		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);

		if (apiValidation) {
			report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK && 
					response.toString().equals("Successfully updated reported property for deviceId: " 
			+ this.getAzureId()), true, 
					TestStep.builder().message("Last activity API request completed").build());
			
			boolean valid = DateUtility.compareDates(revisedLastCommDate, lastCommunicationDate, 
					DateTimeFormat.PAYLOAD.getFormat(), log);
			lastCommunicationDate = revisedLastCommDate;
			revisedLastCommDate = getLastCommDateFromDB(this).split(" ")[0];
			report.assertCondition(revisedLastCommDate.equals(lastCommunicationDate.split(" ")[0]), valid, 
					TestStep.builder().message(valid == true ? "Last communication date in database is updated as expected" : 
						"Last communication date in database is not updated since earlier to current value").build());	
		}
		
		return response;
	}
	
	/** Sneakernet encrypted payload generation AP */
	public APIResponse encryptedSnearkernetTransmission(String filePath) {
		
		Map<String, Object> queryParams = Map.of("merlinProgrammerPrivateKey", MobilityUtilities.MERLINPROGRAMMER_PRIVATE_KEY, 
				"merlinProgrammerIdentityRecord", MobilityUtilities.MERLINPROGRAMMER_IDENTITY_RECORD);
		
		Map<String, Object> header = new HashMap<String, Object>(Map.of("Content-Type", "multipart/form-data"));
		
		APIRequest request = new APIRequest(MobilityUtilities.ENCRYPTED_SNEARKERNET_TRANSMISSION_URL, queryParams, header, "file", new File(filePath));
				
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		if (apiValidation) {
			report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK, true, 
					TestStep.builder().message("Encrypted sneakernet transmission API executed successfully").build());
			
			String encryptedFile = response.getListFromJsonPath("headers.Content-Disposition").get(0).toString();
			encryptedFile = encryptedFile.substring(encryptedFile.indexOf('=') + 2, encryptedFile.length() - 1);
		
			UnixConnector unixConnect = new UnixConnector(log, FrameworkProperties.getProperty("CHS_HOST"), 
					FrameworkProperties.getProperty("CHS_USERNAME"), FrameworkProperties.getProperty("CHS_PASSWORD"));
			report.assertCondition(unixConnect.fileExists("/opt/sjm/ngqTransmissions/encryptedFile/" + encryptedFile), true, 
					TestStep.builder().message("Encrypted payload file '" + encryptedFile + "' successfully uploaded on CHS server").build());
		}
		
		return response;
	}
	
	/*
	 * ----------------------
	 * Local helper functions
	 * ----------------------
	 */
	
	/**Constructs base64 encoded userDeviceInfo*/
	private String getUserDeviceInfo() {
		if (userDeviceInfo == null) {
			userDeviceInfo = MobilityUtilities.base64Encode(
					MobilityUtilities.updateJSON(
						MobilityUtilities.getMobilityProperty("VALIDATE_JSON"),
						Map.of("public_token", MobilityUtilities.ORBIT_VALIDATE_BEARER_TOKEN,
							"public_key", keys.getPublicKeyEncoded(),
							"os", phoneData.getOs(),
							"manufacturer", phoneData.getManufacturer(),
							"app_model", phoneData.getAppModel(),
							"app_version", phoneData.getAppVer(),
							"azure_id", azureId,
							"registration_code", registrationCode,
							"locale", phoneData.getLocale())));
		}
		
		return userDeviceInfo;
	}
	
	/**Constructs base64 encoded bondingUserDeviceInfo*/
	private String getBondingUserDeviceInfo(String fileName) {
		if (bondingUserDeviceInfo == null) {
			PatientDBUtilities patientDBUtils = new PatientDBUtilities(this.currentTest.getReport(), this.currentTest.getDatabase());
			bondingUserDeviceInfo = MobilityUtilities.base64Encode(
					MobilityUtilities.updateJSON(
						MobilityUtilities.getMobilityProperty("BONDING_LOG_JSON"),
						Map.ofEntries(Map.entry("azureId", azureId),
								Map.entry("userRecordid", String.valueOf(identity.getUserRecordId())),
								Map.entry("filename", fileName),
								Map.entry("proxy_version", MobilityUtilities.getMobilityProperty("PROXY_VERSION")),
								Map.entry("app_model", phoneData.getAppModel()),
								Map.entry("app_version", phoneData.getAppVer()),
								Map.entry("os", phoneData.getOs()),
								Map.entry("os_version", phoneData.getOsVer()),
								Map.entry("connectivity", MobilityUtilities.getMobilityProperty("CONNECTIVITY")),
								Map.entry("patientAppId", MobilityUtilities.getUUID()),
								Map.entry("wrapped_AESKey", MobilityUtilities.getUUID()),
								Map.entry("randomSeed", MobilityUtilities.getUUID()),
								Map.entry("device_serial", deviceSerial),
								Map.entry("device_model", patientDBUtils.getDeviceDetails(deviceSerial).get("device_model_num")))));
		}
		return bondingUserDeviceInfo;
	}
	
	/**
	 * Private function to create link to API response JSON created in extent report folder
	 */
	private void createResponseFile(APIResponse response) {
		String method = Thread.currentThread().getStackTrace()[2].getMethodName();
		String filename = method + "_" + FrameworkLog.getCurrentTimestamp() + ".json";

		fileManager.writeToFile(log.getLogDirectory() + filename, response.toString());

		report.logStep(TestStep.builder()
				.message("API Response JSON: <a href='../" + log.getRelativeLogDirectory() + filename
				+ "'>" + "Logs from " + method + "</a>").build());
	}
	
	/*
	 * ----------------------
	 * tools-service (internal) API requests
	 * ----------------------
	 */
	
	/**
	 * Simulates the device manufacturing process and associates an identity cert with the implanted device.
	 */
	private APIResponse createIMDIdentity(String deviceSerial) {
		Map<String, Object> queryParams = Map.of("deviceType", deviceType.toString(),
										"deviceserial", deviceSerial);
		
		APIRequest request = new APIRequest(MobilityUtilities.IMD_IDENTITY_URL, queryParams, MobilityUtilities.ONPREM_AUTH_HEADER);
		report.logStep(TestStep.builder()
				.message("IMD Identity API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>")
				.build());
		
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		if (apiValidation) {
			// TODO: Validate IMD Identity in Keyfactor
		}

		return response;
	}
	
	/**
	 * Associates a public-private key pair with the patient app instance.
	 */
	private APIResponse makeCSRRequest() {
		Map<String, Object> queryParams = Map.of("azureId", azureId);
		
		APIRequest request = new APIRequest(MobilityUtilities.CSR_REQUEST_URL, queryParams, MobilityUtilities.ONPREM_AUTH_HEADER);
		report.logStep(TestStep.builder().message("CSR API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());
		
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		keys = new PatientAppKeys(
				response.getStringFromJsonPath("request"),
				response.getStringFromJsonPath("privateKeyURLEncoded"));
		
		return response;
	}

	/**
	 * Function to execute ALE Signature REST API
	 */
	private APIResponse signALE(AppLifeEvent appLifeEvent) {
		Map<String, Object> queryParams = Map.of("patientAppPrivateKey", keys.getPrivateKeyURLEncoded());
		
		APIRequest request = new APIRequest(MobilityUtilities.ALE_SIGNATURE_URL, queryParams, MobilityUtilities.ONPREM_AUTH_HEADER, appLifeEvent.getEncodedContent(), ContentType.TEXT, APICharset.NONE);
		report.logStep(TestStep.builder().message("ALE Signature API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>").build());
		
		APIResponse response = RestAPIManager.post(request);	
		createResponseFile(response);

		return response;
	}
	
	/**
	 * Session upload - Encrypt Transmission API
	 */
	private APIResponse encryptTransmission(String filePath) {
		Map<String, Object> queryParams = Map.of("deviceType", deviceType.toString(),
								"patientAppPrivateKey", keys.getPrivateKeyURLEncoded());

		APIRequest request = new APIRequest(
				MobilityUtilities.TRANSMISSION_ENCRYPT_URL, 
				queryParams, 
				MobilityUtilities.ONPREM_AUTH_HEADER, 
				MobilityUtilities.base64EncodeFile(filePath), 
				ContentType.TEXT, APICharset.NONE);
		report.logStep(TestStep.builder()
				.message("Transmission Encryption API Request:<textarea>\n" + request.asLoggableString() + "\n</textarea>")
				.build());
		
		APIResponse response = RestAPIManager.post(request);

		return response;
	}
	
	/** @param filename payload key value to upload (assumed to be in Data folder) */
	private APIResponse sendBondingLogs(String filePath, String url) {
				
		String fileName = null;
		if (filePath != null) {
			fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
		}

		Map<String, Object> header = new HashMap<>(MobilityUtilities.ORBIT_BONDING_AUTH_HEADER);
		header.put("userDeviceInfo", getBondingUserDeviceInfo(fileName));
		
		APIRequest request = new APIRequest(url, header, "payload", new File(filePath));
		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);
		
		if (apiValidation) {
			report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK, true, 
					TestStep.builder().message("Bondng logs API successfully completed").build());
			
			report.assertCondition(verifyBondingLogInDB(this), true, TestStep.builder().message("DB check for bonding logs API is successful").build());
			report.assertCondition(verifyBondingLogInAzure(this), true, TestStep.builder().message("Azure check for bonding logs API is successful").build());
		}
		
		return response;
	}
	
	/** Certificate Refresh API call */
	private APIResponse certificateRefresh(AppLifeEvent lifeEvent) {
		CertificateTemplate certificateType = lifeEvent instanceof PatientAppIdentityRefresh
				? CertificateTemplate.AbbottPatientAppIdentity
				: CertificateTemplate.AbbottPatientAppCredential;
		
		Keyfactor certificateStore = new Keyfactor(log);
		
		Set<PatientAppCertificate> currentCertificateList = certificateStore
				.getCertificates(Integer.parseInt(this.getDeviceSerial()), this.azureId, certificateType);
		report.logStep(TestStep.builder()
				.message("Total certificates before refresh are : " + currentCertificateList.size()).build());

		String signature = signALE(lifeEvent).getStringFromJsonPath("signature");

		Map<String, Object> queryParams = Map.of("device-id", azureId, "iothub-uri",
				MobilityUtilities.getMobilityProperty("IOT_HUB_URI"));
		
		Map<String, Object> header = new HashMap<>(MobilityUtilities.ONPREM_AUTH_HEADER);
		header.put("Content-Type", "application/json");

		String body = MobilityUtilities.updateJSON(MobilityUtilities.getMobilityProperty("CERTIFICATE_REFRESH_JSON"),
				Map.of("encoded_content", lifeEvent.getEncodedContent(), "signature", signature));
		
		APIRequest request = new APIRequest(MobilityUtilities.CERTIFICATE_REFRESH_URL, queryParams, header, body,
				ContentType.JSON, APICharset.NONE);

		APIResponse response = RestAPIManager.post(request);
		createResponseFile(response);

		if (apiValidation) {
			String outcome = lifeEvent instanceof PatientAppIdentityRefresh
					? response.getStringFromJsonPath("patientAppIdentityRecord")
					: response.getStringFromJsonPath("patientAppImdCredential");
			report.assertCondition((response.getStatusCode() == HttpStatus.SC_OK) && outcome != null, true,
					TestStep.builder().message("PatientApp certificate refresh API completed").build());

			Set<PatientAppCertificate> revisedCertificateList = certificateStore
					.getCertificates(Integer.parseInt(this.getDeviceSerial()), this.azureId, certificateType);
			report.logStep(TestStep.builder()
					.message("Total certificates after API run are : " + revisedCertificateList.size()).build());
			report.assertCondition(revisedCertificateList.size() == currentCertificateList.size() + 1, true,
					TestStep.builder().message("New certificate is generated successfully").build());
			if (lifeEvent instanceof PatientAppIdentityRefresh) {
				List<Map<String, String>> dbContent = PatientAppDBUtilities.getPPortalUserDevice(this.getCurrentTest(), 
						"user_record_id", String.valueOf(this.getIdentity().getUserRecordId()));
				report.assertCondition(dbContent.size() > 0, true, TestStep.builder().message
						("Record found in pportal.user_device for " + this.getIdentity().getUserRecordId()).build());
				report.assertCondition(dbContent.get(0).get("last_relationship_success_dtm") != null, true, TestStep.builder().
						message("last_relationship_success_dtm value is not null & updated. Value is " + dbContent.get(0).get("last_patientappidentity_sent_dtm")).build());
				report.assertCondition(dbContent.get(0).get("last_patientappidentity_sent_dtm") != null, true, TestStep.builder().
						message("last_patientappidentity_sent_dtm value is not null & updated. Value is " + dbContent.get(0).get("last_patientappidentity_sent_dtm")).build());
			}
		}

		return response;
	}
}