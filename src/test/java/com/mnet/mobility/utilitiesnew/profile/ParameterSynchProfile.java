package com.mnet.mobility.utilities.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.ParameterSynchAttribute;
import com.mnet.webapp.utilities.directalerts.DirectAlertsSelection;

/**
 * Object formation and business logic validation for NGQ/ICM Parameter Synch
 * profile
 * 
 * @version 1.0
 * @author NAIKKX12
 */
public class ParameterSynchProfile extends AppProfile implements DirectAlertsSelection {

	private static final int PARAMETERSYNCHCODE = 840;

	public ParameterSynchProfile(PatientApp patientApp) {
		super(patientApp, PARAMETERSYNCHCODE);
	}

	/** Get all attribute names */
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(ParameterSynchAttribute.values());
	}

	/** Validate requested attribute */
	public boolean validateAttribute(ProfileAttribute attribute) {
		Object expectedValue;

		switch ((ParameterSynchAttribute) attribute) {
		case id:
			expectedValue = String.valueOf(PARAMETERSYNCHCODE);
			break;
		case contentVersion:
			expectedValue = getContentVersion();
			break;
		case dimVersion:
			String dimMajorVersion = patientApp.getDIMMajorVersion();
			String actualDimVersion = (String) getAttributeValue(ParameterSynchAttribute.dimVersion);

			report.logStep(TestStep.builder().message("Attribute: " + attribute + " | Expected value: "
					+ dimMajorVersion + " | Actual value: " + actualDimVersion).build());

			return ((!StringUtils.isEmpty(actualDimVersion)) && actualDimVersion.startsWith(dimMajorVersion));
		case instructions:
			Map<String, String> expectedAlertSelection = expectedInstructions();
			Map<String, String> actualAlertSelection = getUIMappingData(patientApp);
			report.logStep(TestStep.builder().message("Attribute: " + attribute + " | Expected value: "
					+ expectedAlertSelection + " | Actual value: " + actualAlertSelection).build());
			return expectedAlertSelection.equals(actualAlertSelection);
		default:
			report.logStep(TestStep.builder()
					.failMessage((ParameterSynchAttribute) attribute + " attribute not handled").build());
			return false;
		}

		if (expectedValue == null) {
			report.logStep(TestStep.builder().reportLevel(ReportLevel.WARNING).message(
					"Expected value of '" + (ParameterSynchAttribute) attribute + "' attribute yet to be calculated")
					.build());
			return true;
		}

		return compareAttributesAndReport(attribute, expectedValue);
	}

	/**
	 * Get all alert mapping from 840 profile instruction attribute whether they are on (02) or Off (00)
	 */
	private Map<String, String> expectedInstructions() {
		FrameworkLog log = patientApp.getCurrentTest().getLog();
		String attributeValue = (String) getAttributeValue(ParameterSynchAttribute.instructions).toString();
		List<String> payloads = Arrays.asList(attributeValue.split("},"));
		List<String> codes = new ArrayList<String>();
		
		//{cmdType=Standard, requestType=0x62, payload=8E 2F 02 01 00
		//getting 8E 2F 02 01 00 value from above string
		for (String payload : payloads) {
			if (payload.contains("payload")) {
				codes.add(payload.substring(payload.lastIndexOf("payload=") + 8));
			}
		}
		//Getting last two characters from this kind of string 8E 2F 02 01 00
		//if we get 00 then alert is considered Off or not present on UI
		//if we get 02 then alert is considered On (Red or yellow) and present on UI
		Map<String, String> alertStatus = new HashMap<String, String>();
		for (String code : codes) {
			if (code.substring(12, 14).equals("00")) {
				alertStatus.put(code.substring(0, 11), "Off");
			} else {
				alertStatus.put(code.substring(0, 11), "On");
			}
		}
		log.info("Encoded Direct Alerts Selection Status from Patient Profile Container: " + alertStatus);
		return alertStatus;
	}
}