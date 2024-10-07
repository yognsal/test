package com.mnet.middleware.tools;

import org.apache.commons.lang3.StringUtils;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.reporting.TestStep;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoDriverType;
import com.mnet.middleware.utilities.TantoDriver.TantoTransmissionType;
import com.mnet.reporting.utilities.GraylogReporting;

/**
 * Utility tool for Tanto transmission uploads.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class TantoTransmissionTool extends MITETest implements GraylogReporting {

	private TantoDriver driver;
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.REMOTE_MACHINE);
		attributes.add(TestAttribute.DATABASE);
		relativeDataDirectory = "tools";
		super.initialize(context);
		
		driver = new TantoDriver(log, remoteMachine, fileManager, database, report);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void tantoTransmissionTool(String useDatabase, String driverType, String transmissionType,
			String deviceModel, String deviceSerial, String transmitterModel, String transmitterSerial, 
			String payloadFileName, String segmentSize) {
			
			boolean databaseSupport = useDatabase.equalsIgnoreCase("y");
			driver.setDatabaseSupport(databaseSupport);
			
			TantoDriverType tantoDriverType = (driverType.equalsIgnoreCase("9.x") ? TantoDriverType.DRIVER_9X : TantoDriverType.DRIVER_8X);
			TantoTransmissionType tantoTransmissionType = Enum.valueOf(TantoTransmissionType.class, transmissionType);
			
			String requestDetails = "\n Driver type: " + driverType + "\n Transmission type: " + transmissionType
					+ "\n Device model: " + deviceModel + "\n Device serial: " + deviceSerial;
			
			if (tantoDriverType == TantoDriverType.DRIVER_9X) {
				requestDetails += "\n Transmitter model: " + transmitterModel + "\n Transmitter serial: " + transmitterSerial;
			}
			
			requestDetails += "\n Payload file: " + payloadFileName;
			
			Integer segmentSizeValue = null;
			
			if (!StringUtils.isEmpty(segmentSize)) {
				segmentSizeValue = Integer.parseInt(segmentSize);
				requestDetails += "\n Segment size: " + segmentSize;
			}
			
			report.logStep(TestStep.builder()
					.message("Sending Tanto transmission with the following parameters: <textarea>"
					+ requestDetails + " </textarea>").build());
			
			transmitterModel = StringUtils.isEmpty(transmitterModel) ? null : transmitterModel;
			transmitterSerial = StringUtils.isEmpty(transmitterSerial) ? null : transmitterSerial;
					
			boolean transmissionProcessed = driver.sendTransmission(payloadFileName, tantoDriverType, tantoTransmissionType, 
					transmitterModel, transmitterSerial, deviceModel, deviceSerial, segmentSizeValue);
			
			report.assertCondition(transmissionProcessed, true, 
					TestStep.builder().message(transmissionType + " transmission was sent").build());
	}
	
	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, 
					Microservice.TANTO_ROUTING_SERVICE, Microservice.TRANSMISSION_ROUTING_SERVICE, Microservice.ETL_APP_SERVICE);
		}
		
		super.cleanup(result);
	}
}
