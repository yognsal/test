package com.mnet.middleware.utilities.tanto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.tanto.enums.DatabaseField;

/**
 * Represents a subprofile in a Tanto patient profile response.
 * @version Spring 2023
 * @author Arya Biswas
 */
public abstract class TantoPatientSubProfile extends TantoSubProfile {
	
	protected String attributeValue;
	protected String modOverride;
	protected String transmitterOverride;
	protected String deviceOverride;
	protected String defaultValue;
	protected String profilePayloadCode;
	
	private Boolean hasMODTransmitter;
	
	/**Product id for EX1100 transmitter*/
	private static final String INDUCTIVE_TRANSMITTER_PRODUCT_ID = "100000";
	private static final String ACTIVE_PATIENT_STATUS_CD = "46";
	private static final String UTC_IDENTIFIER = "Universal";
	
	/**Any higher profile versions will be baselined to this version.*/
	protected static final int BASELINE_PROFILE_VERSION = 7;
	
	/**Represents a human-readable date / time format.*/
	public enum DateTimeFormat {
		YYYY_MM_DD,
		HH_MI_SS
	}

	protected TantoPatientSubProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, 
			String profilePayloadTypeCode, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, useSoftAssert, onlyReportFailures);
		profilePayloadCode = profilePayloadTypeCode;
	}
	
	/**
	 * Obtains expected value for Tanto XML attributes.
	 * @return System-level default attribute. Override in subclass for custom behavior.
	 * @implNote These are not technically switches, and hence do not follow the standard override behavior.
	 */
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		if (!profileSwitch.isXmlAttribute()) {
			return null;
		}
		
		return database.executeQuery("select payload_attr_default_value" +
				" from extinstruments.transmitter_profile_attribute" +
				" where profile_payload_type_cd = '" + profilePayloadCode + "'" +
				" and payload_attribute_name like '%" + profileSwitch.toString() + "%'").getFirstCellValue();
	}
	
	/**
	 * Defines ER default for applicable switches, wherever the value deviates from the system-wide default.
	 */
	protected String getMODDefault(TantoProfileSwitch profileSwitch) {
		return getDefaultSwitchValue(profileSwitch);
	}
	
	protected String getMODOverride(TantoProfileSwitch profileSwitch) {
		return database.executeQuery("select cts.switch_value" +
                " from customers.customer_transmitter_switch cts" +
				" join customers.customer_transmitter ct on ct.customer_transmitter_id = cts.customer_transmitter_id" +
                " join extinstruments.transmitter_profile_switch tps on tps.transmitter_profile_switch_id = cts.transmitter_profile_switch_id" +
                " join customers.customer_application ca on ca.customer_id = ct.customer_id" +
				" join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
				" join patients.patient ptnt on ptnt.patient_id = cap.patient_id" +
				" join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
				" join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
				" where pd.device_serial_num = '" + deviceSerial + "'" + 
				" and dp.device_model_num = '" + deviceModel + "'" +
				" and tps.switch_name = '" + profileSwitch.toString() + "'").getFirstCellValue();
	}
	
	protected String getTransmitterOverride(TantoProfileSwitch profileSwitch) {
		return database.executeQuery("select pts.switch_value" +
                " from patients.patient_transmitter_switch pts" +
                " join extinstruments.transmitter_profile_switch tps on tps.transmitter_profile_switch_id = pts.transmitter_profile_switch_id" +
                " join patients.patient ptnt on ptnt.patient_id = pts.patient_id" +
                " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'" +
                " and tps.switch_name = '" + profileSwitch.toString() + "'").getFirstCellValue();
	}
	
	protected String getDeviceOverride(TantoProfileSwitch profileSwitch) {
		return database.executeQuery("select dps.device_switch_default_value" +
                " from extinstruments.device_product_switch dps" +
                " join extinstruments.transmitter_profile_switch tps on dps.transmitter_profile_switch_id = tps.transmitter_profile_switch_id" +
                " join devices.device_product dp on dps.device_product_id = dp.device_product_id" +
                " join patients.patient_device pd on dp.device_product_id = pd.device_product_id" +
                " join patients.patient ptnt on pd.patient_id = ptnt.patient_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'" +
                " and tps.switch_name = '" + profileSwitch.toString() + "'").getFirstCellValue();
	}
	
	/**
	 * Queries database for patient-level value of switch.
	 * Override in subclass for custom behavior.
	 */
	protected String getPatientOverride(TantoProfileSwitch profileSwitch) {
		DatabaseField databaseField = profileSwitch.getDatabaseField();
		
		if (databaseField == null) {
			return null;
		}
		
		return database.executeQuery("select ptnt." + databaseField.toString() + 
                " from patients.patient ptnt" +
                " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
	}
	
	/**
	 * Queries database for clinic-level value of switch.
	 * Override in subclass for custom behavior.
	 */
	protected String getClinicOverride(TantoProfileSwitch profileSwitch) {
		DatabaseField databaseField = profileSwitch.getDatabaseField();
		
		if (databaseField == null) {
			return null;
		}
		
		return database.executeQuery("select cust." + databaseField.toString() +
				" from customers.customer cust" +
                " join customers.customer_application ca on cust.customer_id = ca.customer_id" +
                " join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
                " join patients.patient ptnt on ptnt.patient_id = cap.patient_id" +
                " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
	}
	
	@Override
	protected String getSwitchOverride(TantoProfileSwitch profileSwitch) {
		modOverride = null;
		defaultValue = null;
		
		attributeValue = getAttributeValue(profileSwitch);
		
		if (attributeValue != null) { // XML attribute - does not use switch override logic
			return attributeValue;
		}
		
		if (hasMODTransmitter()) {
			modOverride = getMODOverride(profileSwitch);
			
			return (modOverride == null) ? getMODDefault(profileSwitch) : modOverride;
		}
		
		transmitterOverride = getTransmitterOverride(profileSwitch);
		
		if (transmitterOverride != null) {
			return transmitterOverride;
		}
		
		deviceOverride = getDeviceOverride(profileSwitch);
		
		if (deviceOverride != null) {
			return deviceOverride;
		}
		
		String patientOverride = getPatientOverride(profileSwitch);
		
		if (patientOverride != null) {
			return patientOverride;
		}
		
		String clinicOverride = getClinicOverride(profileSwitch);
		
		if (clinicOverride != null) {
			return clinicOverride;
		}
		
		defaultValue = getDefaultSwitchValue(profileSwitch);
		
		return defaultValue;
	}
	
	@Override
	protected String getDefaultSwitchValue(TantoProfileSwitch profileSwitch) {
		return database.executeQuery("select tps.switch_default_value" +
                " from extinstruments.transmitter_profile_switch tps" +
                " join extinstruments.transmitter_profile_version tpv on tpv.transmitter_profile_switch_id = tps.transmitter_profile_switch_id" +
                " join lookup.code cd on tpv.profile_version_cd = cd.code_id" +
                " and tps.switch_name = '" + profileSwitch.toString() + "'" +
                " and cd.code = '" + getBaselinedProfileVersion(profileVersion) + "'").getFirstCellValue();
	}
	
	/**Returns queriable time zone string e.g. US/Central*/
	protected String queryTimeZone() {
		String location = database.executeQuery("select code_desc from lookup.code co" +
	            " join customers.customer cust on cust.time_zone_cd = co.code_id" +
	            " join customers.customer_application ca on cust.customer_id = ca.customer_id" +
	            " join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
	            " join patients.patient ptnt on ptnt.patient_id = cap.patient_id" +
	            " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
	            " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
	            " where pd.device_serial_num = '" + deviceSerial + "'" +
	            " and dp.device_model_num = '" + deviceModel + "'" +
		        " and cap.patient_application_status_cd = '" + ACTIVE_PATIENT_STATUS_CD + "'").getFirstCellValue();
				
		String timezone = executeZoneQuery(location);
		
		if (timezone == null) { // No matching location - find equivalent GMT time zone
			String gmtOffset = location.substring(location.indexOf("GMT"), location.indexOf(")"));
			
			List<List<String>> matchingCities = database.executeQuery("select code_desc from lookup.code where code_qualifier = 'Time_Zone_Cd' and code_desc like '%" + gmtOffset + "'").getAllRows();
			
			for (List<String> currentRow : matchingCities) {
				String currentLocation = currentRow.get(0);
				
				if (currentLocation.equals(location)) {
					continue;
				}
				
				timezone = executeZoneQuery(currentLocation);
				
				if (timezone != null) {
					break;
				}
			}
		}
		
		return timezone;
	}
	
	/**
	 * Checks if patient's device has at least one of the specified capabilities (devices.device_product).
	 */
	protected boolean isDeviceCapable(DatabaseField... databaseFields) {
		String deviceCapability = "";
		
		for (DatabaseField field : databaseFields) {
			String fieldName = field.toString();
			
			if (!field.isDeviceCapability()) {
				throw new RuntimeException("Database field " + fieldName + " does not represent a device capability (devices.device_product)");
			}
			
			deviceCapability += fieldName + "::boolean or ";
		}
		
		deviceCapability = StringUtils.removeEnd(deviceCapability, " or ");
		
		QueryResult deviceCapabilityQuery = database.executeQuery("select " + deviceCapability + 
				" from devices.device_product dp" +
				" where dp.device_model_num = '" + deviceModel + "'");
		
		return (deviceCapabilityQuery.getFirstCellValue().equals("t")) ? true : false;
	}
	
	/**
	 * Converts local clinic time to UTC time in the desired format. 
	 * @param clinicDateTime Pass desired local clinic date and time (time zone will be calculated)
	 * @param format Desired format of timestamp (YYYY-MM-DD or HH24:MI:SS) */
	protected String getUTCTime(LocalDateTime clinicDateTime, DateTimeFormat format) {
		ZonedDateTime clinicTime = ZonedDateTime.of(clinicDateTime, ZoneId.of(queryTimeZone()));       
		ZonedDateTime utcTime = clinicTime.withZoneSameInstant(ZoneOffset.UTC);
		
		switch (format) {
			case YYYY_MM_DD:
				return utcTime.format(DateTimeFormatter.ISO_LOCAL_DATE);
			case HH_MI_SS:
				return utcTime.format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0, 5) + ":00"; // ignore seconds precision
			default:
				throw new RuntimeException("Unexpected date / time format when parsing UTC time");
		}
	}
	
	/**
	 * Converts today's local clinic time to UTC time in the format HH:MI:SS, 
	 * @param hours Hours of clinic time (in 24 hour format)
	 * @param minuates Minutes of clinic time*/
	protected String getUTCTime(int hours, int minutes) {
		return getUTCTime(LocalDateTime.now().withHour(hours).withMinute(minutes), DateTimeFormat.HH_MI_SS);
	}
		
	/**
	 * Returns true if the clinic is a Merlin On Demand (ER) clinic.
	 */
	protected boolean hasMODTransmitter() {
		if (hasMODTransmitter == null) {
			hasMODTransmitter = queryMODTransmitter();
		}
		
		return hasMODTransmitter;
	}
	
	/**Inverts switch value for boolean lockout flags.*/
	protected String interpretIfLockout(String override) {
		switch (override) {
			case "0":
				return "1";
			case "1":
				return "0";
			default:
				return override;
		}
	}
	
	/**
	 * Returns Enable / Disable if the string represents a boolean flag.
	 * Otherwise, returns itself.*/
	protected String interpretIfFlag(String override) {
		switch (override) { // Interpret boolean flags
			case "0":
				return "Disable";
			case "1":
				return "Enable";
			default:
				return override;
		}
	}
	
	/**
	 * Checks profile version against the baseline.
	 * If profile version is greater than the baseline, returns the baseline.
	 */
	protected String getBaselinedProfileVersion(String profileVersion) {
		return (Integer.parseInt(profileVersion) > BASELINE_PROFILE_VERSION) ?
				Integer.toString(BASELINE_PROFILE_VERSION) : profileVersion;
	}
	
	/**
	 * Returns colon (:) separated list of devices not supported by the clinic.
	 */
	protected String getModelExclusion() {
		List<List<String>> excludedDevices = database.executeQuery("select dp.device_model_num" + 
				" from devices.device_product dp" +
				" where dp.device_product_id not in" + 
					" (select cadp.device_product_id" + 
					" from customers.clinic_appl_device_product cadp" +
					" join patients.customer_application_patient cap on cap.customer_application_id = cadp.customer_application_id" + 
					" join patients.patient_device pd on pd.patient_id = cap.patient_id" +
					" join devices.device_product dp2 on dp2.device_product_id = pd.device_product_id" +
					" where pd.device_serial_num = '" + deviceSerial + "'" + 
					" and dp2.device_model_num = '" + deviceModel + "')").getAllRows();
		
		if (excludedDevices.size() == 0) {
			return null;
		}
		
		String profileResponse = "";
		
		for (List<String> device : excludedDevices) {
			if (!profileResponse.equals("")) {
				profileResponse += ":";
			}
			
			profileResponse += device.get(0);
		}
		
		return profileResponse;
	}
	
	/**
	 * Helper functions
	 */
	
	private boolean queryMODTransmitter() {
		String customerTransmitterUser = database.executeQuery("select ct.create_userid" +
				" from customers.customer_transmitter ct" +
				" join customers.customer_application ca on ca.customer_id = ct.customer_id" +
				" join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
				" join patients.patient ptnt on ptnt.patient_id = cap.patient_id" +
				" join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
				" join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
				" join patients.device_transmitter dt on dt.patient_device_id = pd.patient_device_id" +
				" where ct.transmitter_serial_num = dt.transmitter_serial_num" +
				" and pd.device_serial_num = '" + deviceSerial + "'" +
				" and dp.device_model_num = '" + deviceModel + "'" +
				" and dt.transmitter_product_id = " + INDUCTIVE_TRANSMITTER_PRODUCT_ID).getFirstCellValue();
		
		return (customerTransmitterUser != null ? true : false);
	}
	
	/**Fetches zone identifier from database in format Region/Location*/
	private String getZoneIdentifier(String location) {
		String zoneIdentifier = location.substring(location.indexOf(") ") + 2); // extract zone from "(GMT+offset) zone"
		
		if (zoneIdentifier.contains(", ")) { // multiple cities - format as '%location1%|%location2%'
			String[] locations = zoneIdentifier.split(", ");
			zoneIdentifier = "";
			
			for (int index = 0; index < locations.length; index++) {
				zoneIdentifier += "%/" + locations[index] + "%";
				
				if (index != locations.length - 1) {
					zoneIdentifier += "|";
				}
			}
		} else {
			if (zoneIdentifier.contains(UTC_IDENTIFIER)) {
				zoneIdentifier = UTC_IDENTIFIER;
			} else if (zoneIdentifier.contains("Time")) { // represents a time zone rather than a location (Mountain Time, Pacific Time, etc)
				zoneIdentifier = zoneIdentifier.split(" ")[0];
			}
			
			zoneIdentifier = "%/" + zoneIdentifier + "%";
		}
		
		return zoneIdentifier.replace(" ", "_");
	}
	
	private String executeZoneQuery(String location) {
		return database.executeQuery("select name from pg_catalog.pg_timezone_names ptn where name similar to '" + getZoneIdentifier(location) + "'").getFirstCellValue();
	}
}
