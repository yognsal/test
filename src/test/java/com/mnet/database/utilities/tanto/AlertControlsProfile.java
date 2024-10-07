package com.mnet.middleware.utilities.tanto;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.AlertControlsSwitch;
import com.mnet.middleware.utilities.tanto.enums.DatabaseField;

public class AlertControlsProfile extends TantoPatientSubProfile {

	private static final String ALERT_CONTROLS_PROFILE_CODE = "516";
	
	private static final String DISABLE_ST_JURISDICTION_SETTING_CODE = "1506";
	private static final String SWITCH_CODE_ON = "1483";
	private static final String BATTERY_ADVISORY_INDICATION_CODE = "2300";
	
	/**Cached pre Unity 1.6 percent pacing values for Auto2431 / Auto2432*/
	private Map<AlertControlsSwitch, String> preUnity1_6Controls = new HashMap<AlertControlsSwitch, String>();
	
	public AlertControlsProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, ALERT_CONTROLS_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		AlertControlsSwitch currentSwitch = getAlertControlsSwitch(profileSwitch);
		
		String responseSwitchValue = driver.getXMLElement(profileResponse, TantoPayloadProfileType.ALERT_CONTROLS, currentSwitch.toString());
	
		String expectedSwitchValue = getSwitchOverride(profileSwitch);
		
		switch (currentSwitch) {
			case PERCENT_BIV_THRESHOLD_ALERT:
			case PERCENT_BIV_THRESHOLD_NOT:
			case PERCENT_RV_THRESHOLD_ALERT:
			case PERCENT_RV_THRESHOLD_NOT:
				preUnity1_6Controls.put(currentSwitch, expectedSwitchValue);
				break;
			default:
				break;
		}
	
