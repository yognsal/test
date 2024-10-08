package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

/**
 * ALE used to renew PatientApp Identity refresh
 */
public class PatientAppIdentityRefresh extends AppLifeEvent {

	private static final String PATIENTAPP_IDENTITY_REFRESH_CODE = "4016";

	/**
	 * @param patientApp Patient App instance to be bonded.
	 */
	public PatientAppIdentityRefresh(PatientApp patientApp, long timestamp) {
		super("PatientAppIdentityRefresh", AppLifeEventResult.SUCCESS,
				Map.of("app_time", timestamp,
						"public_key", patientApp.getKeys().getPublicKeyEncoded(),
						"azure_id", patientApp.getAzureId(), 
						"user_record_id", patientApp.getIdentity().getUserRecordId(), 
						"device_serial", patientApp.getDeviceSerial()));
	}

	@Override
	public String getALETypeCode() {
		return PATIENTAPP_IDENTITY_REFRESH_CODE;
	}

}
