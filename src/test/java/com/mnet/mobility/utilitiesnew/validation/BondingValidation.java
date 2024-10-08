package com.mnet.mobility.utilities.validation;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.azure.storage.blob.BlobClient;
import com.mnet.database.utilities.DBUtilities;
import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.database.utilities.PatientAppDBUtilities.PatientAppMetadataColumn;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.azure.AzureStorageAccount;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.AssertionLevel;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityDeviceType;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.ActivationCodeData;
import com.mnet.pojo.mobility.PhoneData;

/**
 * Provides functionality to verify that patient app bonded with implanted device.
 * @author Arya Biswas
 * @version Fall 2023
 */
public interface BondingValidation {

	/**
	 * Convenience function to perform all bonding-related validation and reporting.
	 * @return true if and only if all bonding-related validation was successful
	 */
	public default boolean validateBonding(PatientApp patientApp) {
		TestReporter report = patientApp.getCurrentTest().getReport();
		
		boolean userDeviceResult = isUserDeviceActive(patientApp);
		
		report.assertCondition(userDeviceResult, true,
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("Bonding successful: active_flg in pportal.user_device is set to true for azureId = " + patientApp.getAzureId())
				.failMessage("Bonding failed: active_flg in pportal.user_device is set to false for azureId = " + patientApp.getAzureId())
				.build());
		
		return (userDeviceResult && validatePatientAppMetadata(patientApp) && validateLastCommunicationDate(patientApp));
	}
	
	/**Checks for existence of patientapp_metadata database entry and validates against phone data*/
	public default boolean validatePatientAppMetadata(PatientApp patientApp) {
		TestReporter report = patientApp.getCurrentTest().getReport();
		
		Map<String, String> patientAppMetadata = getPatientAppMetadata(patientApp);
		boolean patientAppMetadataFound = patientAppMetadata != null;
		
		report.assertCondition(patientAppMetadataFound, true,
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("Patient app metadata found for patient: \n" + patientAppMetadata)
				.failMessage("Patient app metadata could not be found for patient with device serial: " + patientApp.getDeviceSerial())
				.build());
		
		PhoneData phoneData = patientApp.getPhoneData();
		String os = phoneData.getOs();
		
		String metadataPlatformCode = patientAppMetadata.get("platform_cd");
		String phonePlatformCode = MobilityOS.valueOf(os.toUpperCase()).getPlatformCode();
		
		boolean platformValid = phonePlatformCode.equals(metadataPlatformCode);
		report.assertCondition(platformValid, true, 
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("Database platform code " + metadataPlatformCode + " valid for " + os)
				.failMessage("Database platform code " + metadataPlatformCode + " is not valid for " + os + " | Expected: " + phonePlatformCode)
				.build());
		
		Instant registrationDate = DBUtilities.sqlTimestampToInstant(patientAppMetadata.get("registration_date"));

		Instant firstRegistrationDate = DBUtilities
				.sqlTimestampToInstant(patientAppMetadata.get("first_app_registration_date"));

		boolean registrationValid = (registrationDate != null);

		// Check whether this is a first-time registration (or reprovision post device changeout / data correction)
		if (registrationValid && registrationDate.equals(firstRegistrationDate)) {
			report.logStep(TestStep.builder().reportLevel(ReportLevel.PASS)
					.message(
							"First time registration / reprovision: Registration date matches first registration date: "
									+ registrationDate.toString())
					.build());
		} else {
			registrationValid = registrationValid && registrationDate.isAfter(firstRegistrationDate);

			report.assertCondition(registrationValid, true, TestStep.builder().assertionLevel(AssertionLevel.SOFT)
					.message("Subsequent registration: Registration date " + registrationDate.toString()
							+ " is after first registration date: " + firstRegistrationDate.toString())
					.failMessage("Subsequent registration: Registration date " + registrationDate.toString()
							+ " is not after first registration date " + firstRegistrationDate.toString())
					.build());
			}
		
		String applicationTypeCode = patientAppMetadata.get("application_type_cd");
		MobilityDeviceType deviceType = patientApp.getDeviceType();
		boolean applicationValid = deviceType.getApplicationTypeCode().equals(applicationTypeCode);
		
		report.assertCondition(applicationValid, true, 
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("Application type code " + applicationTypeCode + " is valid for " + deviceType.toString())
				.failMessage("Application type code " + applicationTypeCode + " is invalid for " + deviceType.toString() + " | Expected: " + deviceType.getApplicationTypeCode())
				.build());

		String metadataSoftwareVersion = patientAppMetadata.get("app_sw_version");
		boolean appSoftwareValid = phoneData.getAppVer().equals(metadataSoftwareVersion);
		
		report.assertCondition(appSoftwareValid, true, 
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("App software version in patientapp_metadata matches expected: " + metadataSoftwareVersion)
				.failMessage("App software version mismatch in patientapp_metadata: " + metadataSoftwareVersion + " | Expected: " + phoneData.getAppVer())
				.build());
		
		String metadataAppModelNumber = patientAppMetadata.get("app_model_number");
		boolean appModelValid = phoneData.getAppModel().equals(metadataAppModelNumber);
		
		report.assertCondition(appModelValid, true, 
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("App model number in patientapp_metadata matches expected: " + metadataAppModelNumber)
				.failMessage("App model number mismatch in patientapp_metadata for: " + metadataAppModelNumber + " | Expected: " + phoneData.getAppModel())
				.build());
		
		String metadataAbbreviatedDeviceModel = patientAppMetadata.get("abbreviated_device_model_num");
		boolean appAbbreviatedDeviceModelValid = patientApp.getAbbreviatedModelNum().equals(metadataAbbreviatedDeviceModel);
		
		report.assertCondition(appAbbreviatedDeviceModelValid, true, 
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("Abbreviated device model validated in patientapp_metadata: " + metadataAbbreviatedDeviceModel)
				.failMessage("Abbreviated device model mismatch in patientapp_metadata for: " + metadataAbbreviatedDeviceModel + " | Expected: " + patientApp.getAbbreviatedModelNum())
				.build());
				
		String metadataDimVersion = patientAppMetadata.get("dim_version");
		boolean appDimValid = (metadataDimVersion != null) && metadataDimVersion.startsWith(patientApp.getDIMMajorVersion());
		
		report.assertCondition(appDimValid, true, 
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("DIM version is valid for " + deviceType.toString())
				.failMessage("DIM major version (first 4 digits) mismatch for " + metadataDimVersion + " | Expected: " + patientApp.getDIMMajorVersion())
				.build());
		
		return (patientAppMetadataFound && platformValid && registrationValid && applicationValid
				&& appSoftwareValid && appModelValid && appAbbreviatedDeviceModelValid && appDimValid);
	}
	
