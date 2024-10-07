package com.mnet.database.utilities;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;
import com.mnet.pojo.mobility.EncryptedPayload.TelemetryType;
import com.mnet.pojo.mobility.ale.SessionRecordPayload;

/**
 * Utilities for all NGQ Patient App (reg&bond, session upload) activities
 * 
 * @author NAIKKX12
 *
 */
public class PatientAppDBUtilities extends DBUtilities {
	
	public enum NGQDeviceFamily {
		CDHFA("CDHFA"), CDDRA("CDDRA"), CDVRA("CDVRA");

		private String deviceFamily;

		private NGQDeviceFamily(String deviceFamily) {
			this.deviceFamily = deviceFamily;
		}

		public String getDeviceFamily() {
			return this.deviceFamily;
		}

		private static final List<NGQDeviceFamily> enumValues = Collections.unmodifiableList(Arrays.asList(values()));
		private static final int enumSize = enumValues.size();
		private static final Random random = new Random();

		public static NGQDeviceFamily randomDeviceFamily() {
			return enumValues.get(random.nextInt(enumSize));
		}
	}
	
	/**
	 * Enum for columns for Profile Patient Container Table
	 */
	public enum ProfilePatientContainerColumn{
		MERLIN_NET_ID("merlin_net_id"), CONTENT_CLOB("content_clob"), PATINET_PROFILE_CONTAINER_ID("profile_patient_container_id"), PROFILE_CD("profile_cd");
		
		private String columnName;
		
		private ProfilePatientContainerColumn(String columnName) {
			this.columnName = columnName;
		}
		
		public String getProfilePatientContainerColumnName() {
			return this.columnName;
		}
	}
	
	/**
	 * Enum for columns for Patient Metadata Table
	 */
	public enum PatientAppMetadataColumn{
		REGISTRATION_DATE("registration_date"), FIRST_APP_REGISTRATION_DATE("first_app_registration_date"),
		LAST_TRANSMITTER_CONNECTED_DTM("last_transmitter_connected_dtm"), APP_SW_VERSION("app_sw_version"),
		APP_MODEL_NUMBER("app_model_number"), ABBR_DEVICE_MODEL_NUM("abbreviated_device_model_num"),
		DIM_VERSION("dim_version"), FIRMWARE_VERSION("firmware_version"), DATA_CORRECTION_FLAG("devchgd_datacorrected_flg");
		
		private String columnName;
		
		private PatientAppMetadataColumn(String columnName) {
			this.columnName = columnName;
		}
		
		public String getPatientAppMetadataColumnName() {
			return this.columnName;
		}
	}
	
	/**
	 * Enum for columns for Profile IOT Message Table
	 */
	public enum IOTMessageColumn{
		IS_BONDING_PROFILE("is_bonding_profile"), SENT_FLAG("sent_flg"), AZURE_ID("azureid"), RECEIVED_FLG("received_flg"), CLOUD_VERIFICATION_ID("cloud_verificationid");
		
		private String columnName;
		
		private IOTMessageColumn(String columnName) {
			this.columnName = columnName;
		}
		
		public String getIOTMessageColumnName() {
			return this.columnName;
		}
	}

