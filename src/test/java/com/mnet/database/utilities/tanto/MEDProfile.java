package com.mnet.middleware.utilities.tanto;

import java.time.LocalDateTime;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoAttributeCategory;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.DatabaseField;
import com.mnet.middleware.utilities.tanto.enums.DeviceCheckSwitch;
import com.mnet.middleware.utilities.tanto.enums.MEDSwitch;
import com.mnet.middleware.utilities.tanto.enums.SystemDataSwitch;

/**
 * Business logic validation for Tanto MED subprofile.
 * @version Summer 2023
 * @author Arya Biswas
 */
public class MEDProfile extends TantoPatientSubProfile {

	private static final String MED_PROFILE_CODE = "996";
	private static final String GS_INTERVAL_DAILY = "24";
	private static final String US_INTERVAL_DAILY = "1";
	private static final String SCHED_MED_WINDOW_PREF_MED = "Enable";
	private static final String ACTIVE_MED_SCHEDULES_MED = "1";
	private static final String BATTERY_ADVISORY_ALERT_GROUP = "71";
	private static final int SOFTWARE_PRODUCT_ADVISORY_THRESHOLD = 9625;
	
	public MEDProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, MED_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		MEDSwitch currentSwitch = getMEDSwitch(profileSwitch);
		
		String responseSwitchValue = null;
		
