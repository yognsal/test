package com.mnet.test.sanity.mobility;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.mobility.utilities.PatientApp;

public class NGQCertificateRefreshTest extends MITETest {

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.DATABASE);
		relativeDataDirectory = "sanity/mobility";
		super.initialize(context);
	}
	
	// Certificate refresh API calls and validation
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void certificateRefreshTest(String appPropertiesFile) {
		PatientApp patientApp = new NGQPatientApp(this, appPropertiesFile);
		patientApp.setApiValidation(true);
		
		patientApp.patientAppCredentialRefresh();
		
		patientApp.patientAppIdentityRefresh();
	}
}
