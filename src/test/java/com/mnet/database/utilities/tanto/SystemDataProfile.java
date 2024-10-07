package com.mnet.middleware.utilities.tanto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoAttributeCategory;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.SystemDataSwitch;

/**
 * Business logic validation for Tanto SystemData subprofile.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class SystemDataProfile extends TantoPatientSubProfile {
	
	private static final String SYSTEM_DATA_PROFILE_CODE = "508";
	private static final String SCHEMA_VERSION = "A";
	private static final String NUMBER_OF_PROFILES = "8";
	private static final String LOCKOUT_FLAG = "OFF";
	private static final String SHORT_BTN_ACTION_DEFAULT = "FLP";
	private static final String LONG_BTN_ACTION_DEFAULT = "DCHK";
	/**Tolerated deviation in UTCServerTime in milliseconds.*/
	private static final long UTC_SERVER_TIME_TOLERANCE = 1000;
	/**Represents 9 am clinic time.*/
	private static final int PATIENT_NOTIFY_WINDOW_START_DEFAULT = 9;
	/**Represents 4 pm clinic time.*/
	private static final int PATIENT_NOTIFY_WINDOW_END_DEFAULT = 16;
	
	private Boolean deviceChanged = null;
	private String patientId = null;
	private String updatedDeviceSerial = null;
	private String updatedDeviceModel = null;
	
	public SystemDataProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, SYSTEM_DATA_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		SystemDataSwitch currentSwitch = getSystemDataSwitch(profileSwitch);
		
		String responseSwitchValue = currentSwitch.isXmlAttribute() ? 
				driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.SYSTEM_DATA, TantoAttributeCategory.SYSTEM_INFORMATION, currentSwitch.toString()) :
				driver.getXMLElement(profileResponse, TantoPayloadProfileType.SYSTEM_DATA, currentSwitch.toString());
		
		String expectedSwitchValue = getSwitchOverride(profileSwitch);
		
		// Allow for deviation of +-1 second of UTCServerTime from database value.
		if ((currentSwitch == SystemDataSwitch.UTCServerTime) && (!responseSwitchValue.equals(expectedSwitchValue))) {
			long validationTolerance = getUTCValidationOffset(responseSwitchValue, expectedSwitchValue);
			
			if (validationTolerance <= UTC_SERVER_TIME_TOLERANCE) {
				expectedSwitchValue = responseSwitchValue;
			}
		}
		
		return reportSwitchValidation(profileSwitch, responseSwitchValue, expectedSwitchValue);
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		return SystemDataSwitch.values();
	}
	
	@Override
	protected String getSwitchOverride(TantoProfileSwitch profileSwitch) {
		String override = super.getSwitchOverride(profileSwitch);
		
		// Attributes / MOD / transmitter overrides should be interpreted as-is
		if ((attributeValue != null) || (modOverride != null) || (transmitterOverride != null)) {
			return override;
		}
		
		SystemDataSwitch currentSwitch = getSystemDataSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case VOL_CTRL_PREF:
				return queryCodeById(override);
			case SHORT_BTN_ACTION:
				return (override.equals("1")) ? LOCKOUT_FLAG : SHORT_BTN_ACTION_DEFAULT;
			case LONG_BTN_ACTION:
				return (override.equals("1")) ? LOCKOUT_FLAG : LONG_BTN_ACTION_DEFAULT;
			default:
				return override;
		}
	}
	
	@Override
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		SystemDataSwitch currentSwitch = getSystemDataSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case SchemaVersion:
				return SCHEMA_VERSION;
			case DeviceModel:
				return deviceModel;
			case DeviceSerialNumber:
				return deviceSerial;
			case ProfileVersion:
				return getBaselinedProfileVersion(profileVersion);
			case UTCServerTime:
				return queryProfileDateTime("HH24:MI:SS");
			case ProfileDate:
				return queryProfileDateTime("YYYY-MM-DD");
			case NumberOfProfiles:
				return NUMBER_OF_PROFILES;
			default:
				return null;
		}
	}
	
	@Override 
	protected String getDefaultSwitchValue(TantoProfileSwitch profileSwitch) {
		SystemDataSwitch currentSwitch = getSystemDataSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case PatientNotifyWindowStart:
				return getUTCTime(PATIENT_NOTIFY_WINDOW_START_DEFAULT, 0);
			case PatientNotifyWindowEnd:
				return getUTCTime(PATIENT_NOTIFY_WINDOW_END_DEFAULT, 0);
			default:
				return super.getDefaultSwitchValue(profileSwitch);
		}
	}
	
	@Override
	protected String getMODDefault(TantoProfileSwitch profileSwitch) {
		SystemDataSwitch currentSwitch = getSystemDataSwitch(profileSwitch);
		
		String modDefault = currentSwitch.getEmergencyRoomDefault();
		
		return (modDefault == null) ? getDefaultSwitchValue(profileSwitch) : modDefault;
	}
	
	@Override
	protected String getPatientOverride(TantoProfileSwitch profileSwitch) {
		SystemDataSwitch currentSwitch = getSystemDataSwitch(profileSwitch);
		
		switch(currentSwitch) {
			case UNPAIRED_MODE:
				return hasMODTransmitter() ? "Enable" : "Disable";
			case ENROLLMENT_CHANGE:
				return isDeviceChanged() ? "Enable" : "Disable";
			case UPDATED_DEVICE_MODEL:
				return getUpdatedDeviceModel();
			case UPDATED_DEVICE_SERIAL:
				return getUpdatedDeviceSerial();
			default:
				break;
		}
		
		String queryResult = super.getPatientOverride(profileSwitch);
		
		if (currentSwitch.toString().contains("PatientNotify")) {
			int totalMins = Integer.parseInt(queryResult);
			return getUTCTime(totalMins / 60, totalMins % 60);
		} else {
			return queryResult;
		}
	}
	
	@Override
	protected String getClinicOverride(TantoProfileSwitch profileSwitch) {
		SystemDataSwitch currentSwitch = getSystemDataSwitch(profileSwitch);
		
		switch(currentSwitch) {
			case VOL_CTRL_PREF: // no clinic level value
			case MERLIN_ID:
				return null;
			case CLINIC_TYPE:
				return queryClinicType();
			default:
				return super.getClinicOverride(profileSwitch);
		}
	}
	
	/*
	 * PatientNotifyWindowStart / End only have patient and clinic-level values.
	 */
	
	@Override
	protected String getMODOverride(TantoProfileSwitch profileSwitch) {
		if (isPatientNotifyWindowAttribute(profileSwitch)) {
			return null;
		}
		
		return super.getMODOverride(profileSwitch);
	}
	
	@Override
	protected String getTransmitterOverride(TantoProfileSwitch profileSwitch) {
		if (isPatientNotifyWindowAttribute(profileSwitch)) {
			return null;
		}
		
		return super.getTransmitterOverride(profileSwitch);
	}
	
	@Override
	protected String getDeviceOverride(TantoProfileSwitch profileSwitch) {
		if (isPatientNotifyWindowAttribute(profileSwitch)) {
			return null;
		}
		
		return super.getDeviceOverride(profileSwitch);
	}
	
	/*
	 * Helper functions
	 */
	
	private SystemDataSwitch getSystemDataSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof SystemDataSwitch)) {
			throw new RuntimeException("Invalid switch for SystemData profile: " + profileSwitch.toString());
		}
		
		return (SystemDataSwitch) profileSwitch;
	}
	
	private String getUpdatedDeviceSerial() {
		if (!isDeviceChanged()) {
			return null;
		}
		
		queryUpdatedDevice();
		
		return updatedDeviceSerial;
	}
	
	private String getUpdatedDeviceModel() {
		if (!isDeviceChanged()) {
			return null;
		}
		
		queryUpdatedDevice();
		
		return updatedDeviceModel;
	}
	
	private boolean isPatientNotifyWindowAttribute(TantoProfileSwitch profileSwitch) {
		return (profileSwitch.toString().contains("PatientNotify") ? true : false);
	}
	
	/**Returns difference between response and expected UTC time in milliseconds.*/ 
	private long getUTCValidationOffset(String responseSwitchValue, String expectedSwitchValue) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		long responseTime, expectedTime;
		
		try {
			responseTime = dateFormat.parse(responseSwitchValue).getTime();
			expectedTime = dateFormat.parse(expectedSwitchValue).getTime();
		} catch (ParseException pe) {
			throw new RuntimeException("Failed to parse date " + expectedSwitchValue + " in format " + dateFormat.toPattern());
		}
		
		return Math.abs(responseTime - expectedTime);
	}
	
	/*
	 * Local DB queries
	 */
	
	/**@param dateTimeFormat Valid SQL date/time format e.g YYYY-MM-DD, HH24:MI:SS*/
	private String queryProfileDateTime(String dateTimeFormat) {
		// round timestamp to the nearest second
		return database.executeQuery("select to_char(date_trunc('second', tp.request_date_and_time + interval '500 millisecond'),'" + dateTimeFormat + "')" +
        		" from extinstruments.transmitter_profile tp" +
                " where tp.device_serial_num = '" + deviceSerial + "'" +
                " and tp.device_model_num = '" + deviceModel + "'" +
                " order by tp.last_updt_dtm desc").getFirstCellValue();
	}
	
	private String queryClinicType() {
		return database.executeQuery("select code from lookup.code c" +
				" join customers.customer_application ca on c.code_id = ca.application_cd" +
                " join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
                " join patients.patient ptnt on ptnt.patient_id = cap.patient_id" +
                " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
	}
	
	private String queryCodeById(String codeId) {
		return database.executeQuery("select code from lookup.code where code_id = '" + codeId + "'").getFirstCellValue();
	}
	
	private void queryUpdatedDevice() {
		if (updatedDeviceModel != null) {
			return;
		}
		
		List<String> queryResult = database.executeQuery("select pd.device_serial_num, pd.device_model_num" + 
				" from patients.patient_device pd" +
                " where pd.patient_id = " + patientId +
                " and pd.currently_implanted_flg = 1").getFirstRow();
		
		updatedDeviceSerial = queryResult.get(0);
		updatedDeviceModel = queryResult.get(1);
	}
	
	private boolean isDeviceChanged() {
		if (deviceChanged != null) {
			return deviceChanged;
		}
		
		List<String> queryResult = database.executeQuery("select pd.patient_id, pd.currently_implanted_flg from patients.patient_device pd" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'").getFirstRow();
		
		patientId = queryResult.get(0);
		deviceChanged = queryResult.get(1).equals("0") ? true : false;
		
		return deviceChanged;
	}
	
}