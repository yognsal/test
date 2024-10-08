package com.mnet.mobility.utilities.profile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.DBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.reporting.TestStep;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.PatientInitiatedFollowupAttribute;

/**
 * Object formation and business logic validation for NGQ/ICM Patient Initiated
 * follow-up profile
 * 
 * @version 1.0
 * @author NAIKKX12
 */
public class PatientInitiatedFollowupProfile extends AppProfile {

	private static final int PATIENTINITFOLLOWUPCODE = 260;

	public PatientInitiatedFollowupProfile(PatientApp patientApp) {
		super(patientApp, PATIENTINITFOLLOWUPCODE);
	}

	@Override
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(PatientInitiatedFollowupAttribute.values());
	}

	/**@apiNote FUP DIM scripts - Config23313*/
	@Override
	public boolean validateAttribute(ProfileAttribute attribute) {
		Object expectedValue;
		
		switch ((PatientInitiatedFollowupAttribute) attribute) {
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
				expectedValue = getWorkflowState();
				break;
			case workflowTransactionID:
				expectedValue = getProfileCode();
				break;
			case contentVersion:
				expectedValue = getContentVersion();
				break;
			default:
				report.logStep(TestStep.builder()
						.failMessage((PatientInitiatedFollowupAttribute) attribute + " attribute not handled").build());
				return false;
		}
		
		return compareAttributesAndReport(attribute, expectedValue);
	}
	
	/**
	 * Local helper functions
	 */
	
	/**Retrieve workflow state as per Auto5418*/
	private boolean getWorkflowState() {
		Map<String, String> patient = PatientDBUtilities.getPatient(patientApp.getCurrentTest(), patientApp.getDeviceSerial());
		
		boolean lockoutUnscheduledTransmissions = DBUtilities.interpretDatabaseFlag(patient.get("lockout_patient_init_trans_flg"));
		boolean deviceInEOS = DBUtilities.interpretDatabaseFlag(patient.get("device_at_eos_flg"));
		
		return (deviceInEOS || lockoutUnscheduledTransmissions) ? false : true;
	}
}