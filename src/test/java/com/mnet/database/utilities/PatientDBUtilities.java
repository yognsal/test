package com.mnet.database.utilities;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.pojo.patient.Patient.ScheduleType;
import com.mnet.webapp.utilities.StringPropertyParser;

/**
 * Utilities for all database calls
 * 
 * @author NAIKKX12
 *
 */
public class PatientDBUtilities extends DBUtilities {
	private TestReporter report;
	private DatabaseConnector database;
	Random random = new Random();
	// Code in database that is expected when Email is delivered (233)
	private String codeForEmailDelivered = "233";
	private String pairedTransmitterStatus = "t";

	private StringPropertyParser stringParser;

	/**
	 * Enum of columns for Patient (patients.patient) table
	 */
	public enum PatientColumns {
		PATIENT_ID("patient_id"), EMERGENCY_CONTACT_ADDRESS_ID("emergency_contact_address_id"),PATIENT_ADDRESS_ID("patient_address_id"),
		EMERGENCY_CONTACT_FIRSTNAME("emergency_contact_first_name"),PRIMARY_PHONE_ID("primary_phone_id"),
		EMERGENCY_CONTACT_LASTNAME("emergency_contact_last_name"),
		NOTIFY_EMERGENCY_CONTACT("notify_emergency_contact_flg"), MERLIN_NET_ID("merlin_net_id");

		private String patientColumn;

		private PatientColumns(String patientColumn) {
			this.patientColumn = patientColumn;
		}

		public String getPatientColumn() {
			return this.patientColumn;
		}
	}

	/**
	 * Enum of columns for Patient Address (patients.patient_address) table
	 */
	public enum PatientAddressColumns {
		ADDRESS_ID("patient_address_id"), ADDRESS_FIELD_1("street_address"), ADDRESS_FIELD_2("street_address2"),
		ADDRESS_FIELD_3("street_address3"), CITY("city"), ZIPCODE("zip_code"), COUNTRY_CODE("country_cd"),
		STATE_CODE("state_province");

		private String patientAddressColumn;

		private PatientAddressColumns(String patientAddressColumn) {
			this.patientAddressColumn = patientAddressColumn;
		}

		public String getPatientAddressColumn() {
			return this.patientAddressColumn;
		}
	}

	/**
	 * Enum of columns for Patient Follow UP (patients.followup_date) table
	 */
	public enum PatientFollowupDateColumns {
		SCHEDULED_DATE("scheduled_time"), FOLLOW_UP_STATUS_CODE("followup_status_cd");

		private String patientFollowupColumn;

		private PatientFollowupDateColumns(String patientFollowupColumn) {
			this.patientFollowupColumn = patientFollowupColumn;
		}

		public String getPatientFollowupColumn() {
			return this.patientFollowupColumn;
		}
	}

	/**
	 * Enum of columns for Patient Follow UP (patients.followup_date) table
	 */
	public enum PatientDiscreteDateColumn {
		DISCRETE_DATE("discrete_date_time");

		private String column;

		private PatientDiscreteDateColumn(String column) {
			this.column = column;
		}

		public String getDiscreteDateColumn() {
			return this.column;
		}
	}

	/**
	 * Enum of columns for RT Cache (transmissions.rt_cache) table
	 */
	public enum RTCacheTableColumns {
		PATIENT_ID("patient_id"), SCHEDULED_TIME("scheduled_time"), NEXT_FOLLOWUP_DATE("next_followup_date"), LATEST_COMMENT("latest_patient_comment"),
		FIRST_NAME("first_name"), LAST_NAME("last_name");

		private String rtCacheTableColumn;

		private RTCacheTableColumns(String rtCacheTableColumn) {
			this.rtCacheTableColumn = rtCacheTableColumn;
		}

		public String getRTCacheTableColumns() {
			return this.rtCacheTableColumn;
		}
	}

	public enum BatteryAdvisoryCode{
		Battery_Advisory(2300), Pacemaker_Header(2900), Pacemaker_Laser_Adhesion(2901);

		private int code;

		private BatteryAdvisoryCode(int code) {
			this.code = code;
		}

		public int getAdvisoryCode() {
			return this.code;
		}
	}

	/**
	 * Enum fo columns for Patient Transmission (transmissions.transmission) table
	 */
	public enum PatientTransmissionColumns {
		SCHEDULED_FLAG("scheduled_flg"), DEVICE_SERIAL_NUM("device_serial_num"), PATIENT_ID("patient_id");

		private String patientTransmissionColumn;

		private PatientTransmissionColumns(String patientTransmissionColumn) {
			this.patientTransmissionColumn = patientTransmissionColumn;
		}

		public String getPatientTransmissionColumn() {
			return this.patientTransmissionColumn;
		}
	}

	public PatientDBUtilities(TestReporter testReporter, DatabaseConnector databaseConnector) {
		report = testReporter;
		database = databaseConnector;
		stringParser = new StringPropertyParser(
				FrameworkProperties.getProperty("WEB_MESSAGE_PROPERTIES_FILE"));
	}

	public enum DeviceModelType {
		ICD("ICD"), PACEMAKER("Pacemaker"), ICDANDPACEMAKER("ICD/Pacemaker"), ICM("ICM"), NGQ("NGQ"),
		ICMANDNGQ("ICM/NGQ");

		private String deviceType;

