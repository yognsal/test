package com.mnet.mobility.utilities.profile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.DBUtilities;
import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.DirectAlertProgrammingAttribute;

import io.restassured.path.json.JsonPath;

/**
 * Object formation and business logic validation for Direct Alerts Programming (Auto7691, Auto7804)
 * profile
 * 
 * @version 1.0
 * @author NAIKKX12
 */
public class DirectAlertsProgrammingProfile extends AppProfile {

	private static final int REMOTEPROGRAMCODE = 230;
	private static final String dbDateFormat = "yyyy-MM-dd HH:mm:ss";
	

	public DirectAlertsProgrammingProfile(PatientApp patientApp) {
		super(patientApp, REMOTEPROGRAMCODE);
	}

	@Override
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(DirectAlertProgrammingAttribute.values());
	}

	@Override
	public boolean validateAttribute(ProfileAttribute attribute) {
		Object expectedValue;
		
		switch ((DirectAlertProgrammingAttribute) attribute) {
			case bleSessionTimeout:
			case retryOnFailure:
			case payloadsToRescheduleWorkflow:
			case scriptToExecute_scriptsAvailable:
			case scriptToExecute_sequenceName:
			case uploadPayloadLabel:
			case eventTime_eventType:
				expectedValue = getAttributeDefaultValue(attribute);
				break;
			case workflowState:
				expectedValue = getWorkFlowState();
				break;
			case contentVersion:
				expectedValue = getContentVersion();
				break;
			case workflowTransactionID:
				expectedValue = getProfileCode();
				break;
			default:
				report.logStep(TestStep.builder()
						.failMessage((DirectAlertProgrammingAttribute) attribute + " attribute not handled").build());
				return false;
		}
		
		if (expectedValue == null) {
			report.logStep(TestStep.builder().reportLevel(ReportLevel.WARNING).message("Expected value of '" + (DirectAlertProgrammingAttribute) 
					attribute + "' attribute yet to be calculated").build());
			return true;
		}
		
		return compareAttributesAndReport(attribute, expectedValue);
	}
	
	/** Compute workflow state */
	private boolean getWorkFlowState() {
		List<Map<String, String>> dbContent = PatientAppDBUtilities.getAppLifeEventData
				(patientApp.getCurrentTest(), patientApp.getAzureId());
		JsonPath appLifeEventClob = null;
		if (dbContent != null) {
			for (Map<String, String> map : dbContent) {
				if (map.get("app_life_events_type_cd").equals("4004")){
					appLifeEventClob = new JsonPath(map.get("app_life_events_clob"));
					break;
				}
			}
			
			if (appLifeEventClob != null && appLifeEventClob.getInt("content.workflowId") == 230) {
				if (appLifeEventClob.getInt("content.result") == 19) {
					return false;
				}
				
				if (appLifeEventClob.getInt("content.result") == 2) {
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dbDateFormat);
					LocalDateTime profileLastUpdateDate = LocalDateTime.parse(PatientAppDBUtilities.getPatientProfileContainer(
							patientApp.getCurrentTest(), patientApp.getDeviceSerial(), REMOTEPROGRAMCODE).get(0).get("last_updt_dtm") , formatter);
					
					String query = "select bvvi_state_clear_dtm from pportal.device_metadata dm where "
							+ "device_serial_num = '" + patientApp.getDeviceSerial() + "'";
					report.logStep(TestStep.builder().message("Executing database query: " + query).build());
					
					QueryResult queryResult = database.executeQuery(query);
					if (queryResult.getAllRows().size() == 0) {
						return true;
					}
					
					LocalDateTime bvviStateClearDate = null;
					boolean workflowState = false;
					if (queryResult.getFirstCellValue() != null) {
						bvviStateClearDate = LocalDateTime.parse(queryResult.getFirstCellValue(), formatter);
						long seconds = Duration.between(profileLastUpdateDate, bvviStateClearDate).getSeconds();
						if (seconds >= 0 && seconds <= MobilityUtilities.TOLERANCE_PROFILE_ALE) {
							workflowState = true;
						}	
					}
					return workflowState;
				}
			}
		}
		
		if (DBUtilities.interpretDatabaseFlag(PatientDBUtilities.getPatient(patientApp.getCurrentTest(), 
				patientApp.getDeviceSerial()).get("device_at_eos_flg"))) {
			return false;
		}
		
		//TODO: If a successful bonding confirmation is received from the patient app, AND the clinic-level 
		//DirectAlerts Settings or patient-level overrides have not changed for an existing patient 
		//(i.e. patient with a previously associated NGQ2 patient app installation).
		return true;
	}
}