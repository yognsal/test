package com.mnet.mobility.utilities.profile;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.DBUtilities;
import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.DateUtility.DateTimeFormat;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.DevMedAttribute;

/**
 * Object formation and business logic validation for NGQ/ICM DevMed profile (Auto5442)
 * 
 * @version Q2 2024
 * @author NAIKKX12
 */
public class DevMedProfile extends AppProfile {

	private static final int DEVMEDPROFILECODE = 270;

	public DevMedProfile(PatientApp patientApp) {
		super(patientApp, DEVMEDPROFILECODE);
	}

	@Override
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(DevMedAttribute.values());
	}

	/** Validate requested attribute */
	public boolean validateAttribute(ProfileAttribute attribute) {
		Object expectedValue;
		
		switch ((DevMedAttribute) attribute) {
			case bleSessionTimeout:
				expectedValue = String.valueOf(getAttributeDefaultValue(attribute));
				break;
			case retryOnFailure:
			case payloadsToRescheduleWorkflow:
			case scriptToExecute_scriptsAvailable:
			case scriptToExecute_sequenceName:
			case uploadPayloadLabel:
			case eventTime_eventType:
			case eventTime_eventSchedule_periodic_eventInterval:
				expectedValue = getAttributeDefaultValue(attribute);
				break;
			case workflowState:
				expectedValue = DBUtilities.interpretDatabaseFlag(
						PatientDBUtilities.getPatient(patientApp.getCurrentTest(), 
								patientApp.getDeviceSerial()).get("device_at_eos_flg")) == true ? false : true;
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
			case contentVersion:
				expectedValue = getContentVersion();
				break;
			case workflowTransactionID:
				expectedValue = getProfileCode();
				break;
			default:
				report.logStep(
						TestStep.builder().failMessage((DevMedAttribute) attribute + " attribute not handled").build());
				return false;
		}
		
		if (expectedValue == null) {
			report.logStep(
					TestStep.builder().reportLevel(ReportLevel.WARNING).message("Expected value of '" + (DevMedAttribute) attribute + "' attribute yet to be calculated").build());
			return true;
		}
		return compareAttributesAndReport(attribute, expectedValue);
	}
}