		return reportSwitchValidation(profileSwitch, responseSwitchValue, expectedSwitchValue);
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		return AlertControlsSwitch.values();
	}
	
	@Override
	protected String getSwitchOverride(TantoProfileSwitch profileSwitch) {
		String override = super.getSwitchOverride(profileSwitch);
		
		// Attributes / manual overrides should be interpreted as-is
		if ((attributeValue != null)
				|| (modOverride != null)
				|| (transmitterOverride != null)
				|| (deviceOverride != null)) {
			return override;
		}
		
		AlertControlsSwitch currentSwitch = getAlertControlsSwitch(profileSwitch);
		
		if (!(deviceSupportsAlert(interpretIfThresholdOrDuration(currentSwitch)) && isAlertSupported(currentSwitch))) {
			String interpretedDefault = (defaultValue == null) ? getDefaultSwitchValue(profileSwitch) : defaultValue;
			
			return (isSpecialPercentPacingControl(currentSwitch)) 
					? interpretIfSpecialPercentPacingControl(currentSwitch, interpretedDefault)
					: interpretedDefault;
		}
		
		return interpretIfSpecialPercentPacingControl(currentSwitch, interpretDatabaseCode(currentSwitch, override));
	}
	
	@Override
	protected String getMODDefault(TantoProfileSwitch profileSwitch) {
		AlertControlsSwitch currentSwitch = getAlertControlsSwitch(profileSwitch);
		
		String modDefault = currentSwitch.getEmergencyRoomDefault();
		
		return (modDefault == null) ? getDefaultSwitchValue(profileSwitch) : modDefault;
	}
	
	@Override
	protected String getPatientOverride(TantoProfileSwitch profileSwitch) {
		AlertControlsSwitch currentSwitch = getAlertControlsSwitch(profileSwitch);
		
		DatabaseField databaseField = currentSwitch.getDatabaseField();
		
		if (databaseField == null) {
			return null;
		}
		
		QueryResult queryResult = database.executeQuery("select " + databaseField.toString() + " from patients.patient_alert_handling pah" +
				" join extinstruments.transmitter_profile_switch tps on tps.alert_group_id = pah.alert_group_id" +
				" join patients.customer_application_patient cap on cap.customer_appl_patient_id = pah.customer_appl_patient_id" +
                " join patients.patient_device pd on pd.patient_id = cap.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where tps.switch_name = '" + interpretIfThresholdOrDuration(currentSwitch).toString() + "'" + 
                " and pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'");
		
		return queryResult.getFirstCellValue();
	}
	
	@Override
	protected String getClinicOverride(TantoProfileSwitch profileSwitch) {
		AlertControlsSwitch currentSwitch = getAlertControlsSwitch(profileSwitch);
		
		DatabaseField databaseField = currentSwitch.getDatabaseField();
		
		if (databaseField == null) {
			return null;
		}
		
		QueryResult queryResult = database.executeQuery("select " + databaseField.toString() + " from customers.clinic_alert_handling cah" +
				" join extinstruments.transmitter_profile_switch tps on tps.alert_group_id = cah.alert_group_id" +
				" join patients.customer_application_patient cap on cap.customer_application_id = cah.customer_application_id" +
                " join patients.patient_device pd on pd.patient_id = cap.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where tps.switch_name = '" + interpretIfThresholdOrDuration(currentSwitch).toString() + "'" + 
                " and pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'");
		
		return queryResult.getFirstCellValue();
	}
	
	/*
	 * Helper functions
	 */
	
	private AlertControlsSwitch getAlertControlsSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof AlertControlsSwitch)) {
			throw new RuntimeException("Invalid switch for AlertControls profile: " + profileSwitch.toString());
		}
		
		return (AlertControlsSwitch) profileSwitch;
	}
	
	/**Returns false if the device / jurisdiction is incapable of supporting the alert.
	 * Otherwise, returns true.*/
	private boolean isAlertSupported(AlertControlsSwitch currentSwitch) {
		switch (currentSwitch) {
			case DEV_IN_MRI_MODE_ALERT:
			case DEV_IN_MRI_MODE_NOT:
			case DEV_RST_MRI_MODE_ALERT:
			case DEV_RST_MRI_MODE_NOT:
				return isDeviceCapable(DatabaseField.MRI_CAPABLE_FLG);
			case LFDA_TIMEOUT_ALERT:
			case LFDA_TIMEOUT_NOT:
			case LFDA_NSLN_ALERT:
			case LFDA_NSLN_NOT:
			case LFDA_RV_NOISE_ALERT:
			case LFDA_RV_NOISE_NOT:
				return isDeviceCapable(DatabaseField.LFDA_CAPABLE_FLG);
			case PER_BIV_PACING_ALERT:
			case PER_BIV_PACING_NOT:
			case PER_RV_PACING_ALERT:
			case PER_RV_PACING_NOT:
				return isDeviceCapable(DatabaseField.DEVMED_CAPABLE_FLG);
			case PERCENT_BIV_THRESHOLD_ALERT:
			case PERCENT_BIV_THRESHOLD_NOT:
			case PERCENT_RV_THRESHOLD_ALERT:
			case PERCENT_RV_THRESHOLD_NOT:
				return isDeviceCapable(DatabaseField.MED_CAPABLE_FLG);
			case ST_MAJOR_EPISODE_ALERT:
			case ST_MAJOR_EPISODE_NOT:
			case ST_TYPE_1_ALERT:
			case ST_TYPE_1_NOT:
			case ST_TYPE_2_ALERT:
			case ST_TYPE_2_NOT:
				return isDeviceCapable(DatabaseField.ST_CAPABLE_FLG, DatabaseField.ST_PHASE2_CAPABLE_FLG)
						&& jurisdictionSupportsSTClearing();
			case HVB_PBD_ALERT:
				return deviceInBatteryAdvisory();
			default:
				return true;
		}
	}
	
	/**Returns true if switch is a post Unity 1.6 percent pacing control that requires special handling.*/
	private boolean isSpecialPercentPacingControl(AlertControlsSwitch currentSwitch) {
		switch (currentSwitch) {
			case PER_BIV_PACING_ALERT:
			case PER_BIV_PACING_NOT:
			case PER_RV_PACING_ALERT:
			case PER_RV_PACING_NOT:
				return true;
			default:
				return false;
		}
	}
	
	private String interpretDatabaseCode(AlertControlsSwitch currentSwitch, String override) {
		switch (currentSwitch.getDatabaseField()) {
			case SEVERITY_CD:
				switch (override) {
					case "934": // Urgent
					case "935": // Standard
						return "Enable";
					case "988":
						return "Disable";
					default:
						return override;
				}
			case DISPLAY_FLG:
				return interpretIfFlag(override);
			case DURATION_CD:
				switch (override) {
					case "959": // 7 days
						return "7";
					case "960": // 30 days
						return "30";
					case "961": // 60 days
						return "60";
					case "1282": // 1 day - daily DevMED
						return "1";
					default:
						return override;
				}
			case THRESHOLD:
				return override;
			default:
				throw new RuntimeException("Missing database field for switch: " + currentSwitch);
		}
	}
	
	/**
	 * Interpretation of post Unity 1.6 percent pacing controls as per Auto2431 / Auto2432.
	 * If not an applicable switch, returns the input value.
	 */
	private String interpretIfSpecialPercentPacingControl(AlertControlsSwitch currentSwitch, String switchValue) {
		String interpretedSwitchValue;
		
		switch (currentSwitch) {
			case PER_BIV_PACING_ALERT:
				interpretedSwitchValue = preUnity1_6Controls.get(AlertControlsSwitch.PERCENT_BIV_THRESHOLD_ALERT);
				break;
			case PER_BIV_PACING_NOT:
				interpretedSwitchValue = preUnity1_6Controls.get(AlertControlsSwitch.PERCENT_BIV_THRESHOLD_NOT);
				break;
			case PER_RV_PACING_ALERT:
				interpretedSwitchValue = preUnity1_6Controls.get(AlertControlsSwitch.PERCENT_RV_THRESHOLD_ALERT);
				break;
			case PER_RV_PACING_NOT:
				interpretedSwitchValue = preUnity1_6Controls.get(AlertControlsSwitch.PERCENT_RV_THRESHOLD_NOT);
				break;
			default:
				interpretedSwitchValue = null;
				break;
		}
		
		if (interpretedSwitchValue != null) {
			if (switchValue.equals("Disable") && (Integer.parseInt(profileVersion) >= 5)) {
				switchValue = interpretedSwitchValue;
			}
		}
		
		return switchValue;
	}
	
	/**
	 * Returns corresponding pacing alert switch name for RV / BIV duration & threshold.
	 * If the switch does not use threshold / duration, returns itself.
	 */
	private AlertControlsSwitch interpretIfThresholdOrDuration(AlertControlsSwitch currentSwitch) {
		switch (currentSwitch) {
			case PERCENT_BIV_PACING:
			case BIV_PACING_DURATION:
				return AlertControlsSwitch.PERCENT_BIV_THRESHOLD_ALERT;
			case PERCENT_RV_PACING:
			case RV_PACING_DURATION:
				return AlertControlsSwitch.PERCENT_RV_THRESHOLD_ALERT;
			default:
				return currentSwitch;
		}
	}
	
	/*
	 * Local DB functions
	 */
	
	/**Returns true if the device supports the alert, or the alert has no associated alert group.
	 * Otherwise, returns false.*/
	private boolean deviceSupportsAlert(TantoProfileSwitch profileSwitch) {
		String alertGroupId = database.executeQuery("select alert_group_id from extinstruments.transmitter_profile_switch" +
				" where switch_name = '" + profileSwitch.toString() + "'").getFirstCellValue();
		
		if (alertGroupId == null) {
			return true;
		}
		
		String alertSupported = database.executeQuery("select * from alerts.device_alert da" +
				" join devices.device_product dp on dp.device_product_id = da.device_product_id" +
				" join patients.patient_device pd on pd.device_product_id = dp.device_product_id" +
				" where da.alert_group_id = '" + alertGroupId + "'" +
				" and pd.device_serial_num = '" + deviceSerial + "'" +
				" and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
		
		return (alertSupported != null) ? true : false;
	}
	
	private boolean jurisdictionSupportsSTClearing() {
		String jurisdictionSetting = database.executeQuery("select switch_cd from lookup.legal_jurisdiction_setting ljs" +
				" join lookup.legal_jurisdiction lj on lj.legal_jurisdiction_id = ljs.legal_jurisdiction_id" +
				" join customers.customer c on c.legal_jurisdiction_cd = lj.legal_jurisdiction_cd" +
				" join customers.customer_application ca on ca.customer_id = c.customer_id" +
				" join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
				" join patients.patient_device pd on pd.patient_id = cap.patient_id" +
				" join devices.device_product dp on dp.device_product_id = pd.device_product_id" + 
				" where pd.device_serial_num = '" + deviceSerial + "'" +
				" and dp.device_model_num = '" + deviceModel + "'" +
				" and ljs.jurisdiction_setting_type_cd = '" + DISABLE_ST_JURISDICTION_SETTING_CODE + "'").getFirstCellValue();
		
		return (jurisdictionSetting.equals(SWITCH_CODE_ON)) ? true : false;
	}
	
	private boolean deviceInBatteryAdvisory() {
		String patientAdvisoryCode = database.executeQuery("select advisory_indication_cd from patients.patient_device pd" +
				" join devices.device_product dp on pd.device_product_id = dp.device_product_id" +
				" where pd.device_serial_num = '" + deviceSerial + "'" +
				" and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
		
		return (StringUtils.equals(patientAdvisoryCode, BATTERY_ADVISORY_INDICATION_CODE)) ? true : false;
	}
}
