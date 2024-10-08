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
public class AppSetupStatus extends AppLifeEvent {
	
	private static final String APP_SETUP_STATUS_CODE = "4008";
	
	/**
	 * @param aleResult Desired result code for ALE.
	 * @param patientApp Patient App instance to be bonded.
	 */
	public AppSetupStatus(PatientApp patientApp, AppLifeEventResult aleResult) {
		super("appSetupStatus", aleResult,
				Map.of("result_code", aleResult.getCode(),
						"app_ver", patientApp.getPhoneData().getAppVer(),
						"result_description", aleResult.getDescription(),
						"app_time", System.currentTimeMillis(),
						"azure_id", patientApp.getAzureId(),
						"patient_app_ale_id", MobilityUtilities.getUUID(),
						"user_record_id", patientApp.getIdentity().getUserRecordId()));
	}
	
	@Override
	public String getALETypeCode() {
		return APP_SETUP_STATUS_CODE;
	}
}