	/**Validates last transmitter connected date with last communication date on UI**/
	public default boolean validateLastCommunicationDate(PatientApp patientApp) {
		PatientDBUtilities patientDBUtils = new PatientDBUtilities(patientApp.getCurrentTest().getReport(), patientApp.getCurrentTest().getDatabase());
		String expectedDate = (DateUtility.modifiedDate(0));
		HashMap<String, String> deviceDetails = patientDBUtils.getPatientDeviceDetails("device_serial_num", patientApp.getDeviceSerial());
		
		return expectedDate.equals(DateUtility.changeDateFormat(deviceDetails.get("last_transmitter_connected_dtm").split(" ")[0], null, null));
	}
	
	/** Get last communication date from database */
	public default String getLastCommDateFromDB(PatientApp patientApp) {
		Map<String, String> dbContent = PatientAppDBUtilities.getPatient(patientApp.getCurrentTest(), patientApp.getDeviceSerial());
		
		if (dbContent == null) {
			patientApp.getCurrentTest().getReport().logStep(TestStep.builder().reportLevel(ReportLevel.FAIL).message("DB content null").build());
			return null;
		}
		return dbContent.get("last_transmitter_connected_dtm");
	}
	
	/**Retrieves contents of patients.patientapp_metadata for a given patient*/
	public default Map<String, String> getPatientAppMetadata(PatientApp patientApp) {
		List<Map<String, String>> data = PatientAppDBUtilities.getPatientAppMetaData(patientApp.getCurrentTest(), patientApp.getDeviceSerial());
		
		if (data != null && data.size() == 1) {
			return data.get(0);
		} else {
			patientApp.getCurrentTest().getLog().warn("patientapp_metadata entry missing or multiple entries found for device serial: " + patientApp.getDeviceSerial());
			return null;
		}
	}
	