		private DeviceModelType(String deviceType) {
			this.deviceType = deviceType;
		}

		public String getDeviceModel() {
			return this.deviceType;
		}
	}

	public enum TransmitterProfileColumns {
		TRANSMITTER_SERIAL("transmitter_serial_num"), PROFILE_TYPE_CODE("profile_type_cd"), PROFILE_STATUS_CODE("profile_status_cd");

		private String column;

		private TransmitterProfileColumns(String column) {
			this.column = column;
		}

		public String getTransmitterProfileColumns() {
			return this.column;
		}
	}

	public enum DeviceFlags {
		MED, DEVMED, NOTMEDORDEVMED, GDC2_CAPABLE, NOT_GDC2_CAPABLE, ST_PHASE_CAPABLE, NOT_ST_PHASE_CAPABLE,
		ST_PHASE2_CAPABLE, NOT_ST_PHASE2_CAPABLE;
	}

	public enum PatientStatus {
		ACTIVE("Active"), RELEASED_TRANSFERRED("Released/Transfer to Another Clinic"), EXPIRED("Patient Expired"),
		NOT_FOUND("null");

		private String status;

		private PatientStatus(String status) {
			this.status = status;
		}

		public String getPatientStatus() {
			return this.status;
		}

		public static PatientStatus fromString(String status) {
			for (PatientStatus code : PatientStatus.values()) {
				if (code.status.equalsIgnoreCase(status)) {
					return code;
				}
			}
			return null;
		}
	}

	// TODO: Convert all DB calls to static

	/**
	 * Get patient status - active/ released and so on
	 * 
	 * @param firstName
	 * @param lastName
	 * @return
	 */
	public PatientStatus getPatientStatus(String firstName, String lastName) {
		HashMap<String, String> data = getPatient(firstName, lastName);
		if (data.size() > 0) {
			// patient id column index = 1
			BigInteger id = new BigInteger(data.get("patient_id"));
			String dbQuery = "select * from patients.customer_application_patient cap2 where patient_id = " + id;

			QueryResult queryResult = database.executeQuery(dbQuery);
			if (queryResult.getAllRows().isEmpty()) {
				report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			}

			List<String> output = queryResult.getFirstRow();

			if (output.size() > 0) {
				// patient_application_status_cd column index - 5
				id = new BigInteger(output.get(4));
				dbQuery = "select * from lookup.code where code_qualifier = 'Patient_Status_Cd' and code_id = " + id;
				output = database.executeQuery(dbQuery).getFirstRow();

				if (output.size() > 0) {
					// code_desc column index - 3
					return PatientStatus.fromString(output.get(2));
				}
			}
		}

		return PatientStatus.NOT_FOUND;
	}

