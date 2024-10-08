package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

/**
 * ALE used to communicate that content updates have been received 
 * by the Patient App and should not be retried subsequently.
 * 
 * @author Arya Biswas
 * @version Q1 2024
 */
public class ContentUpdateReceipt extends AppLifeEvent {
	
	private static final String CONTENT_UPDATE_RECEIPT_CODE = "4013";
	
	/**
	 * @param aleResult Desired result code for ALE.
	 * @param patientApp Patient App instance to be bonded.
	 */
	public ContentUpdateReceipt(PatientApp patientApp, AppLifeEventResult aleResult) {
		super("contentUpdateReceipt", aleResult,
				Map.of("result_code", aleResult.getCode(),
						"result_description", aleResult.getDescription(),
						"app_time", System.currentTimeMillis(),
						"azure_id", patientApp.getAzureId(),
						"patient_app_ale_id", MobilityUtilities.getUUID(),
						"user_record_id", patientApp.getIdentity().getUserRecordId()));
	}
	
	@Override
	public String getALETypeCode() {
		return CONTENT_UPDATE_RECEIPT_CODE;
	}
}
