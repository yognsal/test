package com.mnet.test.sanity.mobility;

import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.mobility.utilities.Keyfactor;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.MobilityUtilities.ProfileType;
import com.mnet.pojo.mobility.AppLifeEvent.AppLifeEventResult;
import com.mnet.pojo.mobility.ale.ContentUpdateReceipt;
import com.mnet.pojo.mobility.ale.ProfileReceipt;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.PushNotificationTracker;
import com.mnet.reporting.utilities.GraylogReporting.Microservice;
import com.windowsazure.messaging.NotificationStatus;

/**
 * Tests to verify basic functionality of mobility components.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class NGQMobilityValidationTest extends MITETest {

	private Keyfactor certificateStore;
	private PushNotificationTracker pushNotificationTracker;
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.AZURE);
		attributes.add(TestAttribute.DATABASE);
		relativeDataDirectory = "sanity/mobility";
		super.initialize(context);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void keyfactorTest(String deviceSerial, String azureId) {
		certificateStore = new Keyfactor(log);
		
		report.assertCondition(certificateStore.hasActiveIMDIdentity(Integer.parseInt(deviceSerial)), true, 
				"Active IMDIdentityRecord exists for device serial: " + deviceSerial,
				"IMDIdentityRecord is missing or revoked for device serial: " + deviceSerial);
		
		report.assertCondition(certificateStore.hasActivePatientAppCertificates(azureId), true, 
				"Active PatientAppIdentity / PatientAppCredential exists for azureId: " + azureId, 
				"PatientAppIdentity / PatientAppCredential is missing or revoked for azureId: " + azureId);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void pushNotificationTest(String deviceSerial, String dateOfBirth) {
		long startTime = System.currentTimeMillis();
		
		PatientApp patientApp = new NGQPatientApp(this, MobilityOS.ANDROID);
		String azureId = patientApp.firstTimeRegAndBond(deviceSerial, dateOfBirth);		
		pushNotificationTracker = new PushNotificationTracker(log, report, azure);
		
		NotificationStatus pushNotificationStatus = pushNotificationTracker.getPushNotificationStatus(
				azureId, "tuv", Microservice.PATIENT_PROFILE_SERVICE, startTime);
		
		report.assertValue(pushNotificationStatus, 
				NotificationStatus.NoTargetFound, 
				"Push notification sent to notification hub for azureId: " + azureId + " | Notification Status: " + pushNotificationStatus.toString(),
				"Push notification could not be found for azureId:" + azureId);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void appLifeEventTest(String deviceSerial, String dateOfBirth, String appPropertiesFile) {
		PatientApp patientApp;
		
		if (StringUtils.isEmpty(appPropertiesFile)) {
			patientApp = new NGQPatientApp(this, MobilityOS.ANDROID);
			patientApp.setApiValidation(true);
			patientApp.firstTimeRegAndBond(deviceSerial, dateOfBirth);
		} else {			
			patientApp = new NGQPatientApp(this, appPropertiesFile);
			patientApp.setApiValidation(true);
		}
		
		RestAPIManager.logAllToConsole();
		
		//patientApp.sendD2CMessage(new WorkflowStatus(patientApp, AppLifeEventResult.SERVICE_APP_STATE, ProfileType.SCHEDULED_FOLLOW_UP));
		//patientApp.sendD2CMessage(new PhoneProfileChange(patientApp, AppLifeEventResult.SUCCESS));
		patientApp.sendD2CMessage(new ProfileReceipt(patientApp, AppLifeEventResult.SUCCESS, ProfileType.SCHEDULED_FOLLOW_UP, ProfileType.SCHEDULED_DEVICE_CHECK));
		patientApp.sendD2CMessage(new ContentUpdateReceipt(patientApp, AppLifeEventResult.SUCCESS));
		//patientApp.sendD2CMessage(new LegalAndPrivacy(patientApp, LegalAndPrivacyConsent.ALL));
	}
}