	/**
	 * Verify paired flag based on transmitter's serial number
	 */
	public boolean verifyPairedFlag(String transmitterSerialNum) {
		String dbQuery = "select * from patients.device_transmitter dt where transmitter_serial_num ='<transmitterSerialNum>'";
		dbQuery = dbQuery.replace("<transmitterSerialNum>", transmitterSerialNum);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return false;
		}
		List<String> patientData = queryResult.getFirstRow();
		if (patientData.size() > 0) {
			if (patientData.get(5).equals(pairedTransmitterStatus)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get ST and ST Phase 2 devices list
	 */
	public List<String> getSTAndSTPhase2Devices() {
		String dbQuery = "select device_model_num from devices.device_product dp where st_capable_flg = '1' and st_phase2_capable_flg = '1';";
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}
		return queryResult.getColumn("device_model_num");
	}

	public List<String> getDevicesBasedOnDeviceFlag(DeviceFlags deviceFlag) {
		String query = "select * from devices.device_product dp where <deviceFlag>;";

		switch (deviceFlag) {
		case MED:
			query = query.replace("<deviceFlag>", "med_capable_flg = '1'");
			break;
		case DEVMED:
			query = query.replace("<deviceFlag>", "devmed_capable_flg = '1'");
			break;
		case NOTMEDORDEVMED:
			query = query.replace("<deviceFlag>", "med_capable_flg = '0' and devmed_capable_flg = '0'");
			break;
		case GDC2_CAPABLE:
			query = query.replace("<deviceFlag>", "gdc2_capable_flg = '1'");
			break;
		case NOT_GDC2_CAPABLE:
			query = query.replace("<deviceFlag>", "gdc2_capable_flg = '0'");
			break;
		case ST_PHASE2_CAPABLE:
			query = query.replace("<deviceFlag>", "st_phase2_capable_flg = '1'");
			break;
		case NOT_ST_PHASE2_CAPABLE:
			query = query.replace("<deviceFlag>", "st_phase2_capable_flg = '0'");
			break;
		case ST_PHASE_CAPABLE:
			query = query.replace("<deviceFlag>", "st_capable_flg = '1'");
			break;
		case NOT_ST_PHASE_CAPABLE:
			query = query.replace("<deviceFlag>", "st_capable_flg = '0'");
			break;
		}

		report.logStep(TestStep.builder().message("Executing database query: " + query).build());
		QueryResult queryResult = database.executeQuery(query);
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + query).build());
			return null;
		}
		return queryResult.getColumn("device_model_num");
	}

	/**
	 * Get ICD or Pacemaker or ICD/Pacemaker or ICM or NGQ or ICM/NGQ device lists
	 * based on paramter TODO: detailed DB query to get devices based on device type
	 * provided
	 */
	public List<String> getIcdOrNgqDeviceList(DeviceModelType modelType) {
		List<String> devices = new ArrayList<>();
		String deviceModelType = "";
		String dbQuery = "select device_full_description from devices.device_product dp;";
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		List<String> deviceModels = queryResult.getColumn("device_full_description");

		switch (modelType) {
		case ICD:
			deviceModelType = "Unify";
			break;
		case PACEMAKER:
			deviceModelType = "Accent™";
			break;
		case ICDANDPACEMAKER:
			String[] icdOrPacemaker = { "Unify", "Accent" };
			deviceModelType = icdOrPacemaker[random.nextInt(icdOrPacemaker.length)];
			break;
		case ICM:
			deviceModelType = "ICM";
			break;
		case NGQ:
			String[] ngqDeviceFamilies = { "CDDRA", "CDVRA", "CDHFA" };
			deviceModelType = ngqDeviceFamilies[random.nextInt(ngqDeviceFamilies.length)];
			break;
		case ICMANDNGQ:
			String[] icmOrNgq = { "Gallant" };
			deviceModelType = icmOrNgq[random.nextInt(icmOrNgq.length)];
			break;
		}

		for (String deviceModel : deviceModels) {
			if (deviceModel.contains(deviceModelType)) {
				deviceModel = deviceModel.split(",")[1].trim();
				devices.add(deviceModel);
			}
		}
		return devices;
	}

	/**
	 * Function to verify if transmission once processed is notified through email.
	 * The function Fetches the transmission id using the deviceSerial for which the
	 * transmission was done. With the help of the transmission ID, it fetches the
	 * notitfication ID for external notification. In the end it verifies that
	 * notificaiton ID if it mataches with the delivered notification ID i.e. 233
	 * TODO: Add join to reduce clutter and better readability
	 * 
	 * @param deviceSerial
	 * @return boolean true if mail received else false
	 */

	public boolean verifyTransmissionMailReceived(String patientId) {
		String dbQuery_fetch_tranmsission_id = "select transmission_id from transmissions.transmission where patient_id = '<patientId>' order by create_dtm desc";
		String dbQuery_fetch_notification_id = "select external_notification_id from alerts.external_notification en where transmission_id = '<transmissionID>'";
		String dbQuery_fetch_nds_status_cd = "select * from alerts.alert_notification an where external_notification_id ='<notif_id>';";
		String transmissionID = null;
		dbQuery_fetch_tranmsission_id = dbQuery_fetch_tranmsission_id.replace("<patientId>", patientId);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery_fetch_tranmsission_id).build());
		List<String> requiredData1 = database.executeQuery(dbQuery_fetch_tranmsission_id).getFirstRow();

		if (requiredData1.size() > 0) {
			transmissionID = requiredData1.get(0);
			report.logStep(TestStep.builder().message("TRANSMISSION ID = " + transmissionID).build());
		} else {
			report.logStep(TestStep.builder().reportLevel(TestStep.ReportLevel.ERROR).message("No transmission found for the patient ID: " + patientId).build());
		}

		dbQuery_fetch_notification_id = dbQuery_fetch_notification_id.replace("<transmissionID>", transmissionID);
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery_fetch_notification_id).build());
		QueryResult queryResult = database.executeQuery(dbQuery_fetch_notification_id);
		if(queryResult.getAllRows().isEmpty()) {
			return false;
		}
		List<String> requiredData2 = queryResult.getFirstRow();

		if (requiredData2.size() <= 0) {
			report.logStep(TestStep.builder().reportLevel(TestStep.ReportLevel.ERROR).message("No notification ID for the following transmission ID: " + transmissionID).build());
			return false;
		}

		dbQuery_fetch_nds_status_cd = dbQuery_fetch_nds_status_cd.replace("<notif_id>", requiredData2.get(0));
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery_fetch_nds_status_cd).build());
		List<String> requiredData3 = database.executeQuery(dbQuery_fetch_nds_status_cd).getFirstRow();

		if (requiredData3.size() > 0) {
			if (requiredData3.get(1).contains(codeForEmailDelivered)) {
				return true;
			}
		}
		return false;

	}

	/**
	 * Function to check if transmitter number is associated with the patient. Set
	 * boolean true to check if transmitter is associated. Set boolean false if
	 * transmitter is removed from the patient
	 */

	public boolean checkTransmitterAssociated(boolean associated, String deviceSerialNum) {
		String dbQuery = "select * from patients.patient_device pd where device_serial_num = '<deviceSerialNum>'";
		dbQuery = dbQuery.replace("<deviceSerialNum>", deviceSerialNum);
		String dbQuery2 = "select * from patients.device_transmitter dt where patient_device_id = '<deviceId>'";
		String deviceId = getSpecificCellValue(dbQuery, "patient_device_id");

		dbQuery2 = dbQuery2.replace("<deviceId>", deviceId);
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery2).build());
		QueryResult queryResult = database.executeQuery(dbQuery2);

		if (!associated && queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("Transmitter Serial Number associated with the patient is removed/not updated").build());
			return true;
		}

		if (associated && queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No output from executed query").build());
			return false;
		}

		report.logStep(TestStep.builder().message("Transmitter Serial Number associated with the patient").build());
		return true;
	}

	/**
	 * Verify the implant status and device model for the device selected when
	 * enrolling the patient
	 * 
	 * @param deviceModel - This needs to be fetched from patient profile page
	 */

	public boolean verifyDeviceImplantStatus(String deviceSerial, String deviceModel) {
		boolean success = true;
		HashMap<String, String> patientDeviceDetails = getPatientDeviceDetails("device_serial_num", deviceSerial);
		HashMap<String, String> deviceDetails = getDeviceDetails(deviceSerial);
		if (!patientDeviceDetails.get("currently_implanted_flg").equals("1")) {
			report.logStep(TestStep.builder().reportLevel(TestStep.ReportLevel.ERROR).message("The device Serial is not currently implanted: " + deviceSerial).screenshotType(TestStep.ScreenshotType.SCROLLING).build());
			success = false;
		}

		if (!deviceModel.contains(deviceDetails.get("device_name"))) {
			report.logStep(TestStep.builder().reportLevel(TestStep.ReportLevel.ERROR).message("The device model passed thorugh UI does not match in DB: " + deviceModel).build());
			success = false;
		}

		return success;

	}

	/**
	 * Function to retrieve transmission info of provided patient including null (in
	 * case of orphaned transmission)
	 */
	public List<Map<String, String>> getPatientTransmissionData(String patientId) {
		String dbQuery;
		if (patientId == null) {
			dbQuery = "select * from transmissions.transmission t where patient_id is null";
		} else {
			dbQuery = FrameworkProperties.getApplicationVersion() == "d3" ? 
					"select * from transmissions.transmission t where patient_id = '" + patientId + "'" : 
						"select * from transmissions.transmission t join transmissions.transmission_device td on td.transmission_id = t.transmission_id "
						+ "join patients.patient_device pd on td.device_serial_num = pd.device_serial_num where pd.patient_id = '" + patientId + "'";
		}

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	public List<Map<String, String>> getPatientTransmissionData(PatientTransmissionColumns columnName, String filterValue) {
		String dbQuery = "select * from transmissions.transmission t";
		return getDBContents(report, database,dbQuery, columnName.getPatientTransmissionColumn(), filterValue);
	}

	/**
	 * Get clinical comments based on patient ID
	 * 
	 * @implNote Please note that for getting clinical comments, we are using
	 *           primary_key - patient_id to get the patient. Any other information
	 *           passed to get the patient transmission might result in unnecessary
	 *           data. Since our main target is to get transmission for same patient
	 *           or patient that is used in test, we need ot use patient ID here as
	 *           it the primary key. Using any other column value was resulting in
	 *           data containing multiple patient ID, so it was becoming hard to
	 *           track
	 */
	@Deprecated
	public List<Map<String, String>> getClinicalComments(String patientId) {
		String dbQuery = "select * from transmissions.programmer_comment pc join (select transmission_id from transmissions.transmission t where patient_id = '<patientId>' order by create_dtm desc limit 1) as recentData on recentData.transmission_id = pc.transmission_id;";
		dbQuery = dbQuery.replace("<patientId>", patientId);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}
	
	public List<Map<String, String>> getClinicalComment(String deviceSerial) {
		String dbQuery = "select * from transmissions.programmer_comment pc where transmission_id in ( select transmission_id from transmissions.transmission_device where device_serial_num= '<deviceSerial>');";
		dbQuery = dbQuery.replace("<deviceSerial>", deviceSerial);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**
	 * Function to get rows under a specific column based on DB query
	 * 
	 * @param query      - full database query passed as parameter
	 * @param columnName - Column name under which you need the rows
	 * @return Rows
	 */

	public List<String> storeRows(String query, String columnName) {
		report.logStep(TestStep.builder().message("Executing database query: " + query).build());
		QueryResult queryResult = database.executeQuery(query);
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + query).build());
			return null;
		}
		List<String> requiredRows = queryResult.getColumn(columnName);
		return requiredRows;
	}

	/**
	 * Function to get a specific cell value
	 * 
	 * @param query      - full database query passed as parameter
	 * @param columnName - Column name under which you need the rows
	 * @return Rows
	 */

	public String getSpecificCellValue(String query, String columnName) {
		report.logStep(TestStep.builder().message("Executing database query: " + query).build());
		QueryResult queryResult = database.executeQuery(query);
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + query).build());
			return null;
		}
		List<String> requiredRow = queryResult.getColumn(columnName);
		return requiredRow.get(0);
	}

	/**
	 * Get Transmitter Details for the patient
	 * Use - getTransmitterDetails(String columnName, String filterValue)
	 */
	@Deprecated
	public HashMap<String, String> getTransmitterDetails(String deviceSerialNumber) {
		String deviceID = getSpecificCellValue(
				"select * from patients.patient_device pd where device_serial_num = '" + deviceSerialNumber + "'",
				"patient_device_id");
		String dbQuery = "select * from patients.device_transmitter dt where patient_device_id = '<deviceID>'";
		dbQuery = dbQuery.replace("<deviceID>", deviceID);

		return executeDBQuery(dbQuery);
	}

	/**
	 * Updated method to get transmitter details for a patient.
	 * @implNote Transmitter details are fetched from patient.device_transmitter table by using patient_device_id which will stay unique.
	 */
	public HashMap<String, String> getTransmitterDetails(String columnName, String filterValue) {
		String deviceID = getSpecificCellValue(
				"select * from patients.patient_device pd where "+columnName+" = '" + filterValue + "'",
				"patient_device_id");
		String dbQuery = "select * from patients.device_transmitter dt where patient_device_id = '<deviceID>'";
		dbQuery = dbQuery.replace("<deviceID>", deviceID);

		return executeDBQuery(dbQuery);
	}

	/**
	 * Get the device details for the patient enrolled based on the device serial
	 * number
	 */
	public HashMap<String, String> getDeviceDetails(String deviceSerialNumber) {
		String deviceProductID = getSpecificCellValue(
				"select * from patients.patient_device pd where device_serial_num = '" + deviceSerialNumber + "'",
				"device_product_id");
		String dbQuery = "select * from devices.device_product dp where device_product_id = '<deviceProductID>'";
		dbQuery = dbQuery.replace("<deviceProductID>", deviceProductID);

		return executeDBQuery(dbQuery);
	}

	/**
	 * Read patient details from database based on patient name provided
	 */
	//@Deprecated
	public HashMap<String, String> getPatient(String firstName, String lastName) {
		String dbQuery = "select * from patients.patient p where first_name = '<firstname>' and last_name = '<lastname>'";
		dbQuery = dbQuery.replace("<firstname>", firstName);
		dbQuery = dbQuery.replace("<lastname>", lastName);

		return executeDBQuery(dbQuery);
	}

	/**
	 * Method to get Patient details.
	 * 
	 * @implNote If you want to find a patient with two values (eg. First name and
	 *           last name) then fill details for both columnNames else pass
	 *           columnName2 and value2 as null
	 */
	public HashMap<String, String> getPatient(String columnName1, String columnName2, String value1, String value2) {
		String dbQuery;
		if (columnName2 == null && value2 == null) {
			dbQuery = "select * from patients.patient p where " + columnName1 + " = '" + value1 + "';";
		} else {
			dbQuery = "select * from patients.patient p where " + columnName1 + " = '" + value1 + "' and " + columnName2
					+ " = '" + value2 + "'";
		}

		return executeDBQuery(dbQuery);
	}

	/**Retrieve patient details by device serial.*/
	public static Map<String, String> getPatient(MITETest currentTest, String deviceSerial) {
		String dbQuery = "select * from patients.patient p"
				+ " join patients.patient_device pd on p.patient_id = pd.patient_id"
				+ " where pd.device_serial_num = '" + deviceSerial + "'";

		List<Map<String, String>> patient = getDBContents(currentTest.getReport(), currentTest.getDatabase(), dbQuery);

		return (patient.size() != 0) ? patient.get(0) : Map.of();
	}

	/**
	 * Method to get Patient address details
	 */
	public HashMap<String, String> getPatientAddressDetails(String columnName, String value) {
		String dbQuery = "select * from patients.patient_address pa where " + columnName + " = '" + value + "';";

		return executeDBQuery(dbQuery);
	}

	/**
	 * Method to get patient shipment details
	 */
	public HashMap<String, String> getPatientShipmentDetails(String columnName, String value) {
		String dbQuery = "select * from patients.transmitter_shipment ts where " + columnName + " = '" + value + "';";

		return executeDBQuery(dbQuery);
	}

	/**
	 * Method to get patient even details
	 */
	public List<Map<String, String>> getPatientEventsDetails(String columnName, String value) {
		String dbQuery = "select * from patients.\"event\" e";
		return getDBContents(report, database, dbQuery, columnName, value);
	}

	/**
	 * Device details associated with a patient
	 */
	public HashMap<String, String> getPatientDeviceDetails(String columnName, String filterValue) {
		String dbQuery = "select * from patients.patient_device pd where " + columnName + " = '" + filterValue + "';";

		return executeDBQuery(dbQuery);
	}

	/**
	 * Function to retrieve device product information
	 */
	public List<Map<String, String>> getDevices() {
		String dbQuery = "select * from devices.device_product dp";

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**
	 * Get Profiling information related to the transmitter
	 */
	@Deprecated
	public HashMap<String, String> getTransmitterProfiles(String transmitterSerialNum) {
		String dbQuery = "select * from extinstruments.transmitter_profile tp where transmitter_serial_num = '<transmitterSerial>';";
		dbQuery = dbQuery.replace("<transmitterSerial>", transmitterSerialNum);

		return executeDBQuery(dbQuery);
	}

	/**
	 * Get Profiling information related to the transmitter
	 */
	public List<Map<String, String>> getTransmitterProfiles(String columnName, String filterValue) {
		String dbQuery = "select * from extinstruments.transmitter_profile tp where "+columnName+" = '"+filterValue+"';";

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());

	}

	/**
	 * Get Transmitter Product Information
	 */
	public HashMap<String, String> getTransmitterProdcutDetails(String columnName, String value) {
		String dbQuery = "select * from extinstruments.transmitter_product tp where " + columnName + " = " + value
				+ ";";

		return executeDBQuery(dbQuery);
	}

	/**
	 * Get Patient information from Customer Application Table
	 */
	@Deprecated
	public HashMap<String, String> getPatientFromCustomerApplication(String columnName, String value) {
		String dbQuery = "select *  from patients.customer_application_patient cap where " + columnName + " = " + value
				+ ";";

		return executeDBQuery(dbQuery);
	}

	/**
	 * Get Smart schedule details set by patient
	 */
	public List<Map<String, String>> getFollowUpSchedules(String patientID) {
		String dbQuery = "select * from patients.followup_date where Customer_Appl_Patient_id = (select cap.Customer_Appl_Patient_id from patients.customer_application_patient cap where patient_id = <patientID>) order by create_dtm desc";
		dbQuery = dbQuery.replace("<patientID>", patientID);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**
	 * Get Discrete schedule set by patient.
	 */
	public List<Map<String, String>> getDiscreteSchedule(String columnName, String value) {
		String dbQuery = "select * from patients.discrete_date_schedule";
		return getDBContents(report, database,dbQuery, columnName, value);
	}

	/**
	 * Get calculated schedule set by patient.
	 */
	public List<Map<String, String>> getCalculatedSchedule(String columnName, String value) {
		String dbQuery = "select * from patients.calculated_schedule cs";
		return getDBContents(report, database,dbQuery, columnName, value);
	}

	/**
	 * Return list of Lead Chamber models for verification purpose
	 */
	public List<String> getLeadChamberModels() {
		String dbQuery = "select * from lookup.code c where code_qualifier = 'Lead_Chamber_Cd';";

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}
		List<String> requiredRows = queryResult.getColumn("code_desc");
		return requiredRows;
	}

	/**
	 * This method is used to backdate the next follow up date. It is applicable for
	 * both manual and smart schedule
	 */
	@Deprecated
	public void backdateFollowup(String scheduleType, String patientID, String dateToBackdate, int daysToBackdate) {
		CommonDBUtilities commonDBUtils = new CommonDBUtilities(report, database);

		List<Map<String, String>> patientFollowUp = getFollowUpSchedules(patientID);

		String followUpID = patientFollowUp.get(0).get("followup_date_id");

		database.executeUpdate(
				"update patients.followup_date set scheduled_time = '" + DateUtility.getPastDates(daysToBackdate)
				+ " 00:00:00.000' where followup_date_id = '" + followUpID + "';");

		if (scheduleType.equals("MANUAL")) {
			List<Map<String, String>> patientDiscreteDates = getDiscreteSchedule("patient_id", patientID);

			for (int i = 0; i < patientDiscreteDates.size(); i++) {
				if (DateUtility.changeDateFormat(patientDiscreteDates.get(i).get("discrete_date_time").split(" ")[0])
						.equals(dateToBackdate)) {
					String discreteDateID = patientDiscreteDates.get(i).get("discrete_date_schedule_id");
					database.executeUpdate("update patients.discrete_date_schedule set discrete_date_time = '"
							+ DateUtility.getPastDates(daysToBackdate)
							+ " 00:00:00.000' where discrete_date_schedule_id  = '" + discreteDateID + "';");
				}
			}
		}
		if (scheduleType.equals("SMART")) {
			database.executeUpdate("update patients.calculated_schedule set main_start_date = '"
					+ DateUtility.getPastDates(daysToBackdate) + " 00:00:00.000' where patient_id  = '" + patientID
					+ "';");
		}

		report.logStep(TestStep.builder().message("Running ASA Timer for the status to change for 200 (NEXT) -> 204 (OVERDUE)").build());
		commonDBUtils.asaTimer();
	}

	/**
	 * This method is used to backdate the next follow up date. It is applicable for
	 * both manual and smart schedule and temporary schedule also.
	 */
	public void backdateFollowup(ScheduleType scheduleType, String patientID, String dateToBackdate, int daysToBackdate, boolean updateTempDate, int tempDateBackdate, boolean runASATimer) {
		CommonDBUtilities commonDBUtils = new CommonDBUtilities(report, database);

		List<Map<String, String>> patientFollowUp = getFollowUpSchedules(patientID);

		String followUpID = patientFollowUp.get(0).get("followup_date_id");
		database.executeUpdate(
				"update patients.followup_date set scheduled_time = '" + DateUtility.getPastDates(daysToBackdate)
				+ " 00:00:00.000' where followup_date_id = '" + followUpID + "';");

		if (scheduleType==ScheduleType.MANUAL) {
			List<Map<String, String>> patientDiscreteDates = getDiscreteSchedule("patient_id", patientID);

			for (int i = 0; i < patientDiscreteDates.size(); i++) {
				if (DateUtility.changeDateFormat(patientDiscreteDates.get(i).get("discrete_date_time").split(" ")[0], null, null)
						.equals(dateToBackdate)) {
					String discreteDateID = patientDiscreteDates.get(i).get("discrete_date_schedule_id");
					database.executeUpdate("update patients.discrete_date_schedule set discrete_date_time = '"
							+ DateUtility.getPastDates(daysToBackdate)
							+ " 00:00:00.000' where discrete_date_schedule_id  = '" + discreteDateID + "';");
				}
			}
		}
		if (scheduleType==ScheduleType.SMART) {
			database.executeUpdate("update patients.calculated_schedule set main_start_date = '"
					+ DateUtility.getPastDates(daysToBackdate) + " 00:00:00.000' where patient_id  = '" + patientID
					+ "';");
		}

		if (updateTempDate) {
			database.executeUpdate("update patients.calculated_schedule set temp_start_date = '"
					+ DateUtility.getPastDates(tempDateBackdate) + " 00:00:00.000' where patient_id  = '" + patientID
					+ "';");
		}

		if (runASATimer) {
			report.logStep(TestStep.builder().message("Running ASA Timer for the status to change for 200 (NEXT) -> 204 (OVERDUE)").build());
			commonDBUtils.asaTimer();
		}
	}

	/**
	 * This method can be used to update either smart or manual schedule (Calculated Schedule or discrete schedule table)
	 * @implNote This method does not runs asa timer after update, if required call it separately
	 */

	public void updateSmartOrManualSchedule(ScheduleType scheduleType, String patientID, String dateToBackdate, int daysToBackdate) {

		switch (scheduleType) {
		case MANUAL:
			List<Map<String, String>> patientDiscreteDates = getDiscreteSchedule("patient_id", patientID);

			for (int i = 0; i < patientDiscreteDates.size(); i++) {
				if (DateUtility.changeDateFormat(patientDiscreteDates.get(i).get("discrete_date_time").split(" ")[0],
						null, null).equals(dateToBackdate)) {
					String discreteDateID = patientDiscreteDates.get(i).get("discrete_date_schedule_id");
					database.executeUpdate("update patients.discrete_date_schedule set discrete_date_time = '"
							+ DateUtility.getPastDates(daysToBackdate)
							+ " 00:00:00.000' where discrete_date_schedule_id  = '" + discreteDateID + "';");
				}
			}
			break;
		case SMART:
			database.executeUpdate("update patients.calculated_schedule set main_start_date = '"
					+ DateUtility.getPastDates(daysToBackdate) + " 00:00:00.000' where patient_id  = '" + patientID
					+ "';");
			break;
		default:
			report.logStep(TestStep.builder().reportLevel(TestStep.ReportLevel.WARNING).message("Selected schedule type does not require scheduling").build());
		}
	}

	/**
	 * This method will be used to only update followup date in Followup Dates Table
	 * @implNote This method does not runs asa timer after update, if required call it separately
	 */
	public void updatedFollowupDate(String patientID, int daysToBackdate, String time) {
		if(time == null) {
			time = "00:00:00.000";
		}
		List<Map<String, String>> patientFollowUp = getFollowUpSchedules(patientID);

		String followUpID = "";
		followUpID = patientFollowUp.get(0).get("followup_date_id");
		database.executeUpdate(
				"update patients.followup_date set scheduled_time = '" + DateUtility.getPastDates(daysToBackdate)
				+ " "+time+"' where followup_date_id = '" + followUpID + "';");

	}

	/**
	 * Get device product details
	 */	
	public List < Map < String, String >> getDeviceProduct(String columnName, String value) {
		String dbQuery = "select * from devices.device_product";
		return getDBContents(report, database,dbQuery, columnName, value);
	}

	/**
	 * Get patients patient details
	 */
	public List < Map < String, String >> getpatientsPatient(String columnName, String value) {
		String dbQuery = "select * from patients.patient p";
		return getDBContents(report, database,dbQuery, columnName, value);
	}

	/**
	 * Get patients patient details
	 */
	public List < Map < String, String >> getPatientsFromCustomerApplication(String columnName, String value){
		String dbQuery = "select * from patients.customer_application_patient cap";
		return getDBContents(report, database,dbQuery, columnName, value);
	}

	/**
	 * Get details from RT cache table
	 */
	public List<Map<String, String>> getRTCacheDetails(RTCacheTableColumns columnName, String value){
		String dbQuery = "select * from transmissions.rt_cache rc";
		return getDBContents(report, database,dbQuery, columnName.getRTCacheTableColumns(), value);
	}

	/**
	 *  query databases bases on tablename and column name
	 */
	public List<Map<String, String>> getTransmissionData(String serialNumber, String filterValue, String tableName, String columnName) {
		String dbQuery = "select * from transmissions."+tableName+" d where "+columnName+" = '"+serialNumber+"' order by transmission_id";
		return getDBContents(report, database,dbQuery, serialNumber, filterValue);
	}

	/**
	 * Set battery advisory status for patient
	 */
	public void setBatteryAdvisoryForPatient(String patientID, String deviceModel, String deviceSerial, BatteryAdvisoryCode code) {
		String dbQuery = "select advisory_patient_info_id from patients.advisory_patient_info api order by advisory_patient_info_id desc;";
		String recentID = getDBContents(report, database, dbQuery, null, null).get(0).get("advisory_patient_info_id");
		int recentIDInLong = Integer.parseInt(recentID);
		int increaseID = 1;
		int id = recentIDInLong + increaseID;
		database.executeUpdate("insert into patients.advisory_patient_info values ("+id+",'"+deviceModel+"','"+deviceSerial+"');");

		database.executeUpdate("update patients.patient_device set Advisory_Indication_CD="+code.getAdvisoryCode()+" where patient_id  = '"+patientID+"';");
	}

	// Helper Functions

	private HashMap<String, String> executeDBQuery(String dbQuery) {
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}
		HashMap<String, String> details = new HashMap<>();
		List<String> values = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			details.put(columnNames.get(index), values.get(index));
		}
		return details;
	}

	/**
	 * This method get alert severity.
	 * @param transmissionId
	 * @return
	 */
	public List<Map<String, String>> getAlertServerity(String transmissionId) {
		String dbQuery = "select * from alerts.website_notification wn where transmission_id = '<transmissionId>'";
		dbQuery = dbQuery.replace("<transmissionId>", transmissionId);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**
	 * This method get lookup code.
	 * @param codeId
	 * @return
	 */
	public List<Map<String, String>> getLookUpCode(String codeId) {
		String dbQuery = "select * from lookup.code c where code_id = '<codeId>'";
		dbQuery = dbQuery.replace("<codeId>", codeId);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**
	 * This method get transmission report.
	 */
	public List<Map<String, String>> getTransmissionReport(String transmission_id) {
		String dbQuery = "select * from transmissions.report r where transmission_id = '<transmission_id>'";
		dbQuery = dbQuery.replace("<transmission_id>", transmission_id);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**
	 * This method get external notification.
	 * @param transmission_id
	 * @return
	 */
	public String getExternalNotificationId(String transmission_id) {
		String query = "select * from alerts.external_notification en where transmission_id  ='<transmission_id>'";
		query = query.replace("<transmission_id>", transmission_id);
		report.logStep(TestStep.builder().message("Executing database query: " + query).build());

		return database.executeQuery(query).getFirstCellValue();
	}

	/**
	 * This method get alert from database.
	 * @param external_notification_id
	 * @return
	 */
	public List<Map<String, String>> getalert(String external_notification_id) {
		String dbQuery = "select * from alerts.alert_notification an where external_notification_id  = '<external_notification_id>'";
		dbQuery = dbQuery.replace("<external_notification_id>", external_notification_id);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}


	/**
	 * Verify Ongoing flag in episode and egm.
	 * @param transmission_id
	 * @return
	 */
	public String getOngoingflg(String transmission_id) {
		String query = "select ongoing_flg from transmissions.episode_and_egm eae where transmission_id  ='<transmission_id>'";
		query = query.replace("<transmission_id>", transmission_id);
		report.logStep(TestStep.builder().message("Executing database query: " + query).build());

		return database.executeQuery(query).getFirstCellValue();
	}

	/**Fetch data from transmissions.archive_session_file table**/
	public List<Map<String, String>> getTransmissionArchive(String transmission_id) {
		String dbQuery = "select * from transmissions.archive_session_file asf where transmission_id = '<transmission_id>'";
		dbQuery = dbQuery.replace("<transmission_id>", transmission_id);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**Fetch data from transmissions.alert table**/
	public List<Map<String, String>> getTransmissionAlerts(String transmission_id) {
		String dbQuery = "select * from transmissions.alert where transmission_id = '<transmission_id>'";
		dbQuery = dbQuery.replace("<transmission_id>", transmission_id);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**Fetch data from transmissions.episode_and_egm table**/
	public List<Map<String, String>> getEpisodesAndEGMsData(String transmission_id) {
		String dbQuery = "select * from transmissions.episode_and_egm eae where transmission_id = '<transmission_id>'";
		dbQuery = dbQuery.replace("<transmission_id>", transmission_id);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**Fetch data from transmissions.transfer_log table**/
	public List<Map<String, String>> getTransferLogData(String transmission_id) {
		String dbQuery = "select * from transmissions.transfer_log tl where transmission_id = '<transmission_id>'";
		dbQuery = dbQuery.replace("<transmission_id>", transmission_id);

		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);

		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}

		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}

	/**
	 * Method gets Access details from dB table access_log for Patient with patient id given as parameter
	 * @param Patient_id
	 * @return
	 */
	public static List<Map<String, String>> getAccessDetails(MITETest currentTest, String columnName, String filterValue) {
		String query = "select * from patients.access_log al where "  + columnName + " = '" + filterValue + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}

		
	/**
	 * This method zm data from db.
	 * @param transmission_id
	 * @return
	 */
	public List<Map<String, String>> getzmDataPoint(String transmission_id) {
		String dbQuery = "select * from transmissions.zm_average_data_point zadp where transmission_id = '<transmission_id>'";
		dbQuery = dbQuery.replace("<transmission_id>", transmission_id);
 
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);
 
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}
 
		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}
	
	/**
	 * This Method get session file.
	 * @param device_serial_num
	 * @return
	 */
	public List<Map<String, String>> getArchiveSessionFiles(String device_serial_num) {
		String dbQuery = "select  *  from transmissions.transaction_processing tp  where device_serial_num = '<device_serial_num>'";
		dbQuery = dbQuery.replace("<device_serial_num>", device_serial_num);
 
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);
 
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}
 
		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}
	
	/**
	 * This Method get processing details.
	 * @param transaction_identity
	 * @return
	 */
	public List<Map<String, String>> getProcessingDetails(String transaction_identity) {
		String dbQuery = "select * from transmissions.transaction_processing_detail tpd where transaction_identity = '<transaction_identity>'";
		dbQuery = dbQuery.replace("<transaction_identity>", transaction_identity);
 
		report.logStep(TestStep.builder().message("Executing database query: " + dbQuery).build());
		QueryResult queryResult = database.executeQuery(dbQuery);
 
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(TestStep.builder().message("No result when executed the following query: " + dbQuery).build());
			return null;
		}
 
		return stringParser.convertResultSetToMap(queryResult.getColumnNames(), queryResult.getAllRows());
	}
}