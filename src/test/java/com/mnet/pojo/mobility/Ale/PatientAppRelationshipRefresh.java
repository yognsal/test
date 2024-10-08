package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

/**
 * ALE used to renew PatientApp credential refresh
 */
public class PatientAppRelationshipRefresh extends AppLifeEvent {

	private static final String PATIENTAPP_RELATIONSHIP_REFRESH_CODE = "4017";

	/**
	 * @param patientApp Patient App instance to be bonded.
	 */
	public PatientAppRelationshipRefresh(PatientApp patientApp, long timestamp) {
		super("PatientAppIdentityRefresh", AppLifeEventResult.SUCCESS,
				Map.of("app_time", timestamp,
						"azure_id", patientApp.getAzureId(), 
						"user_record_id", patientApp.getIdentity().getUserRecordId(), 
						"device_serial", patientApp.getDeviceSerial()));
	}

	@Override
	public String getALETypeCode() {
		return PATIENTAPP_RELATIONSHIP_REFRESH_CODE;
	}

}