	/**Checks pportal.user_device.active_flg*/
	public default boolean isUserDeviceActive(PatientApp patientApp) {
		List<Map<String, String>> data = PatientAppDBUtilities.getPPortalUserDevice(patientApp.getCurrentTest(), "azureid", patientApp.getAzureId());
		
		if (data.size() == 0) {
			return false;
		}
		
		return data.get(0).get("active_flg").equals("t") ? true : false;
	}
	
	/**
	 * Verify Bonding Date under Registration date, First app registration date and
	 * last transmitter connected dtm columns of patient app metadata table
	 */
	public default boolean verifyBondingDate(PatientApp patientApp, String expectedDate) {
		List<Map<String, String>> patientMetadata = PatientAppDBUtilities
				.getPatientAppMetaData(patientApp.getCurrentTest(), patientApp.getDeviceSerial());

		return patientMetadata.get(0).get(PatientAppMetadataColumn.REGISTRATION_DATE.getPatientAppMetadataColumnName())
				.split(" ")[0].equals(expectedDate)
				&& patientMetadata.get(0)
						.get(PatientAppMetadataColumn.FIRST_APP_REGISTRATION_DATE.getPatientAppMetadataColumnName())
						.split(" ")[0].equals(expectedDate);

	}
	
	/** Verify if bonding log file record is generated in database */
	public default boolean verifyBondingLogInDB(PatientApp patientApp) {
		TestReporter report = patientApp.getCurrentTest().getReport();
		
		List<Map<String, String>> dbContentList = PatientAppDBUtilities.getDeviceBondingLog(patientApp.getCurrentTest(), patientApp.getAzureId()); 
		if (dbContentList.isEmpty()) {
			report.logStep(TestStep.builder().reportLevel(ReportLevel.INFO).message("No log file record is generated in the database").build());
			return false;
		}
		
		Map<String, String> dbContent = dbContentList.get(0);

		report.logStep(TestStep.builder().message("Database record generated for pre-bonding log API:<textarea>\n" + dbContent + "\n</textarea>").build());
		
		if ((MobilityUtilities.getMobilityProperty("PROXY_VERSION").equals(dbContent.get("proxy_version"))) &&
				(patientApp.getPhoneData().getAppModel().equals(dbContent.get("app_model"))) &&
						(patientApp.getPhoneData().getAppVer().equals(dbContent.get("app_version"))) &&
								(patientApp.getPhoneData().getOs().equals(dbContent.get("os_name"))) &&
										(patientApp.getPhoneData().getOsVer().equals(dbContent.get("os_version")))) {
			report.logStep(TestStep.builder().message("Proxy version, app model & version, OS and it's version used in bonding logs generation matches with database record created").build());
			return true;
		}
		return false;
	}
	
