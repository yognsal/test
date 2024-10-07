package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor
/**Represents fields from Tanto Maintenance sub-profile.*/
public enum MaintenanceSwitch implements TantoProfileSwitch {

	US_Interval(true),
	MAINT_PREF,
	RF_STAT_COLLECT,
	STAT_DATA_UPLD_PREF,
	MAINT_REBOOT_PREF;
	
	private boolean isXmlAttribute;
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.MAINTENANCE.getProfileClass();
	}
	
	@Override
	public DatabaseField getDatabaseField() {
		return null;
	}
	
}