	/**
	 * Get user device info from pportal
	 */
	public static List<Map<String, String>> getPPortalUserDevice(MITETest currentTest, String filterColumn, String filterValue) {
		String query = "select * from pportal.user_device";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query, filterColumn, filterValue);
	}
	
	/**
	 * Get patient metadata information
	 */
	public static List<Map<String, String>> getPatientAppMetaData(MITETest currentTest, String deviceSerial) {
		String query = "select pm.* from patients.patientapp_metadata pm"
				+ " join patients.patient_device pd on pd.patient_id = pm.patient_id "
				+ " where pd.device_serial_num = '" + deviceSerial + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/**Get transmission data from transmissions.transmission_device table**/
	public static List<Map<String, String>> getTransmission(MITETest currentTest, String deviceSerial, TelemetryType telemetryType){
		String query = null;
		
		if(FrameworkProperties.getApplicationVersion().equals("d4")) {
			query = "select * from transmissions.transmission t join transmissions.transmission_device td on "
					+ "t.transmission_id = td.transmission_id join patients.patient_device pd on t.patient_id = pd.patient_id "
					+ "where pd.device_serial_num = '"+deviceSerial+"' and t.telemetry_type_cd = '"+telemetryType.getCode()+"';";
		}else {
			query = "select * from transmissions.transmission t"
					+ " join patients.patient_device pd on pd.patient_id = t.patient_id "
					+ " where pd.device_serial_num = '" + deviceSerial + "'"
					+ " and t.telemetry_type_cd = " + telemetryType.getCode();
		}
		
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);	
	}
		
	/**
	 * Get Data from app life events table based on azure id
	 */
	public static List<Map<String, String>> getAppLifeEventData(MITETest currentTest, String azureid){
		String query = "select * from applifeevents.app_life_events ale join lookup.code c on ale.app_life_events_type_cd = c.code_id where azureid = '"
				+ azureid + "' order by app_life_event_timestamp desc;";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}

	
	/**
	 * Retrieves database entry of ALE associated with a given patient_app_ale_id.
	 */
	public static Map<String, String> getAppLifeEventById(PatientApp patientApp, AppLifeEvent appLifeEvent) {
		if (appLifeEvent instanceof SessionRecordPayload) { // no patient_app_ale_id associated
			return null;
		}
		
		String query = "select * from applifeevents.app_life_events ale"
				+ " where azureid = '" + patientApp.getAzureId() + "'"
				+ " and patient_app_ale_id = '" + appLifeEvent.getPatientAppALEId() + "'";
		
		MITETest currentTest = patientApp.getCurrentTest();
		
		List<Map<String, String>> dbContents = getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
		
		return (dbContents.size() != 0) ? dbContents.get(0) : Map.of();
	}
	
	/**
	 * Retrieves number of ALEs of a given type sent by a patient app instance.
	 */
	public static int getAppLifeEventCount(PatientApp patientApp, AppLifeEvent appLifeEvent) {
		String query = "select count(*) from applifeevents.app_life_events ale"
				+ " where azureid = '" + patientApp.getAzureId() + "'"
				+ " and app_life_events_type_cd = " + appLifeEvent.getALETypeCode()
				+ " order by create_dtm desc";
		
		MITETest currentTest = patientApp.getCurrentTest();
		
		List<Map<String, String>> dbContents = getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
		
		return (dbContents.size() != 0) ? Integer.parseInt(dbContents.get(0).get("count")) : 0;
	}
	
	/** Get Patient container table details for patient with deviceSerial & profile code as optional parameter; if zero then ignore profile code */
	public static List<Map<String, String>> getPatientProfileContainer(MITETest currentTest, String deviceSerial, Integer profileCode) {
		
		String query = "select * from profile.profile_patient_container ppc"
				+ " join patients.patient p on ppc.merlin_net_id = p.merlin_net_id"
				+ " join patients.patient_device pd on p.patient_id = pd.patient_id"
				+ " where pd.device_serial_num = '" + deviceSerial + "'";
		
		if (profileCode != null) {
			query = query + " and ppc.profile_cd = '" + profileCode + "'";
		}
		
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/**
	 * Retrieves content version for a specific profile.
	 */
	public static int getContentVersion(MITETest currentTest, String deviceSerial, int profileCode) {
		return Integer.parseInt(getPatientProfileContainer(currentTest, deviceSerial, profileCode).get(0).get("content_version"));
	}
	
	/**
	 * Get Data from pportal -> user record table based on user record ID
	 */
	public static List<Map<String, String>> getUserRecordData(MITETest currentTest, String userRecordID){
		String query = "select * from pportal.user_record ur where user_record_id = '"+userRecordID+"';";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}

	/** Get device details */
	public static List<Map<String, String>> getDeviceData(MITETest currentTest, String deviceSerial) {
		String query = "select * from patients.patient_device"
				+ " where device_serial_num = '" + deviceSerial + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** Get data from iot_message table for provided azureid and message Type code; if null will return for 4200 */
	public static List<Map<String, String>> getIOTMessage(MITETest currentTest, String azureId, String messageTypeCode){
		if (messageTypeCode == null) {
			messageTypeCode = "4200";
		}
		String query = "select * from profile.iot_message im where azureid = '" +azureId+ "' and message_type_cd = '" +messageTypeCode+ "' order by create_dtm desc";;
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** Get default profile container table details for specified profile code (if 0, will return for all codes) */
	public static List<Map<String, String>> getDefaultProfileContainer(MITETest currentTest, int profileCode) {
		
		String query = "select * from profile.profile_default_container";
		if (profileCode != 0) {
			query = query + " where profile_cd = '" + profileCode + "'";
		}
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** Get device bonding logs for azure ID specified */
	public static List<Map<String, String>> getDeviceBondingLog(MITETest currentTest, String azureId) {
		
		String query = "select * from applifeevents.device_bonding_log dbl where azureid ='" + azureId + "' order by create_dtm desc";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** Get patient details for  provided device serial */
	public static Map<String, String> getPatient(MITETest currentTest, String deviceSerial) {
		
		String query = "select * from patients.patient p join patients.patient_device pd on p.patient_id = pd.patient_id where pd.device_serial_num = '" + deviceSerial + "'";
		List<Map<String, String>> dbContents = getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
		if (dbContents.size() == 0) {
			return null;
		}
		return dbContents.get(0);
	}
	
	/** Get patient details for  provided device serial */
	public static Map<String, String> getPatientAlertDetails(MITETest currentTest, String patientID) {
		
		String query = "select * from patients.patient_alert_handling pah where customer_appl_patient_id = '" + patientID + "'";
		List<Map<String, String>> dbContents = getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
		if (dbContents.size() == 0) {
			return null;
		}
		return dbContents.get(0);
	}

	/**
	 * Get Random NGQ device based on device family (CDDRA, CDVRA, CDHFA)
	 * Set device family parameter to null if you want any random ngq device
	 */
	public static Map<String, String> getNGQDevice(MITETest currentTest, NGQDeviceFamily deviceFamily) {
		String query = null;
		String ngqDeviceFamily;
		
		if(deviceFamily == null) {
			ngqDeviceFamily = NGQDeviceFamily.randomDeviceFamily().getDeviceFamily();
		}else {
			ngqDeviceFamily = deviceFamily.getDeviceFamily();
		}
		query = "select * from devices.device_product dp where device_model_num like '%"+ngqDeviceFamily+"%'";
		List<Map<String, String>> devicesList = getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
		
		return devicesList.get(CommonUtils.getRandomNumber(0, devicesList.size() - 1));
	}
	
	/**Get details from ICM User record table based on user record ID**/
	public static Map<String, String> getICMUserRecord(MITETest currentTest, String userRecordID){
		String query = "select * from pportal.icm_user_record iur where user_record_id = '"+userRecordID+"';";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query).get(0);
		
	}
	
	/**Get details from pportal.code table based on code_id**/
	public static Map<String, String> getPportalCodeData(MITETest currentTest, String codeID){
		String query = "select * from pportal.code c where code_id = '"+codeID+"';";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query).get(0);
		
	}
	
	/**Get details from lookup.code table based on code_id**/
	public static Map<String, String> getLookUpCodeData(MITETest currentTest, String codeID){
		String query = "select * from lookup.code c where code_id = '"+codeID+"';";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query).get(0);
		
	}

	/** finction to extract enable_med_flg database on merline id  */
	public static List<Map<String, String>> getCollectDirectTrendDiagnosticsFlag(MITETest currentTest, String merlinId) {
		String query = "select * from patients.patient p where merlin_net_id = '"+merlinId+"'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** Get Transmission alert id */
	public static List<Map<String, String>> getTransmissionAlert(MITETest currentTest, String columnName, String filterValue) {
		String query = "select * from transmissions.alert where " + columnName + " = '" + filterValue + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** get report details */
	public static List<Map<String, String>> getTransmissionReports(MITETest currentTest, String columnName, String filterValue) {
		String query = "select * from transmissions.report r where " + columnName + " = '" + filterValue + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** get report details */
	public static List<Map<String, String>> getAlertExternalNotification(MITETest currentTest, String columnName, String filterValue) {
		String query = "select * from alerts.external_notification en where " + columnName + " = '" + filterValue + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** get report details */
	public static List<Map<String, String>> getAlertNotification(MITETest currentTest, String columnName, String filterValue) {
		String query = "select * from alerts.alert_notification an where " + columnName + " = '" + filterValue + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}
	
	/** get report details */
	public static List<Map<String, String>> getTransmissionEmailMessageDetails(MITETest currentTest, String columnName, String filterValue) {
		String query = "select * from alerts.nds_email_message where "  + columnName + " = '" + filterValue + "'";
		return getDBContents(currentTest.getReport(), currentTest.getDatabase(), query);
	}	
	
}