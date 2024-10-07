package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor
/**Represents fields from Tanto GDC subprofile.*/
public enum GDCSwitch implements TantoProfileSwitch {

	GS_Interval(true),
	US_Interval(true),
	SCHED_GDC_PREF,
	CLEAR_GDC_FLAG;
	
	private boolean isXmlAttribute;
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.GDC.getProfileClass();
	}
	
	@Override
	public DatabaseField getDatabaseField() {
		return null;
	}
}