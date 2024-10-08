package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;
import com.mnet.pojo.mobility.PhoneData;

import lombok.Getter;

/**
 * ALE used to communicate status of patient app pairing with implanted device.
 */
public class PhoneProfileChange extends AppLifeEvent {
	
	private static final String PHONE_PROFILE_CHANGE_CODE = "4000";
	
	@Getter
	private PhoneData phoneData;
	
	/**
	 * Phone profile change ALE using phone data provided during provisioning.
	 * @param patientApp Patient App instance to be bonded
	 * @param aleResult Desired result code for ALE.
	 */
	public PhoneProfileChange(PatientApp patientApp, AppLifeEventResult aleResult) {
		this(patientApp, aleResult, patientApp.getPhoneData());
		
		phoneData = patientApp.getPhoneData();
	}
	
	/**
	 * Phone profile change ALE using newly provided phone data.
	 * Existing phone data associated with the Patient App instance will be updated when this ALE is sent successfully.
	 * @param patientApp Patient App instance to be bonded
	 * @param aleResult Desired result code for ALE.
	 */
	public PhoneProfileChange(PatientApp patientApp, AppLifeEventResult aleResult, PhoneData phoneData) {
		super("phoneProfileChange", aleResult,
				Map.ofEntries(
						Map.entry("app_version", phoneData.getAppVer()),
						Map.entry("app_model", phoneData.getAppModel()),
						Map.entry("os", phoneData.getOs()),
						Map.entry("os_version", phoneData.getOsVer()),
						Map.entry("manufacturer", phoneData.getManufacturer()),
						Map.entry("mtx", phoneData.getMtx()),
						Map.entry("imei_number", phoneData.getImeiNumber()),
						Map.entry("model", phoneData.getModel()),
						Map.entry("locale", phoneData.getLocale()),
						Map.entry("result_code", aleResult.getCode()),
						Map.entry("result_description", aleResult.getDescription()),
						Map.entry("app_time", System.currentTimeMillis()),
						Map.entry("azure_id", patientApp.getAzureId()),
						Map.entry("patient_app_ale_id", MobilityUtilities.getUUID()),
						Map.entry("user_record_id", (patientApp.getIdentity() != null) ? patientApp.getIdentity().getUserRecordId() : 0)));
		
		this.phoneData = phoneData;
	}
	
	@Override
	public String getALETypeCode() {
		return PHONE_PROFILE_CHANGE_CODE;
	}
}
