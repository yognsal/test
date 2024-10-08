package com.mnet.mobility.tools;

import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.pojo.mobility.EncryptedPayload.TelemetryType;
import com.mnet.reporting.utilities.GraylogReporting;

/**
 * 
 * Performs NGQ Session Record upload with option to Reg&Bond patient.
 * 
 * @author NAIKKX12, Arya Biswas
 *
 */
public class NGQSessionUpload extends MITETest implements GraylogReporting {
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.AZURE);
		relativeDataDirectory = "tools";
		super.initialize(context);
	}

	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void ngqSessionUpload(String deviceSerial, String dob, String os, String telemetryType, String workflowId, String transmissionFile, String appPropertiesFile) {
		NGQPatientApp ngqPatientApp;
		
		if (StringUtils.isEmpty(appPropertiesFile)) {
			ngqPatientApp = new NGQPatientApp(this, MobilityOS.valueOf(os.toUpperCase()));
			ngqPatientApp.setApiValidation(true);
			ngqPatientApp.firstTimeRegAndBond(deviceSerial, dob);
		} else {			
			ngqPatientApp = new NGQPatientApp(this, appPropertiesFile);
			ngqPatientApp.setApiValidation(true);
		}
		
		if (!transmissionFile.contains(".zip")) {
			transmissionFile += ".zip";
		}
		
		ngqPatientApp.sessionRecordUpload(TelemetryType.valueOf(telemetryType), Integer.parseInt(workflowId), 
				FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_NGQ") + "\\" + transmissionFile);
		
		// TODO: UI validation - already handled in smoke test
	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, 
					Microservice.NGQ_APP_CONFIG_SERVICE,
					Microservice.NGQ_APP_REGISTRATION_SERVICE,
					Microservice.NGQ_TRANSMISSION_SERVICE,
					Microservice.TRANSMISSION_ROUTING_SERVICE,
					Microservice.ETL_APP_SERVICE,
					Microservice.EVENT_EVALUATION_SERVICE);
		}

		super.cleanup(result);
	}
}