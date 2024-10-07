package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
/**Represents fields from Tanto System Data subprofile.*/
public enum SystemDataSwitch implements TantoProfileSwitch {

	SchemaVersion(true),
	DeviceModel(true),
	DeviceSerialNumber(true),
	ProfileVersion(true),
	UTCServerTime(true),
	ProfileDate(true),
	NumberOfProfiles(true),
	PatientNotifyWindowStart(true, DatabaseField.PATIENT_MESSAGING_START_TIME),
	PatientNotifyWindowEnd(true, DatabaseField.PATIENT_MESSAGING_END_TIME),
	PROFILE_SYNC_PREF,
	ADETECT_DIALUP_NUM,
	UNPAIRED_MODE,
	ENROLLMENT_CHANGE,
	NOTIFY_DELAY_ALERT,
	NOTIFY_DELAY_FLP,
	NOTIFY_DELAY_MED,
	NOTIFY_DELAY_SERVER,
	ALLWD_UNSCHED_EVENTS,
	MINOR_VERSION,
	SCHED_REF_TIME,
	UPDATED_DEVICE_MODEL,
	UPDATED_DEVICE_SERIAL,
	VOL_CTRL_PREF("OFF", DatabaseField.TRANSMITTER_VOLUME_CD),
	CLINIC_TYPE("ER"),
	SHORT_BTN_ACTION(DatabaseField.LOCKOUT_PATIENT_INIT_TRANS_FLG),
	LONG_BTN_ACTION("FLP", DatabaseField.LOCKOUT_PATIENT_INIT_DC_FLG),
	MERLIN_ID(DatabaseField.MERLIN_NET_ID);

	private boolean isXmlAttribute;
	private String emergencyRoomDefault;
	private DatabaseField databaseField;
	
	private SystemDataSwitch(boolean useAsXmlAttribute) {
		isXmlAttribute = useAsXmlAttribute;
	}
	
	private SystemDataSwitch(String erDefault) {
		emergencyRoomDefault = erDefault;
	}
	
	private SystemDataSwitch(DatabaseField dbFlag) {
		databaseField = dbFlag;
	}
	
	private SystemDataSwitch(boolean useAsXmlAttribute, DatabaseField dbFlag) {
		this(useAsXmlAttribute);
		databaseField = dbFlag;
	}
	
	private SystemDataSwitch(String erDefault, DatabaseField dbFlag) {
		this(erDefault);
		databaseField = dbFlag;
	}
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.SYSTEM_DATA.getProfileClass();
	}
	
}
