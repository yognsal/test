package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
public enum MEDSwitch implements TantoProfileSwitch {

	GS_Interval(true),
	US_Interval(true),
	SCHED_MED_PREF(DatabaseField.ENABLE_MED_FLG),
	SCHED_MED_WINDOW_PREF,
	ACTIVE_MED_SCHEDULES,
	MED_SCHEDULE_1,
	MED_SCHEDULE_2;
	
	private boolean isXmlAttribute;
	private DatabaseField databaseField;

	private MEDSwitch(boolean useAsXmlAttribute) {
		isXmlAttribute = useAsXmlAttribute;
	}
	
	private MEDSwitch(DatabaseField flag) {
		databaseField = flag;
	}
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.MED.getProfileClass();
	}
}
