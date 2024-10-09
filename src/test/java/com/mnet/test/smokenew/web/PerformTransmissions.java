package com.mnet.test.smoke.web;

import java.util.HashMap;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.middleware.utilities.PayloadExcelUtilities.PayloadAttribute;
import com.mnet.middleware.utilities.TransmissionUtilities;
import com.mnet.pojo.patient.Patient;
import com.mnet.reporting.utilities.GraylogReporting;

public class PerformTransmissions extends MITETest implements GraylogReporting {

	private TransmissionUtilities transmissionUtil;
	private String clinicalCommentDate;
	private String clinicalCommentText;
	private static final String sessionTime = "11:11:25";

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.REMOTE_MACHINE);
		super.initialize(context);

		// Initialize test-specific page objects
		transmissionUtil = new TransmissionUtilities(log, database, report, fileManager, remoteMachine);

	}

	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	private void performTransmissions(String custID, String DeviceModelNumber, String SerialNumber,
			String TransmitterNumber) {
		int transmissioncount = Integer.parseInt(FrameworkProperties.getProperty("TRANSMISSION_COUNT"));

		for (int i = 0; i < transmissioncount; i++) {

			Patient patient = new Patient();
			patient.setDeviceModelNum(DeviceModelNumber);
			patient.setDeviceSerialNum(SerialNumber);
			patient.setTransmitterSerialNum(TransmitterNumber);

			HashMap<String, String> payloadDataToUpdate = new HashMap<String, String>();
			payloadDataToUpdate.put(PayloadAttribute.CUSTOMERID.getRowName(), custID);
			payloadDataToUpdate.put(PayloadAttribute.DEVICEMODEL.getRowName(), patient.getDeviceModelNum());
			payloadDataToUpdate.put(PayloadAttribute.DEVICESERIAL.getRowName(), patient.getDeviceSerialNum());
			payloadDataToUpdate.put(PayloadAttribute.TRANSMIITERSERIAL.getRowName(), patient.getTransmitterSerialNum());
			payloadDataToUpdate.put(PayloadAttribute.SESSIONTIMEZONE.getRowName(), "America/Los_Angeles");
//			payloadDataToUpdate.put(PayloadAttribute.CLINICALCOMMENTDATE.getRowName(), clinicalCommentDate);
//			payloadDataToUpdate.put(PayloadAttribute.CLINICALCOMMENT.getRowName(), clinicalCommentText);

			// Use if need to update SESSIONDATEANDTIME
//			payloadDataToUpdate.put(PayloadAttribute.SESSIONDATEANDTIME.getRowName(), getPastDates(7) + " " + sessionTime);

			report.assertCondition(
					transmissionUtil.transmissionWithUserDefinedPayload(patient, null, null, null, payloadDataToUpdate),
					true,
					"100 S Send a valid transmission data package with clinical comments (Transmission A) via Tanto. ",
					"100 S Failed: Send a valid transmission data package with clinical comments (Transmission A) via Tanto. ");

		}

	}

//	private String getPastDates(int daysToSubtract) {
//		Instant now = Instant.now(Clock.system(ZoneId.of("UTC")));
//		Instant pastDate = now.minus(daysToSubtract, ChronoUnit.DAYS);
//		String date = pastDate.toString();
//
//		return date.split("T")[0];
//	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		// loginUtils.logout();
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.ALL_MICROSERVICES);
		}
		super.cleanup(result);
	}

}
