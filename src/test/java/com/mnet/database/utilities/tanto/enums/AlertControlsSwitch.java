package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.Getter;

@Getter
public enum AlertControlsSwitch implements TantoProfileSwitch {
	
	/**DirectAlert: Atrial Pacing Lead Impedance Out of Range*/
	AIMP_OOR_ALERT,
	/**DirectAlert Notification: Atrial Pacing Lead Impedance Out of Range*/
	AIMP_OOR_NOT,
	/**DirectAlert: Charge Time Limit Reached*/
	CCRG_LMT_ALERT,
	/**DirectAlert Notification: Charge Time Limit Reached*/
	CCRG_LMT_NOT,
	/**DirectAlert: Device at EOS
	 * @implNote Legacy devices only i.e V-series*/
	DEV_EOS_ALERT,
	/**DirectAlert Notification: Device at EOS
	 * @implNote Legacy devices only i.e V-series*/
	DEV_EOS_NOT, 
	/**DirectAlert: Device at ERI*/
	DEV_ERI_ALERT,
	/**DirectAlert Notification: Device at ERI*/
	DEV_ERI_NOT, 
	/**DirectAlert: Device Programmed to Emergency Pacing Values*/
	DEV_EVVI_ALERT, 
	/**DirectAlert Notification: Device Programmed to Emergency Pacing Values*/
	DEV_EVVI_NOT,
	/**DirectAlert: Device Reset*/
	DEV_RST_ALERT,
	/**DirectAlert Notification: Device Reset*/
	DEV_RST_NOT,	
	/**DirectAlert: High Voltage Lead Impedance Out of Range*/
	HVIMP_OOR_ALERT,
	/**DirectAlert Notification: High Voltage Lead Impedance Out of Range*/
	HVIMP_OOR_NOT,
	/**DirectAlert: Backup VVI or AAI*/
	HW_BVVI_ALERT,
	/**DirectAlert Notification: Backup VVI or AAI*/
	HW_BVVI_NOT,
	/**DirectAlert: LV Pacing Lead Impedance Out of Range*/
	LVIMP_OOR_ALERT, 
	/**DirectAlert Notification: LV Pacing Lead Impedance Out of Range*/
	LVIMP_OOR_NOT,
	/**DirectAlert:  V. Noise Reversion*/
	NOISE_REV_ALERT,
	/**DirectAlert Notification:  V. Noise Reversion*/
	NOISE_REV_NOT,
	/**DirectAlert: Non-sustained VF Episode Occurred*/
	NSVF_EPIS_ALERT,
	/**DirectAlert Notification: Non-sustained VF Episode Occurred*/
	NSVF_EPIS_NOT,
	/**DirectAlert: Non-sustained VT Episode Occurred*/
	NSVT_EPIS_ALERT,
	/**DirectAlert Notification: Non-sustained VT Episode Occurred*/
	NSVT_EPIS_NOT,
	/**DirectAlert: Possible High Voltage Lead Issue*/
	OCD_ALERT, 
	/**DirectAlert Notification: Possible High Voltage Lead Issue*/
	OCD_NOT,
	/**DirectAlert: Possible HV Circuit Damage*/
	SOSD_ALERT, 
	/**DirectAlert Notification: Possible HV Circuit Damage*/
	SOSD_NOT,
	/**DirectAlert: RV Pacing Lead Impedance Out of Range*/
	RVIMP_OOR_ALERT, 
	/**DirectAlert Notification: RV Pacing Lead Impedance Out of Range*/
	RVIMP_OOR_NOT,
	/**DirectAlert: Tachy Therapy Disabled*/
	TTRPY_DIS_ALERT, 
	/**DirectAlert Notification: Tachy Therapy Disabled*/
	TTRPY_DIS_NOT,
	/**DirectAlert: AT/AF Episode Duration > Threshold*/
	ATAF_DUR_ALERT, 
	/**DirectAlert Notification:T/AF Episode Duration > Threshold*/
	ATAF_DUR_NOT,
	/**DirectAlert: AT/AF Burden > Threshold*/
	ATAF_WK_DUR_ALERT, 
	/**DirectAlert Notification: AT/AF Burden > Threshold*/
	ATAF_WK_DUR_NOT,
	/**DirectAlert: Average Ventricular Rate during AT/AF > Threshold*/
	ATAF_VRATE_ALERT, 
	/**DirectAlert Notification: Average Ventricular Rate during AT/AF > Threshold*/
	ATAF_VRATE_NOT,
	/**DirectAlert: Successful ATP Pacing delivered*/
	ATP_RX_SUCCESS_ALERT, 
	/**DirectAlert Notification: Successful ATP Pacing delivered*/
	ATP_RX_SUCCESS_NOT,
	/**DirectAlert: High Voltage Therapy Delivered*/
	HV_TRPY_ALERT, 
	/**DirectAlert Notification: High Voltage Therapy Delivered*/
	HV_TRPY_NOT,
	/**DirectAlert: ST Episode detected
	 * @implNote ST Clearing disabled for US jurisdiction*/
	ST_MAJOR_EPISODE_ALERT, 
	/**DirectAlert Notification: ST Episode detected
	 * @implNote ST Clearing disabled for US jurisdiction*/
	ST_MAJOR_EPISODE_NOT,
	/**DirectAlert: Therapy Accelerated Rhythm*/
	TRPY_ACCEL_ALERT, 
	/**DirectAlert Notification: Therapy Accelerated Rhythm*/
	TRPY_ACCEL_NOT, 	
	SPARE_1_ALERT, SPARE_1_NOT,
	SPARE_2_ALERT, SPARE_2_NOT,
	SPARE_3_ALERT, SPARE_3_NOT,
	SPARE_4_ALERT, SPARE_4_NOT,
	SPARE_5_ALERT, SPARE_5_NOT,
	/**DirectAlert: High Ventricular Rate detected
	 * @implNote Most pacemaker (device_category_cd = 955) devices*/
	HIGH_VRATE_EPISODE_ALERT, 
	/**DirectAlert Notification: High Ventricular Rate detected
	 * @implNote Most pacemaker (device_category_cd = 955) devices*/
	HIGH_VRATE_EPISODE_NOT,
	V_AUTOCAP_ALERT, V_AUTOCAP_NOT,
	ACAP_CONFIRM_ALERT, ACAP_CONFIRM_NOT,
	RVCAP_CONFIRM_ALERT, RVCAP_CONFIRM_NOT,
	LVCAP_CONFIRM_ALERT, LVCAP_CONFIRM_NOT,
	/**DirectAlert: Congestion Duration Exceeded Programmed Threshold*/
	CONG_MON_ALERT, 
	/**DirectAlert Notification: Congestion Duration Exceeded Programmed Threshold*/
	CONG_MON_NOT,
	/**DirectAlert: Device in MRI settings
	 * @implNote MRI capable devices only*/
	DEV_IN_MRI_MODE_ALERT, 
	/**DirectAlert Notification: Device in MRI settings
	 * @implNote MRI capable devices only*/
	DEV_IN_MRI_MODE_NOT,
	/**@implNote MRI capable device + DEV_RST_ALERT*/
	DEV_RST_MRI_MODE_ALERT,
	/**@implNote MRI capable device + DEV_RST_NOT*/
	DEV_RST_MRI_MODE_NOT,
	/**DirectAlert: Longevity Analysis (requires Tech Services support)*/
	EARLY_DEPLETION_DETECTED_ALERT,
	/**DirectAlert Notification: Longevity Analysis (requires Tech Services support)*/
	EARLY_DEPLETION_DETECTED_NOT, 
	/**DirectAlert: Sustained RV lead noise detected
	 * @implNote Same as LFDA_RV_NOISE_ALERT*/
	LFDA_TIMEOUT_ALERT, 
	/**DirectAlert Notification: Sustained RV lead noise detected
	 * @implNote Same as LFDA_RV_NOISE_NOT*/
	LFDA_TIMEOUT_NOT,
	/**DirectAlert: ST Episode Detected - Type II
	 * @implNote ST / ST Phase II capable devices only. ST Clearing disabled for US jurisdiction.*/
	ST_TYPE_2_ALERT, 
	/**DirectAlert Notification: ST Episode Detected - Type II
	 * @implNote ST / ST Phase II capable devices only. ST Clearing disabled for US jurisdiction.*/
	ST_TYPE_2_NOT,
	/**DirectAlert: Three or more VT/VF episodes in 24 hours*/
	VT_VF_3_PER_DAY_ALERT, 
	/**DirectAlert Notification: Three or more VT/VF episodes in 24 hours*/
	VT_VF_3_PER_DAY_NOT,
	/**DirectAlert: All therapies exhausted*/
	THERAPY_EXHAUSTED_ALERT, 
	/**DirectAlert Notification: All therapies exhausted*/
	THERAPY_EXHAUSTED_NOT,
	/**DirectAlert: At least one shock unsuccessful*/
	HV_THERAPY_UNSUC_ALERT, 
	/**DirectAlert Notification: At least one shock unsuccessful*/
	HV_THERAPY_UNSUC_NOT,
	/**DirectAlert: VT/VF episode occurred*/
	VT_VF_OCCURED_ALERT, 
	/**DirectAlert Notification: VT/VF episode occurred*/
	VT_VF_OCCURED_NOT,	
	/**DirectAlert: Non-sustained RV lead noise detected
	 * @implNote LFDA capable devices only*/
	LFDA_NSLN_ALERT, 
	/**DirectAlert Notification: Non-sustained RV lead noise detected
	 * @implNote LFDA capable devices only*/
	LFDA_NSLN_NOT,
	/**DirectAlert: Sustained RV lead noise / oversensing detected
	 * @implNote LFDA capable devices only*/
	LFDA_RV_NOISE_ALERT, 
	/**DirectAlert Notification: Sustained RV lead noise / oversensing detected
	 * @implNote LFDA capable devices only*/
	LFDA_RV_NOISE_NOT,
	/**DirectAlert: ST Type I Episode Detected
	 * @implNote ST / ST Phase II capable devices only. ST Clearing disabled for US jurisdiction.*/
	ST_TYPE_1_ALERT, 
	/**DirectAlert Notification: ST Type I Episode Detected
	 * @implNote ST / ST Phase II capable devices only. ST Clearing disabled for US jurisdiction.*/
	ST_TYPE_1_NOT,
	/**DirectAlert: Battery Performance
	 * @implNote Requires entry in advisory_patient_info before patient enrollment*/
	HVB_PBD_ALERT, 
	/**DirectAlert: Battery Performance
	 * @implNote Requires entry in advisory_patient_info before patient enrollment*/
	HVB_PBD_NOT,
	/**DirectAlert: Percent BiV pacing less than {0}% over {1} days
	 * @implNote 3xxx-xx series devices*/
	PERCENT_BIV_THRESHOLD_ALERT, 
	/**DirectAlert Notification: Percent BiV pacing less than {0}% over {1} days
	 * @implNote 3xxx-xx series devices*/
	PERCENT_BIV_THRESHOLD_NOT,
	/**DirectAlert: BiV Percent Pacing Less Than Limit*/
	PER_BIV_PACING_ALERT, 
	/**DirectAlert Notification: BiV Percent Pacing Less Than Limit*/
	PER_BIV_PACING_NOT,
	PERCENT_BIV_PACING, BIV_PACING_DURATION,
	/**DirectAlert: Percent RV pacing less than {0}% over {1} days
	 * @implNote 2xxx-xx series devices*/
	PERCENT_RV_THRESHOLD_ALERT, 
	/**DirectAlert Notification: Percent RV pacing less than {0}% over {1} days
	 * @implNote 2xxx-xx series devices*/
	PERCENT_RV_THRESHOLD_NOT,
	/**DirectAlert: RV Percent Pacing Greater Than Limit*/
	PER_RV_PACING_ALERT, 
	/**DirectAlert Notification: RV Percent Pacing Greater Than Limit*/
	PER_RV_PACING_NOT,
	PERCENT_RV_PACING, RV_PACING_DURATION;
	
	private String emergencyRoomDefault;
	private DatabaseField databaseField;
	
	private AlertControlsSwitch() {
		String name = this.name();

		if (name.contains("ALERT")) {
			switch (name) {
				case "NOISE_REV_ALERT":
				case "NSVF_EPIS_ALERT":
				case "NSVT_EPIS_ALERT":
					break;
				default: 
					emergencyRoomDefault = "Enable";
					break;
			}
			
			databaseField = DatabaseField.SEVERITY_CD;
		} else if (name.contains("NOT")) {
			databaseField = DatabaseField.DISPLAY_FLG;
		} else if (name.contains("DURATION")) {
			databaseField = DatabaseField.DURATION_CD;
		} else if (name.equals("PERCENT_BIV_PACING") || name.equals("PERCENT_RV_PACING")){ 
			databaseField = DatabaseField.THRESHOLD;
		}
	}
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.ALERT_CONTROLS.getProfileClass();
	}
}
