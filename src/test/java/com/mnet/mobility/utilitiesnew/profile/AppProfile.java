package com.mnet.mobility.utilities.profile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.mobility.utilities.PatientApp;

import io.restassured.path.json.JsonPath;
import lombok.Getter;

/**
 * Represents a profile object in a mobility profile response (NGQ / ICM).
 * @version Q2 2024
 * @author NAIKKX12, Arya Biswas
 */
public abstract class AppProfile {

	protected PatientApp patientApp;
	protected TestReporter report;
	protected DatabaseConnector database;
	
	/**Profile content associated with profile_patient_container*/
	@Getter
	private JsonPath contentClob;
	/**Profile content associated with profile_default_container*/
	@Getter
	private JsonPath defaultContent;
	/**Profile code outlined in PIM*/
	@Getter
	private int profileCode;
	/**Latest version of profile content in profile_patient_container*/
	@Getter
	private int contentVersion;
	
	public AppProfile(PatientApp patientApp, int profileCode) {
		this.profileCode = profileCode;
		this.patientApp = patientApp;
		this.report = patientApp.getCurrentTest().getReport();
		this.database = patientApp.getCurrentTest().getDatabase();
		
		Map<String, String> patientContainer = getPatientContainer();
		
		contentClob = new JsonPath(patientContainer.get("content_clob"));
		defaultContent = new JsonPath(getDefaultContainerClob());
		contentVersion = (patientContainer.size() == 0) ? 0 : Integer.parseInt(patientContainer.get("content_version"));
	}
	
	/** Returns an array of all attributes represented in the given profile. */
	public abstract List<ProfileAttribute> getAllAttributes();
	
	/** Validates the given attribute */
	public abstract boolean validateAttribute(ProfileAttribute profileAttribute);
	
	/** Validates all attributes in the given profile*/
	public boolean validate() {
		boolean isValid = true;
		
		List<ProfileAttribute> attributes = getAllAttributes();

		for (ProfileAttribute attribute : attributes) {
			isValid = validateAttribute(attribute) ? isValid : false;
		}

		return isValid;
	}
	
	/** Returns value of attribute as defined in patient profile container.*/
	public Object getAttributeValue(ProfileAttribute profileAttribute) {
		return contentClob.get(profileAttribute.getJsonPath());
	}
		
	/** Returns value of attribute as defined in patient default container.*/
	protected Object getAttributeDefaultValue(ProfileAttribute profileAttribute) {
		return defaultContent.get(profileAttribute.getJsonPath());
	}
	
	/** Compares value in content clob to expected value (as per business logic) and reports accordingly.*/
	protected boolean compareAttributesAndReport(ProfileAttribute profileAttribute, Object expectedValue) {
		Object actualValue = getAttributeValue(profileAttribute);
		boolean attributeValid = Objects.equals(expectedValue, actualValue);
		
		ReportLevel reportLevel = attributeValid ? ReportLevel.INFO : ReportLevel.WARNING;
		
		report.logStep(TestStep.builder().reportLevel(reportLevel)
				.message("Attribute: " + profileAttribute.getJsonPath() + 
						" | Expected value: " + expectedValue + 
						" | Actual value: " + actualValue).build());
		return attributeValid;
	}
	
	/**
	 * Local helper functions
	 */
	
	/** Get default container based on profile code*/
	private String getDefaultContainerClob() {
		return PatientAppDBUtilities.getDefaultProfileContainer(this.patientApp.getCurrentTest(), getProfileCode()).get(0).get("content_clob");
	}
	
	/** Read content clob from profile.profile_patient_container table for provided profile code */
	private Map<String, String> getPatientContainer() {
		List<Map<String, String>> dbContent = PatientAppDBUtilities.getPatientProfileContainer(patientApp.getCurrentTest(), patientApp.getDeviceSerial(), profileCode);
		
		return (dbContent.size() == 0) ? Map.of() : dbContent.get(0);
	}
}