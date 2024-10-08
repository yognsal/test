package com.mnet.test.sanity.mobility;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.core.MITETest;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.validation.ProfileValidation;

public class NGQProfileTest extends MITETest implements ProfileValidation {

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.DATABASE);
		relativeDataDirectory = "sanity/mobility";
		super.initialize(context);
	}
	
	// TODO: End-to-end profile test with profile trigger validation
	
	/**260 profile test with existing patient*/
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void patientInitiatedFollowupTest(String appPropertiesFile) {
		PatientApp patientApp = new NGQPatientApp(this, appPropertiesFile);
		
		report.assertCondition(validateProfile(patientApp, AppProfileType.PATIENT_INITIATED_FOLLOWUP), true, 
				TestStep.builder().message("260 Profile attributes validated").build());
	}
	
}
