package com.mnet.mobility.utilities.profile;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.database.utilities.DBUtilities;
import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.DateUtility.DateTimeFormat;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.DevMedAttribute;
import com.mnet.mobility.utilities.profile.enums.ScheduledDeviceCheckAttribute;

/**
 * Object formation and business logic validation for NGQ/ICM Scheduled device (Auto5466)
 * check profile
 * 
 * @version 1.0
 * @author NAIKKX12
 */
public class ScheduledDeviceCheckProfile extends AppProfile {

	private static final int SCHEDDEVICECHECKCODE = 210;
	private CommonDBUtilities commonDBUtils;

	public ScheduledDeviceCheckProfile(PatientApp patientApp) {
		super(patientApp, SCHEDDEVICECHECKCODE);
		commonDBUtils = new CommonDBUtilities(report, database);
	}

	@Override
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(ScheduledDeviceCheckAttribute.values());
	}

	@Override
	public boolean validateAttribute(ProfileAttribute attribute) {
		Object expectedValue;
		
		switch ((ScheduledDeviceCheckAttribute) attribute) {
			case bleSessionTimeout:
			case retryOnFailure:
			case payloadsToRescheduleWorkflow:
			case scriptToExecute_scriptsAvailable:
			case scriptToExecute_sequenceName:
			case uploadPayloadLabel:
			case eventTime_eventType:
			case eventTime_eventSchedule_periodic_eventInterval:
				expectedValue = getAttributeDefaultValue(attribute);
				break;
			case eventTime_eventSchedule_periodic_eventTime:
				List<Map<String, String>> dbContent = PatientAppDBUtilities.getPatientProfileContainer
				(patientApp.getCurrentTest(), patientApp.getDeviceSerial(), getProfileCode());
				expectedValue = dbContent.size() > 0 ? dbContent.get(0).get("last_updt_dtm").split(" ")[0] : "";
				
				Date date = new Date((Long.parseLong(String.valueOf(getAttributeValue
						(DevMedAttribute.eventTime_eventSchedule_periodic_eventTime))) + 86400) * 1000);
				String actualValue = new SimpleDateFormat(DateTimeFormat.PAYLOAD.getFormat()).format(date);
				
				report.logStep(TestStep.builder().message("Attribute: " + attribute + 
						" | Expected value: " + expectedValue + 
						" | Actual value: " + actualValue).build());
				return actualValue.equals(expectedValue);
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
				report.logStep(
						TestStep.builder().failMessage((ScheduledDeviceCheckAttribute) attribute + " attribute not handled").build());
				return false;
		}
		
		if (expectedValue == null) {
			report.logStep(
					TestStep.builder().reportLevel(ReportLevel.WARNING).message("Expected value of '" + (ScheduledDeviceCheckAttribute) attribute + "' attribute yet to be calculated").build());
			return true;
		}
		
		return compareAttributesAndReport(attribute, expectedValue);
	}
	
	private boolean getWorkFlowState() {
		boolean deviceCheck = DBUtilities.interpretDatabaseFlag(
				commonDBUtils.getDBOverride("device_check_flg", patientApp.getDeviceSerial()));
		boolean eos = DBUtilities.interpretDatabaseFlag(PatientDBUtilities.getPatient(
				patientApp.getCurrentTest(), patientApp.getDeviceSerial()).get("device_at_eos_flg"));
		
		return (deviceCheck == false || eos == true) ? false : true;
	}
}