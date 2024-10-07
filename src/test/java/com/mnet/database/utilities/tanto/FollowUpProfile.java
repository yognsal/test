package com.mnet.middleware.utilities.tanto;

import java.time.LocalDateTime;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoAttributeCategory;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.DatabaseField;
import com.mnet.middleware.utilities.tanto.enums.FollowUpSwitch;

/**
 * Business logic validation for Tanto Follow-up subprofile.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class FollowUpProfile extends TantoPatientSubProfile {
	
	private static final String FOLLOW_UP_PROFILE_CODE = "504";
	private static final String CALCULATED_SCHEDULE_CODE = "509";
	private static final String DISCRETE_SCHEDULE_CODE = "510";
	private static final String UNSCHEDULED_CODE = "511";
	private static final String ST_JURISDICTION_SETTING_CODE = "1506";
	private static final String SWITCH_CODE_ON = "1483";
	private static final LocalDateTime BASELINE_FOLLOWUP_UTC = LocalDateTime.of(2000, 01, 01, 2, 0);
	
	public FollowUpProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, FOLLOW_UP_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		FollowUpSwitch currentSwitch = getFollowUpSwitch(profileSwitch);
		
		String responseSwitchValue = currentSwitch.isXmlAttribute() ? 
				driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.FOLLOW_UP, TantoAttributeCategory.GENERATE_SCHEDULE, currentSwitch.toString()) :
				driver.getXMLElement(profileResponse, TantoPayloadProfileType.FOLLOW_UP, currentSwitch.toString());
		
		return reportSwitchValidation(profileSwitch, responseSwitchValue, getSwitchOverride(profileSwitch));
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
		
		FollowUpSwitch currentSwitch = getFollowUpSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case UNSCHED_FLP_PREF:
				if (defaultValue == null) {
					override = interpretIfLockout(override);
				}
				break;
			case SCHED_FLP_PREF:
				if (override.equals(UNSCHEDULED_CODE)) {
					return "Disable";
				} else if (override.equals(CALCULATED_SCHEDULE_CODE) || override.equals(DISCRETE_SCHEDULE_CODE)) {
					return "Enable";
				} else {
					report.logStep(TestStep.builder().reportLevel(ReportLevel.ERROR).message("Invalid scheduling method in DB for SCHED_FLP_PREF").build());
					return null;
				}
			case CLEAR_ST_FLAG:
				if (!(isDeviceCapable(DatabaseField.ST_CAPABLE_FLG, DatabaseField.ST_PHASE2_CAPABLE_FLG) 
						&& isSTJurisdictionSettingEnabled())) {
					return "Disable";
				}
				break;
			case GDC2_SCHED_FLP_PREF:
				if (!isDeviceCapable(DatabaseField.GDC2_CAPABLE_FLG)) {
					return "Disable";
				}
				break;
			default:
				break;
		}
		
		return interpretIfFlag(override);
	}
	
	@Override
	protected TantoProfileSwitch[] getAllSwitches() {
		return FollowUpSwitch.values();
	}
	
	@Override
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		FollowUpSwitch currentSwitch = getFollowUpSwitch(profileSwitch);
		
		if (!currentSwitch.isXmlAttribute()) {
			return null;
		}
		
		String followUpDate = getFollowUpDate();
		
		switch (currentSwitch) {
			case GS_DateOfEvent:
				return (followUpDate != null) ? 
						followUpDate.split(" ")[0] :
						getUTCTime(BASELINE_FOLLOWUP_UTC, DateTimeFormat.YYYY_MM_DD);
			case GS_TimeOfEvent:
				return (followUpDate != null) ? 
						followUpDate.split(" ")[1] :
						getUTCTime(BASELINE_FOLLOWUP_UTC, DateTimeFormat.HH_MI_SS);
			default:
				throw new RuntimeException("Unhandled FollowUp attribute: " + currentSwitch.toString());
		}
	}
	
	@Override
	protected String getPatientOverride(TantoProfileSwitch profileSwitch) {
		FollowUpSwitch currentSwitch = getFollowUpSwitch(profileSwitch);
		
		if (currentSwitch == FollowUpSwitch.GDC2_SCHED_FLP_PREF) { // depends only on clinic-level setting
			return null;
		}
		
		return super.getPatientOverride(profileSwitch);
	}
	
	/*
	 * Helper functions
	 */
	
	private FollowUpSwitch getFollowUpSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof FollowUpSwitch)) {
			throw new RuntimeException("Invalid switch for FollowUp profile: " + profileSwitch.toString());
		}
		
		return (FollowUpSwitch) profileSwitch;
	}
	
	/*
	 * Local DB functions
	 */
	
	/**@param dateTimeFormat Postgres timestamp format for return value.*/
	private String getFollowUpDate() {		
		return database.executeQuery("select to_char(cap.next_followup_date at time zone '" + queryTimeZone() + "' at time zone 'UTC', 'YYYY-MM-DD HH24:MI:SS')" +
        		" from patients.customer_application_patient cap" +
                " join patients.patient_device pd on pd.patient_id = cap.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
	}
	
	private boolean isSTJurisdictionSettingEnabled() {
		String switchCode = database.executeQuery("select switch_cd from lookup.legal_jurisdiction_setting ljs" + 
				" join lookup.legal_jurisdiction lj on ljs.legal_jurisdiction_id = lj.legal_jurisdiction_id"  +
				" join customers.customer cust on lj.legal_jurisdiction_cd = cust.legal_jurisdiction_cd" +
				" join customers.customer_application ca on cust.customer_id = ca.customer_id" +
				" join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
				" join patients.patient ptnt on ptnt.patient_id = cap.patient_id" + 
				" join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
				" join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
				" where pd.device_serial_num = '" + deviceSerial + "'" +
				" and dp.device_model_num = '" + deviceModel + "'" +
				" and ljs.jurisdiction_setting_type_cd = '" + ST_JURISDICTION_SETTING_CODE + "'").getFirstCellValue();
		
		return (switchCode.equals(SWITCH_CODE_ON)) ? true : false;
	}
}
