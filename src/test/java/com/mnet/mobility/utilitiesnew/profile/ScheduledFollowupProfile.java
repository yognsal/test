package com.mnet.mobility.utilities.profile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.database.utilities.DBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.DevMedAttribute;
import com.mnet.mobility.utilities.profile.enums.ScheduledFollowupAttribute;

/**
 * Object formation and business logic validation for NGQ/ICM Follow-up profile ()
 * 
 * @version 1.0
 * @author NAIKKX12
 */
public class ScheduledFollowupProfile extends AppProfile {

	private static final int FOLLOWUPPROFILECODE = 200;
	private static final int SCHEDULE_PERIOD = 540;
	private static final String dbDateFormat = FrameworkProperties.getProperty("PAYLOAD_ZIP_DATE_FORMAT");
	private CommonDBUtilities commonDBUtils;

	public ScheduledFollowupProfile(PatientApp patientApp) {
		super(patientApp, FOLLOWUPPROFILECODE);
		commonDBUtils = new CommonDBUtilities(report, database);
	}

	@Override
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(ScheduledFollowupAttribute.values());
	}

	@Override
	public boolean validateAttribute(ProfileAttribute attribute) {
		Object expectedValue;

		switch ((ScheduledFollowupAttribute) attribute) {
		case bleSessionTimeout:
		case retryOnFailure:
		case payloadsToRescheduleWorkflow:
		case scriptToExecute_scriptsAvailable:
		case uploadPayloadLabel:
		case eventTime_eventType:
			expectedValue = getAttributeDefaultValue(attribute);
			break;
		case scriptToExecute_sequenceName:
			expectedValue = getScriptSequenceName();
			break;
		case eventTime_eventSchedule_scheduled:
			List<String> profileSchedule = profileSchedule();
			List<String> dbSchedule = getSchedule();
			report.logStep(TestStep.builder().message("Dates scheduled in profile: " + profileSchedule).build());
			report.logStep(TestStep.builder().message("Date schedule on UI/ database: " + dbSchedule).build());
			report.logStep(TestStep.builder().message("Followup date: " + getNextFollowDate()).build());

			boolean success = true;
			if (profileSchedule.size() == 0 && dbSchedule.size() == 0) {
				return true;
			}
			for (String date : profileSchedule) {
				if (!dbSchedule.contains(date)) {
					success = false;
				}
			}
			return success;
		case workflowState:
			expectedValue = getWorkflowState();
			break;
		case contentVersion:
			expectedValue = getContentVersion();
			break;
		case workflowTransactionID:
			return ((Integer) getAttributeValue(
					ScheduledFollowupAttribute.workflowTransactionID) == FOLLOWUPPROFILECODE);
		default:
			report.logStep(
					TestStep.builder().failMessage((DevMedAttribute) attribute + " attribute not handled").build());
			return false;
		}

		if (expectedValue == null) {
			report.logStep(TestStep.builder().reportLevel(ReportLevel.WARNING).message(
					"Expected value of '" + (ScheduledFollowupAttribute) attribute + "' attribute yet to be calculated")
					.build());
			return true;
		}

		return compareAttributesAndReport(attribute, expectedValue);
	}

	/** Get Next follow-up date from database */
	private String getNextFollowDate() {
		String query = "select fd.scheduled_time from patients.followup_date fd join patients.customer_application_patient cap on "
				+ "cap.customer_appl_patient_id = fd.customer_appl_patient_id join patients.patient p on p.patient_id = cap.patient_id "
				+ "join patients.patient_device pd on pd.patient_id = p.patient_id where pd.device_serial_num = '"
				+ patientApp.getDeviceSerial() + "' " + "and followup_status_cd = '" + FOLLOWUPPROFILECODE
				+ "' order by fd.create_dtm desc";
		report.logStep(TestStep.builder().message("Executing database query: " + query).build());
		QueryResult result = database.executeQuery(query);
		if (result.getAllRows().size() > 0) {
			String value = result.getFirstRow().get(0);
			return value.substring(0, value.indexOf(" "));
		}
		return null;
	}

	/** Get schedule set in profile in human readable date format */
	private List<String> profileSchedule() {
		Object schedule = getAttributeValue(ScheduledFollowupAttribute.eventTime_eventSchedule_scheduled);
		ArrayList<Long> epochDates = schedule == null ? new ArrayList<>() : (ArrayList<Long>) schedule;
		ArrayList<String> scheduledDates = new ArrayList<>();
		for (long epoch : epochDates) {
			scheduledDates.add(DateUtility.changeEpochToDate(epoch, true));
		}
		return scheduledDates;
	}

	/** Get schedule (manual/ automatic follow-up) for validation */
	private List<String> getSchedule() {
		List<String> schedule = new ArrayList<>();
		String nextFollowupDate = getNextFollowDate();
		if (nextFollowupDate == null) {
			return schedule;
		}

		FrameworkLog log = patientApp.getCurrentTest().getLog();

		// Check discrete_schedule table
		schedule = getManualSchedule(nextFollowupDate, log);

		if ((schedule.size()) == 0) {
			schedule = getAutoSchedule(nextFollowupDate, log);
		}

		return schedule;
	}

	/** Business logic to get workflowstate attribute value */
	private boolean getWorkflowState() {
		boolean deviceInEOS = DBUtilities.interpretDatabaseFlag(PatientDBUtilities
				.getPatient(patientApp.getCurrentTest(), patientApp.getDeviceSerial()).get("device_at_eos_flg"));
		return (profileSchedule().size() == 0 || deviceInEOS) ? false : true;
	}

	/** Business logic to get script execute -> Sequnce name */
	private String getScriptSequenceName() {
		boolean clearDiagnostics = DBUtilities
				.interpretDatabaseFlag(commonDBUtils.getDBOverride("clear_diag_flg", patientApp.getDeviceSerial()));
		boolean clearEpisodes = DBUtilities.interpretDatabaseFlag(
				commonDBUtils.getDBOverride("clear_epis_incl_egm_flg", patientApp.getDeviceSerial()));

		if (clearDiagnostics && clearEpisodes) {
			return "seqFollowupClrAll";
		}

		if (clearDiagnostics && clearEpisodes == false) {
			return "seqFollowupClrDiag";
		}

		if (clearDiagnostics == false && clearEpisodes) {
			return "seqFollowupClrEpisEgms";
		}
		
		if (clearDiagnostics == false && clearEpisodes == false) {
			return "seqFollowupNoClearing";
		}

		return null;
	}

	/* Get manual schedule */
	private List<String> getManualSchedule(String nextFollowupDate, FrameworkLog log) {
		List<String> schedule = new ArrayList<>();

		String query = "select discrete_date_time from patients.discrete_date_schedule dds2 join patients.patient_device pd on "
				+ "pd.patient_id = dds2.patient_id where pd.device_serial_num = '" + patientApp.getDeviceSerial()
				+ "' order by discrete_date_time";
		report.logStep(TestStep.builder().message("Executing database query: " + query).build());
		QueryResult result = database.executeQuery(query);
		List<List<String>> dbContent = result.getAllRows();
		String date;
		if (dbContent.size() > 0) {
			for (List<String> ls : dbContent) {
				date = ls.get(0).substring(0, ls.get(0).indexOf(" "));

				if (DateUtility.compareDates(date, nextFollowupDate, dbDateFormat, log)) {
					schedule.add(DateUtility.convertDateToWords(date, dbDateFormat));
				}
				if (schedule.size() == 2) {
					break;
				}
			}
		}

		return schedule;
	}

	/** Get schedule (manual/ automatic follow-up) for validation */
	private List<String> getAutoSchedule(String nextFollowupDate, FrameworkLog log) {
		List<String> schedule = new ArrayList<>();

		String query = "select main_start_date, main_period_cd, main_series_length_cd, temp_start_date, temp_period_cd, temp_duration_cd from "
				+ "patients.calculated_schedule cs join patients.patient_device pd on pd.patient_id = cs.patient_id "
				+ "where pd.device_serial_num = '" + patientApp.getDeviceSerial() + "'";

		report.logStep(TestStep.builder().message("Executing database query: " + query).build());
		QueryResult result = database.executeQuery(query);
		List<List<String>> dbContent = result.getAllRows();
		if (dbContent.size() > 0) {
			List<String> entireSchedule = new ArrayList<>();
			List<String> permanentSchedule = new ArrayList<>();
			LocalDate localDate = null;
			int transmitFrequency = 0, totalDuration = 0, nextIndex = 0, rotation;

			query = "select code from lookup.code c where code_id = '" + dbContent.get(0).get(1)
					+ "' and code_qualifier = 'Main_Period_Cd'";
			switch (database.executeQuery(query).getFirstRow().get(0)) {
			case "1W":
				transmitFrequency = 7;
				break;
			case "13W":
				transmitFrequency = 91;
				break;
			default:
				report.logStep(TestStep.builder().reportLevel(ReportLevel.FAIL)
						.message("Mentioned transmit frequency not handled").build());
			}
			query = "select code from lookup.code c where code_id = '" + dbContent.get(0).get(2)
					+ "' and code_qualifier = 'Main_Series_Length_Cd'";
			switch (database.executeQuery(query).getFirstRow().get(0)) {
			case "1M":
				totalDuration = 30;
				break;
			case "39W":
				totalDuration = 273;
				break;
			default:
				report.logStep(TestStep.builder().reportLevel(ReportLevel.FAIL)
						.message("Mentioned total duration not handled").build());
			}
			
			// Create permanent schedule
			if(transmitFrequency < totalDuration) {
				rotation = totalDuration / transmitFrequency;	
			}else {
				float rawRotationValue = (float) SCHEDULE_PERIOD / (transmitFrequency*2);
				rotation = Math.round(rawRotationValue);
			}
			
			String date = dbContent.get(0).get(0).substring(0, dbContent.get(0).get(0).indexOf(" "));
			permanentSchedule.add(date);
			localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(dbDateFormat));
			for (int index = 0; index < rotation; index++) {
				localDate = localDate.plusDays((transmitFrequency < totalDuration) ? transmitFrequency : (transmitFrequency*2));
				date = localDate.format(DateTimeFormatter.ofPattern(dbDateFormat));
				permanentSchedule.add(date);
			}
			

			if (dbContent.get(0).get(3) != null) {
				// Create entire schedule based on temporary schedule set
				date = dbContent.get(0).get(3).substring(0, dbContent.get(0).get(3).indexOf(" "));
				for (String str : permanentSchedule) {
					if (DateUtility.compareDates(date, str, dbDateFormat, log)) {
						entireSchedule.add(str);
					}
				}
				entireSchedule.add(date);
				query = "select code from lookup.code c where code_id = '" + dbContent.get(0).get(4)
						+ "' and code_qualifier = 'Temp_Period_Cd'";
				switch (database.executeQuery(query).getFirstRow().get(0)) {
				case "1W":
					transmitFrequency = 7;
					break;
				case "13W":
					transmitFrequency = 91;
					break;
				default:
					report.logStep(TestStep.builder().reportLevel(ReportLevel.FAIL)
							.message("Mentioned transmit frequency not handled").build());
				}
				query = "select code from lookup.code c where code_id = '" + dbContent.get(0).get(5)
						+ "' and code_qualifier = 'Temp_Duration_Cd'";
				switch (database.executeQuery(query).getFirstRow().get(0)) {
				case "1M":
					totalDuration = 30;
					break;
				default:
					report.logStep(TestStep.builder().reportLevel(ReportLevel.FAIL)
							.message("Mentioned total duration not handled").build());
				}

				rotation = totalDuration / transmitFrequency;
				localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern(dbDateFormat));
				for (int index = 0; index < rotation; index++) {
					localDate = localDate.plusDays(transmitFrequency);
					date = localDate.format(DateTimeFormatter.ofPattern(dbDateFormat));
					entireSchedule.add(date);
				}
				date = entireSchedule.get(entireSchedule.size() - 1);
				for (String str : permanentSchedule) {
					if (DateUtility.compareDates(str, date, dbDateFormat, log)) {
						entireSchedule.add(str);
					}
				}
			} else {
				entireSchedule.addAll(permanentSchedule);
			}

			for (int index = 0; index < entireSchedule.size(); index++) {
				if (DateUtility.compareDates(entireSchedule.get(index), nextFollowupDate, dbDateFormat, log)) {
					date = DateUtility.convertDateToWords(entireSchedule.get(index), dbDateFormat);
					schedule.add(date);
					nextIndex = index + 1;
					break;
				}
			}

			if (nextIndex != 0 && nextIndex < entireSchedule.size()) {
				schedule.add(DateUtility.convertDateToWords(entireSchedule.get(nextIndex), dbDateFormat));
			}
		}

		return schedule;
	}
}