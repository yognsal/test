package com.mnet.mobility.utilities.validation;

import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.framework.azure.AzureStorageAccount;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.utilities.Timeout;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.EncryptedPayload.TelemetryType;

/**
 * Provides functionality to validate a mobility session record upload.
 * @author Arya Biswas
 * @version Fall 2023
 */
public interface SessionRecordValidation {
	
	/**Retrieves number of transmissions associated with a given device serial.*/
	public default int getTransmissionCount(MITETest currentTest, String deviceSerial, TelemetryType telemetryType) {
		List<Map<String, String>> data = PatientAppDBUtilities.getTransmission(currentTest, deviceSerial, telemetryType);
		return (data != null) ? data.size() : 0;
	}
	
	/**Validates whether transmission count has increased relative to initial count
	 * @param initialTransmissionCount Transmission count prior to transmission processing for given telemetry type. Retrieve using {@link #getTransmissionCount(MITETest, String, TelemetryType)}*/
	public default void validateSessionRecordCount(MITETest currentTest, String deviceSerial, TelemetryType telemetryType, int initialTransmissionCount) {
		long ETL_PROCESSING_TIMEOUT = Long.parseLong(FrameworkProperties.getProperty("ETL_PROCESSING_TIMEOUT"));
		
		TestReporter report = currentTest.getReport();
		
		report.logStep(TestStep.builder().message("Waiting for mobility / MW services to populate backend...").build());
		Timeout.waitForTimeout(currentTest.getLog(), PatientApp.API_MOBILITY_TIMEOUT + ETL_PROCESSING_TIMEOUT);
		
		int currentTransmissionCount = getTransmissionCount(currentTest, deviceSerial, telemetryType);
		
		String transmissionCount =  "\n " + telemetryType.toString() + " transmission count before upload: " + initialTransmissionCount 
				+ "\n" + telemetryType.toString() + " transmission count after upload: " + currentTransmissionCount;
		
		report.assertCondition(currentTransmissionCount > initialTransmissionCount, true,
				TestStep.builder()
				.message("Record is created in database for successful session upload" + transmissionCount)
				.failMessage("No new record is created in database for session upload" + transmissionCount)
				.build());
	}
	
	/** Validate azure storage account for session record upload */
	public default boolean validatePayloadInAzure(MITETest currentTest, String payloadFileName) {
		AzureStorageAccount storageAccount = (AzureStorageAccount) currentTest.getAzure().getResource
				(FrameworkProperties.getProperty("AZURE_SESSION_RECORD_CONTAINER"), AzureStorageAccount.class);
		return storageAccount.listBlobs(FrameworkProperties.getProperty("AZURE_SESSION_RECORD_FOLDER")).contains(payloadFileName);
	}
	
}
