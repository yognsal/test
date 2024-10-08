package com.mnet.test.sanity.mobility;

import java.text.ParseException;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.framework.utilities.DateUtility.DateTimeFormat;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.validation.SessionRecordValidation;

/**
 * Sanity test for Last Activity API call and validation
 * @author NAIKKX12
 *
 */
public class LastActivityTest extends MITETest implements SessionRecordValidation {
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.AZURE);
		relativeDataDirectory = "sanity/mobility";
		super.initialize(context);
	}
	
	// Last Activity API call and validation
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void lastActivityTest(String appPropertiesFile) throws ParseException {
		String databaseDateTimeFormat = DateTimeFormat.PAYLOAD.getFormat();
		
		PatientApp patientApp = new NGQPatientApp(this, appPropertiesFile);
		patientApp.setApiValidation(true);
		
		String date = DateUtility.modifiedDate(70, databaseDateTimeFormat);
		patientApp.lastActivity(date);
	}
}
