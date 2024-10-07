package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public enum DeviceCheckSwitch implements TantoProfileSwitch {
	GS_Interval(true),
	GS_TimeOfEvent(true),
	UNSCH_DCHK_PREF(DatabaseField.LOCKOUT_PATIENT_INIT_DC_FLG),
	SCHED_DCHK_PREF(DatabaseField.DEVICE_CHECK_FLG);
	
	private boolean isXmlAttribute;
	private DatabaseField databaseField;
		
	private DeviceCheckSwitch(boolean useAsXmlAttribute) {
		isXmlAttribute = useAsXmlAttribute;
	}
	
	private DeviceCheckSwitch(DatabaseField flag) {
		databaseField = flag;
	}
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.DEVICE_CHECK.getProfileClass();
	}
	
}
