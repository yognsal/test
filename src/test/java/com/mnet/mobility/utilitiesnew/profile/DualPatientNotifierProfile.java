package com.mnet.mobility.utilities.profile;

import java.util.Arrays;
import java.util.List;

import com.mnet.database.utilities.DBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.DualPatientNotifierAttribute;

/**
 * Object formation and business logic validation for NGQ/ICM Dual PN profile
 * 
 * @version 1.0
 * @author NAIKKX12
 */
public class DualPatientNotifierProfile extends AppProfile {

	private static final int DUALPNPROFILECODE = 280;

	public DualPatientNotifierProfile(PatientApp patientApp) {
		super(patientApp, DUALPNPROFILECODE);
	}

	@Override
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(DualPatientNotifierAttribute.values());
	}

	@Override
	public boolean validateAttribute(ProfileAttribute attribute) {
		Object expectedValue;
		
		switch ((DualPatientNotifierAttribute) attribute) {
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
				expectedValue = DBUtilities.interpretDatabaseFlag(
						PatientDBUtilities.getPatient(patientApp.getCurrentTest(), 
								patientApp.getDeviceSerial()).get("device_at_eos_flg")) == true ? false : true;
				break;
			case contentVersion:
				expectedValue = getContentVersion();
				break;
			case workflowTransactionID:
				expectedValue = DUALPNPROFILECODE;
				break;
			default:
				patientApp.getCurrentTest().getReport().logStep(
						TestStep.builder().failMessage((DualPatientNotifierAttribute) attribute + " attribute not handled").build());
				return false;
		}
		
		if (expectedValue == null) {
			report.logStep(
					TestStep.builder().reportLevel(ReportLevel.WARNING).message("Expected value of '" + (DualPatientNotifierAttribute) attribute + "' attribute yet to be calculated").build());
			return true;
		}
		
		return compareAttributesAndReport(attribute, expectedValue);
	}
}