		switch (currentSwitch) {
			case GS_Interval:
				responseSwitchValue = driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.MED, TantoAttributeCategory.GENERATE_SCHEDULE, currentSwitch.toString());
				break;
			case US_Interval:
				responseSwitchValue = driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.MED, TantoAttributeCategory.UPLOAD_SCHEDULE, currentSwitch.toString());
				break;
			default:
				responseSwitchValue = driver.getXMLElement(profileResponse, TantoPayloadProfileType.MED, currentSwitch.toString());
				break;
		}
		
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
		
		MEDSwitch currentSwitch = getMEDSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case SCHED_MED_PREF:
				if (!isDeviceCapable(DatabaseField.MED_CAPABLE_FLG, DatabaseField.DEVMED_CAPABLE_FLG)) {
					override = "Disable";
				}
				return interpretIfFlag(override);
			case MED_SCHEDULE_1:
			case MED_SCHEDULE_2:
				return getMEDSchedule(override);
			default:
				return override;
		}
	}
	
	@Override
	protected TantoProfileSwitch[] getAllSwitches() {
		return MEDSwitch.values();
	}
	
	@Override
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		MEDSwitch currentSwitch = getMEDSwitch(profileSwitch);
		
		if (!currentSwitch.isXmlAttribute()) {
			return null;
		}
		
		if (currentSwitch == MEDSwitch.GS_Interval && isDeviceCapable(DatabaseField.MED_CAPABLE_FLG)) {
			return GS_INTERVAL_DAILY;
		}
		
		boolean dailyMED = inDailyMEDAdvisory();
		
		if (!dailyMED) {
			return super.getAttributeValue(profileSwitch);
		} else if (currentSwitch == MEDSwitch.GS_Interval) {
			return GS_INTERVAL_DAILY;
		} else {
			return US_INTERVAL_DAILY;
		}
	}
	
	@Override
	protected String getPatientOverride(TantoProfileSwitch profileSwitch) {
		MEDSwitch currentSwitch = getMEDSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case SCHED_MED_WINDOW_PREF:
				if (isDeviceCapable(DatabaseField.MED_CAPABLE_FLG)) {
					return SCHED_MED_WINDOW_PREF_MED;
				}
				break;
			case ACTIVE_MED_SCHEDULES:
				if (isDeviceCapable(DatabaseField.MED_CAPABLE_FLG, DatabaseField.DEVMED_CAPABLE_FLG)) {
					return ACTIVE_MED_SCHEDULES_MED;
				}
				break;
			default:
				break;
		}
		
		return super.getPatientOverride(profileSwitch);
	}
	
	/*
	 * Helper functions
	 */
	
	private MEDSwitch getMEDSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof MEDSwitch)) {
			throw new RuntimeException("Invalid switch for FollowUp profile: " + profileSwitch.toString());
		}
		
		return (MEDSwitch) profileSwitch;
	}
	
	/**Converts MED_SCHEDULE values from clinic time to UTC in format HHMM*/
	private String getMEDSchedule(String databaseDefault) {
		int hours = Integer.parseInt(databaseDefault.substring(0, 2));
		int minutes = Integer.parseInt(databaseDefault.substring(2, 4));
		
		String utcTime = getUTCTime(LocalDateTime.now().withHour(hours).withMinute(minutes), DateTimeFormat.HH_MI_SS);
		
		return utcTime.replace(":", "").substring(0, 4);
	}
	
	/**Conditions for GS_Interval / US_Interval deviation as per Auto5176*/
	private boolean inDailyMEDAdvisory() {
		return (!isTransmitterUnpaired())
					&& deviceInBatteryAdvisory()
					&& clinicSupportsBatteryAdvisory()
					&& (!(transmitterSoftwareSupportsAdvisory()
							&& isTransmitterFirmwareUpgraded()
							&& usesDailyDirectAlertCheck()));
	}
	
	private boolean isTransmitterUnpaired() {
		String unpairedMode = driver.getXMLElement(profileResponse, TantoPayloadProfileType.SYSTEM_DATA, SystemDataSwitch.UNPAIRED_MODE.toString());
		
		return (unpairedMode.equals("Enable")) ? true : false;
	}
	
	private boolean usesDailyDirectAlertCheck() {
		String dailyDirectAlert = driver.getXMLElement(profileResponse, TantoPayloadProfileType.DEVICE_CHECK, DeviceCheckSwitch.SCHED_DCHK_PREF.toString());
		
		return (dailyDirectAlert.equals("Enable")) ? true : false;
	}
	
	/**Checks major / minor version for firmware-based advisory. Format: EX2000 v4.6 PR_4.90*/
	private boolean transmitterSoftwareSupportsAdvisory() {
		String[] softwareVersion = transmitterSWVersion.substring(transmitterSWVersion.indexOf("PR_") + 3).split("\\.");
		
		int majorVersion = Integer.parseInt(softwareVersion[0]);
		
		if (majorVersion > 8) {
			return true;
		}
		
		int minorVersion = Integer.parseInt(softwareVersion[1]);
		
		return (minorVersion > 22) ? true : false;
	}
	
	/*
	 * Local DB functions
	 */
	
	private boolean deviceInBatteryAdvisory() {
		QueryResult advisoryPatientQuery = database.executeQuery("select * from patients.advisory_patient_info api"
				+ " where api.device_serial_number = '" + deviceSerial + "'"
				+ " and api.device_model_number = '" + deviceModel + "'");
		
		return (advisoryPatientQuery.getFirstCellValue() != null) ? true : false;
	}
	
	private boolean clinicSupportsBatteryAdvisory() {
		QueryResult clinicAdvisoryQuery = database.executeQuery("select * from customers.clinic_alert_handling cah"
				+ " join patients.customer_application_patient cap on cap.customer_application_id = cah.customer_application_id"
				+ " join patients.patient_device pd on pd.patient_id = cap.patient_id"
				+ " join devices.device_product dp on dp.device_product_id = pd.patient_device_id"
				+ " where pd.device_serial_num = '" + deviceSerial + "'"
			    + " and dp.device_model_num = '" + deviceModel + "'"
			    + " and cah.alert_group_id = '" + BATTERY_ADVISORY_ALERT_GROUP + "'");
		
		if (clinicAdvisoryQuery.getFirstCellValue() != null) {
			return true;
		}
		
		QueryResult productVersionQuery = database.executeQuery("select spv.product_ver_number from customers.customer_application ca"
				+ " join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id"
				+ " join patients.patient_device pd on pd.patient_id = cap.patient_id"
				+ " join devices.device_product dp on dp.device_product_id = pd.patient_device_id"
				+ " join system.software_product_version spv on ca.webapp_version_id = spv.software_product_version_id"
				+ " where pd.device_serial_num = ''"
				+ " and dp.device_model_num = ''");
		
		int productVersion = Integer.parseInt(productVersionQuery.getFirstCellValue());
		
		return (productVersion > SOFTWARE_PRODUCT_ADVISORY_THRESHOLD) ? true : false;
	}
	
	private boolean isTransmitterFirmwareUpgraded() {
		String firmwareFlag = database.executeQuery("select fw_pbd_patch_flg from patients.patient_device pd"
				+ " join devices.device_product dp on dp.device_product_id = pd.device_product_id"
				+ " where pd.device_serial_num = '" + deviceSerial + "'"
				+ " and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
				
		if (firmwareFlag == null) {
			return false;
		}
		
		return (firmwareFlag.equals("1")) ? true : false;
	}
}
