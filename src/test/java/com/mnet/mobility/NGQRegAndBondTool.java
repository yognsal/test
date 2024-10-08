package com.mnet.mobility.tools;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.reporting.utilities.GraylogReporting;

/**
 * 
 * Basic reg & bond flow w.r.t. D3 for FVT as an utility
 * 
 * @author NAIKKX12
 *
 */
public class NGQRegAndBondTool extends MITETest implements GraylogReporting {
	
	private PatientApp patientApp;
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.AZURE);
		relativeDataDirectory = "tools";
		super.initialize(context);
	}

	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void ngqRegAndBondTool(String deviceSerial, String dob, String deviceType) {
		patientApp = new NGQPatientApp(this, MobilityOS.valueOf(deviceType.toUpperCase()));
		patientApp.setApiValidation(true);
		patientApp.firstTimeRegAndBond(deviceSerial, dob);
	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, 
					Microservice.NGQ_APP_CONFIG_SERVICE, 
					Microservice.NGQ_APP_LIFE_EVENT_SERVICE,
					Microservice.NGQ_APP_REGISTRATION_SERVICE,
					Microservice.MOBILITY_SYNC_SERVICE);
		}
		
		super.cleanup(result);
	}
}