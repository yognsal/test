package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;
import com.mnet.pojo.mobility.EncryptedPayload;

/**
 * ALE used to upload a transmission from the implanted device.
 * Should not be sent via d2c mechanism (use /ngq-transmission-service/session-record instead).
 */
public class SessionRecordPayload extends AppLifeEvent {
	
	private static final String SESSION_RECORD_CODE = "4001";
	
	/**
	 * @param patientApp Patient App instance to be bonded.
	 * @param transmission Encrypted transmission payload to upload.
	 * @param recordId UUID associated with the session record payload.
	 */
	public SessionRecordPayload(PatientApp patientApp, EncryptedPayload transmission, String recordId, long timestamp) {
		super("sessionRecordPayload", AppLifeEventResult.SUCCESS,
				Map.ofEntries(
						Map.entry("app_time", timestamp),
						Map.entry("azure_id", patientApp.getAzureId()),
						Map.entry("user_record_id", patientApp.getIdentity().getUserRecordId()),
						Map.entry("filename", transmission.getFileName()),
						Map.entry("workflow_id", Integer.toString(transmission.getWorkflowId())),
						Map.entry("payload_label", transmission.getPayloadLabel()),
						Map.entry("record_id", recordId),
						Map.entry("transmission_key", transmission.getTransmissionKey()),
						Map.entry("random_seed", transmission.getRandomSeed()),
						Map.entry("payload_hash_value", transmission.getPayloadHashValue()),
						Map.entry("patient_app_identity_record", patientApp.getIdentity().getPatientAppIdentityRecord()),
						Map.entry("payload", transmission.getEncryptedSessionRecord())));
	}
	
	@Override
	public String getALETypeCode() {
		return SESSION_RECORD_CODE;
	}
	
}
