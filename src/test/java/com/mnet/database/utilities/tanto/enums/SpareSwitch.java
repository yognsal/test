package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
/**Represents fields from Tanto Spare subprofile.*/
public enum SpareSwitch implements TantoProfileSwitch {

	GS_DateOfEvent(true),
	GS_Interval(true),
	GS_WeeklyEvent(true),
	GS_TimeOfEvent(true),
	GS_UnscheduledEvent(true),
	US_DateOfEvent(true),
	US_Interval(true),
	US_WeeklyEvent(true),
	US_TimeOfEvent(true),
	US_UnscheduledEvent(true),
	SPARE_FLAG1,
	SPARE_FLAG2,
	SPARE_FLAG3,
	SPARE_FLAG4,
	SPARE_FLAG5,
	SPARE_FLAG6,
	SPARE_FLAG7,
	SPARE_FLAG8,
	SPARE_FLAG9,
	SPARE_FLAG10,
	SPARE_INTEGER1,
	SPARE_INTEGER2,
	SPARE_INTEGER3,
	SPARE_INTEGER4,
	SPARE_INTEGER5,
	SPARE_INTEGER6,
	SPARE_INTEGER7,
	SPARE_INTEGER8,
	SPARE_INTEGER9,
	SPARE_INTEGER10,
	SPARE_REAL1,
	SPARE_REAL2,
	SPARE_REAL3,
	SPARE_REAL4,
	SPARE_REAL5,
	SPARE_REAL6,
	SPARE_REAL7,
	SPARE_REAL8,
	SPARE_REAL9,
	SPARE_REAL10,
	SPARE_TEXT1(DatabaseField.TRANS_SCRIPT_CLEAR_COMMAND),
	SPARE_TEXT2(DatabaseField.TRANS_SCRIPT_EXECUTION_SEQ),
	SPARE_TEXT3,
	SPARE_TEXT4,
	SPARE_TEXT5,
	SPARE_TEXT6,
	SPARE_TEXT7,
	SPARE_TEXT8,
	SPARE_TEXT9,
	SPARE_TEXT10;
	
	private boolean isXmlAttribute;
	private DatabaseField databaseField;
	
	private SpareSwitch(boolean useAsXmlAttribute) {
		isXmlAttribute = useAsXmlAttribute;
	}
	
	private SpareSwitch(DatabaseField dbField) {
		databaseField = dbField;
	}
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.SPARE.getProfileClass();
	}
	
}
