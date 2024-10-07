package com.mnet.middleware.tools;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.XMLData;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoDriverType;
import com.mnet.middleware.utilities.TantoDriver.TantoProfileType;
import com.mnet.middleware.utilities.TantoProfileResponseValidator;
import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.pojo.xml.TantoComProfileRequest;
import com.mnet.pojo.xml.TantoPatientProfileRequest;
import com.mnet.reporting.utilities.GraylogReporting;

/**
 * Utility tool for Tanto patient profile / ComProfile uploads.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class TantoProfileTool extends MITETest implements GraylogReporting {

	private TantoDriver driver;
	private TantoProfileResponseValidator profile;
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.addAll(Arrays.asList(TestAttribute.REMOTE_MACHINE, TestAttribute.DATABASE));
		relativeDataDirectory = "tools";
		super.initialize(context);
		
		driver = new TantoDriver(log, remoteMachine, fileManager, report);
		profile = new TantoProfileResponseValidator(log, driver, database, report, true, false);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void tantoProfileTool(String validateProfile, String driverType, String profileType, String profileVersion,
			String deviceModel, String deviceSerial, String transmitterModel, String transmitterSerial,
			String transmitterSWVersion, String transmissionType, String timeOfFollowup, String transmitterModelExtension) {
		
		TantoDriverType tantoDriverType = (driverType.equalsIgnoreCase("9.x")) ? TantoDriverType.DRIVER_9X : TantoDriverType.DRIVER_8X;
		TantoProfileType tantoProfileType = Enum.valueOf(TantoProfileType.class, profileType);
		
		if (tantoProfileType == null) {
			report.logStep(TestStep.builder().reportLevel(ReportLevel.FAIL)
					.failMessage("Invalid profile type: " + profileType).build());
		}
		
		String profileRequestDetails = "\n Driver type: " + driverType + "\n Profile type: " + profileType
				+ "\n Profile version: " + profileVersion + "\n Transmitter model: " + transmitterModel
				+ "\n Transmitter serial: " + transmitterSerial;
		
		if (!StringUtils.isEmpty(deviceModel)) {
			profileRequestDetails += "\n Device model: " + deviceModel + "\n Device serial: " + deviceSerial;
		}
		
		if (!StringUtils.isEmpty(transmissionType)) {
			profileRequestDetails += "\n Transmission type: " + transmissionType + "\n TimeOfFollowup: " + timeOfFollowup;
		}
		
		if (tantoDriverType == TantoDriverType.DRIVER_9X) {
			profileRequestDetails += "\n TransmitterScriptSWVersion: 1 \n TransmitterScriptContentVersion: 0" 
					+ "\n TransmitterModelExtension: " + transmitterModelExtension;
		}
		
		report.logStep(TestStep.builder()
				.message("Sending Tanto patient profile request with the following parameters: <textarea>"
				+ profileRequestDetails + "</textarea>").build());
		
		XMLData profileRequest, profileResponse;
		
		if (tantoProfileType == TantoProfileType.ComProfile_IMD) {
			profileRequest = new TantoComProfileRequest(deviceModel, deviceSerial, transmitterSWVersion, profileVersion);			
		} else if (tantoProfileType == TantoProfileType.ComProfile_Transmitter) {
			profileRequest = new TantoComProfileRequest(transmitterSWVersion, profileVersion);
		} else if (tantoDriverType == TantoDriverType.DRIVER_9X){
			profileRequest = new TantoPatientProfileRequest(transmitterModel, transmitterSerial, 
					transmissionType, timeOfFollowup, transmitterSWVersion, profileVersion, "1", "0", transmitterModelExtension);
		} else {
			profileRequest = new TantoPatientProfileRequest(transmitterModel, transmitterSerial, 
					transmitterSWVersion, profileVersion);
		}
		
		if (StringUtils.isEmpty(deviceModel)) {
			profileResponse = driver.sendProfile(tantoProfileType, tantoDriverType, profileRequest, transmitterModel, transmitterSerial);
		} else {
			profileResponse = driver.sendProfile(tantoProfileType, tantoDriverType, profileRequest, 
					transmitterModel, transmitterSerial, deviceModel, deviceSerial);
		}
		
		report.assertCondition(profileResponse.isFailure(), false, 
				TestStep.builder().message("Received Tanto profile response").build());

		if (validateProfile.equalsIgnoreCase("y")) {
			boolean validationResult = profile.validateSubProfile(deviceModel, deviceSerial, transmitterSWVersion, profileVersion, profileResponse, TantoSubProfileType.ALERT_CONTROLS);
			
			report.assertCondition(validationResult, true, 
					TestStep.builder().message("Tanto profile response validated").build());
		}
	}
	
	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, 
					Microservice.TANTO_ROUTING_SERVICE, Microservice.TANTO_PATIENT_PROFILE_SERVICE, Microservice.TANTO_COMM_PROFILE_SERVICE);
		}
		
		super.cleanup(result);
	}
}