	/** Verify if log file generated in database reflected in azure container */
	public default boolean verifyBondingLogInAzure(PatientApp patientApp) {
		String logFilePath = PatientAppDBUtilities.getDeviceBondingLog(patientApp.getCurrentTest(), patientApp.getAzureId()).get(0).get("file_path");
		if (logFilePath == null) {
			patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("No log file path entry added in the database").build());
			return false;
		}
		patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("Log file path retrieved from the database is : " + logFilePath).build());
		AzureStorageAccount storageAccount = getBondingLogContainer(patientApp.getCurrentTest());
		return storageAccount.listBlobs(FrameworkProperties.getProperty("AZURE_BONDING_LOG_FOLDER")).contains(logFilePath);
	}
	
	/** Verify if Report file generated in database reflected in azure container */
	public default boolean verifyReportInAzure(PatientApp patientApp, String transmissionId) {
		String logFilePath = PatientAppDBUtilities.getTransmissionReports(patientApp.getCurrentTest(), "transmission_id",  transmissionId).get(0).get("file_url");
		if (logFilePath == null) {
			patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("No log file path entry added in the database").build());
			return false;
		}
		patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("report file path retrieved from the database is : " + logFilePath).build());
		AzureStorageAccount storageAccount = (AzureStorageAccount) patientApp.getCurrentTest().getAzure().getResource
				(FrameworkProperties.getProperty("AZURE_SESSION_RECORD_CONTAINER"), AzureStorageAccount.class);
		
		return storageAccount.listBlobs(FrameworkProperties.getProperty("AZURE_REPORT_FOLDER")).contains(logFilePath);		
	}
	
	/** Verify if Transmission zip file generated in database reflected in azure container */
	public default boolean verifyTransmissionZipInAzure(PatientApp patientApp, String filePath) {
		if (filePath == null) {
			patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("No Transmission zip file path entry added in the database").build());
			return false;
		}
		patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("Transmission zip file path retrieved from the database is : " + filePath).build());
		AzureStorageAccount storageAccount = (AzureStorageAccount) patientApp.getCurrentTest().getAzure().getResource
				(FrameworkProperties.getProperty("AZURE_SESSION_RECORD_CONTAINER"), AzureStorageAccount.class);
		
		return storageAccount.listBlobs(FrameworkProperties.getProperty("AZURE_SESSION_RECORD_FOLDER")).contains(filePath);		
	}
	
	/** Verify if log file can be accessed and downloaded */
	public default String downloadTransmissionZipInAzure(PatientApp patientApp, String filePath) {
		AzureStorageAccount storageAccount = (AzureStorageAccount) patientApp.getCurrentTest().getAzure().getResource
				(FrameworkProperties.getProperty("AZURE_SESSION_RECORD_CONTAINER"), AzureStorageAccount.class); 
		String localFilePath = storageAccount.downloadBlobAtPath(FrameworkProperties.getProperty("AZURE_SESSION_RECORD_FOLDER"), filePath);
		patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("Log file path is : " + localFilePath).build());
		return localFilePath;	
	}
	
	/** Verify if log file can be accessed and downloaded */
	public default boolean IsBondingLogDownloaded(PatientApp patientApp) {
		String logFilePath = PatientAppDBUtilities.getDeviceBondingLog(patientApp.getCurrentTest(), patientApp.getAzureId()).get(0).get("file_path");
		if (logFilePath == null) {
			patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("No log file path entry added in the database").build());
			return false;
		}
		AzureStorageAccount storageAccount = getBondingLogContainer(patientApp.getCurrentTest()); 
		String localFilePath = storageAccount.downloadBlobAtPath(FrameworkProperties.getProperty("AZURE_BONDING_LOG_FOLDER"), logFilePath);
		patientApp.getCurrentTest().getReport().logStep(TestStep.builder().message("Log file path is : " + localFilePath).build());
		return patientApp.getCurrentTest().getFileManager().fileExists(localFilePath);
		
	}
	
	/** Get the storage account object for bonding logs container */
	static AzureStorageAccount getBondingLogContainer(MITETest currentTest) {
		return (AzureStorageAccount) currentTest.getAzure().getResource(FrameworkProperties.getProperty("AZURE_BONDING_LOG_CONTAINER"), AzureStorageAccount.class);
	}
	
	/**
	 * Get the Retry count, failure count, Code and code value post running
	 * activaiton code API from ICM User record table based on user record id
	 **/
	public default ActivationCodeData activationResponseValuesInDB(PatientApp patientApp) {
		ActivationCodeData activationCodeDBValues = new ActivationCodeData();
		Map<String, String> userRecordData = PatientAppDBUtilities.getICMUserRecord(patientApp.getCurrentTest(), String.valueOf(patientApp.getIdentity().getUserRecordId()));
		String codeValue = PatientAppDBUtilities.getPportalCodeData(patientApp.getCurrentTest(), userRecordData.get("icm_ac_response_cd")).get("code");
		
		activationCodeDBValues.setFailureCount(userRecordData.get("activation_failure_cnt"));
		activationCodeDBValues.setRetryCount(userRecordData.get("activation_retry_cnt"));
		activationCodeDBValues.setCode(userRecordData.get("icm_ac_response_cd"));
		activationCodeDBValues.setCodeValue(codeValue);
		
		return activationCodeDBValues;
	}
}