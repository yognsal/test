package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
/**Represents fields from Tanto Follow-up subprofile.*/
public enum FollowUpSwitch implements TantoProfileSwitch {

	GS_DateOfEvent(true),
	GS_TimeOfEvent(true),
	UNSCHED_FLP_PREF(DatabaseField.LOCKOUT_PATIENT_INIT_TRANS_FLG),
	SCHED_FLP_PREF(DatabaseField.SCHEDULING_METHOD_CD),
	CLEAR_EPIS_FLAG(DatabaseField.CLEAR_EPISODAL_DIAGNOSTICS_FLG),
	CLEAR_ST_FLAG(DatabaseField.CLEAR_ST_MONITOR_FLG),
	CLEAR_DIAG_FLAG(DatabaseField.CLEAR_STATS_DIAGNOSTICS_FLG),
	CLEAR_SEGM_FLAG(DatabaseField.CLEAR_EGMS_FLG),
	GDC2_SCHED_FLP_PREF(DatabaseField.GDC2_ENABLED_FLG);
	
	private boolean isXmlAttribute;
	private DatabaseField databaseField;
	
	private FollowUpSwitch(boolean useAsXmlAttribute) {
		isXmlAttribute = useAsXmlAttribute;
	}
	
	private FollowUpSwitch(DatabaseField flag) {
		databaseField = flag;
	}
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.FOLLOW_UP.getProfileClass();
	}

}
