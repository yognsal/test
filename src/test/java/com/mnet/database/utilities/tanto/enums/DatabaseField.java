package com.mnet.middleware.utilities.tanto.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor
/**
 * Represents a PCS database field.
 * (usually in patients / customers schema)
 */
public enum DatabaseField {
	PATIENT_MESSAGING_START_TIME,
	PATIENT_MESSAGING_END_TIME,
	TRANSMITTER_VOLUME_CD,
	LOCKOUT_PATIENT_INIT_TRANS_FLG,
	LOCKOUT_PATIENT_INIT_DC_FLG,
	MERLIN_NET_ID,
	SCHEDULING_METHOD_CD,
	CLEAR_EPISODAL_DIAGNOSTICS_FLG,
	CLEAR_ST_MONITOR_FLG,
	CLEAR_STATS_DIAGNOSTICS_FLG,
    CLEAR_EGMS_FLG,
    GDC2_ENABLED_FLG,
	DEVICE_CHECK_FLG,
	ENABLE_MED_FLG,
	SEVERITY_CD,
	DISPLAY_FLG,
	DURATION_CD,
	THRESHOLD,
	TRANS_SCRIPT_CLEAR_COMMAND,
	TRANS_SCRIPT_EXECUTION_SEQ,
	ST_CAPABLE_FLG(true),
	ST_PHASE2_CAPABLE_FLG(true),
	GDC2_CAPABLE_FLG(true),
	MED_CAPABLE_FLG(true),
	DEVMED_CAPABLE_FLG(true),
	MRI_CAPABLE_FLG(true),
	LFDA_CAPABLE_FLG(true),
	RF_CAPABLE_FLG(true);
	
	private boolean isDeviceCapability;
}
