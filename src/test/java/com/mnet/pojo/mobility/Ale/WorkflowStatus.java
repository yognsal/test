package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.MobilityUtilities.ProfileType;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

/**
 * ALE used to communicate status of patient app pairing with implanted device.
 * 
 * @author Arya Biswas
 * @version Q1 2024
 */
public class WorkflowStatus extends AppLifeEvent {
	
	private static final String WORKFLOW_STATUS_CODE = "4004";
	
	/**
	 * @param aleResult Desired result code for ALE.
	 * @param patientApp Patient App instance to be bonded.
	 */
	public WorkflowStatus(PatientApp patientApp, AppLifeEventResult aleResult, ProfileType profileType) {
		super("workflowStatus", aleResult,
				Map.of("workflow_id", profileType.getProfileCode(),
						"content_version", PatientAppDBUtilities.getContentVersion(
								patientApp.getCurrentTest(), patientApp.getDeviceSerial(), profileType.getProfileCode()),
						"result_code", aleResult.getCode(),
						"result_description", aleResult.getDescription(),
						"app_time", System.currentTimeMillis(),
						"azure_id", patientApp.getAzureId(),
						"patient_app_ale_id", MobilityUtilities.getUUID(),
						"user_record_id", patientApp.getIdentity().getUserRecordId()));
	}
	
	@Override
	public String getALETypeCode() {
		return WORKFLOW_STATUS_CODE;
	}
}
