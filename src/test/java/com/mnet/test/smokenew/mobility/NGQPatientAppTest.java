package com.mnet.test.smoke.mobility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CustomerDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities.DeviceModelType;
import com.mnet.framework.api.APIResponse;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.AssertionLevel;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.middleware.utilities.PayloadExcelUtilities.PayloadAttribute;
import com.mnet.middleware.utilities.PayloadGenerator;
import com.mnet.middleware.utilities.TantoDriver.TantoTransmissionType;
import com.mnet.middleware.utilities.TransmissionUtilities;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.mobility.utilities.validation.BondingValidation;
import com.mnet.mobility.utilities.validation.SessionRecordValidation;
import com.mnet.pojo.clinic.admin.DirectAlertHomeTransmitter.ContactType;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.mobility.EncryptedPayload.TelemetryType;
import com.mnet.pojo.patient.Patient;
import com.mnet.pojo.patient.Patient.DeviceType;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicAdminNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.DirectAlertMobileAppTransmitterPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.ColumnCheckboxes;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientFilterType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientSearchType;
import com.mnet.webapp.pageobjects.patient.PatientProfileAndDeviceData;
import com.mnet.webapp.pageobjects.patient.TransmitterProfilePage;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;
import com.mnet.webapp.utilities.PatientUtilities;

/**
 * Session upload smoke test
 * 
 * @author NAIKKX12
 *
 */
public class NGQPatientAppTest extends MITETest implements GraylogReporting, BondingValidation, SessionRecordValidation {

	private ClinicNavigationBar navigationBar;
	private PatientListPage patientListPage;
	private PatientProfileAndDeviceData patientDataPage;
	private TransmitterProfilePage transmitterProfilePage;
	private DirectAlertMobileAppTransmitterPage directAlertMobileAppTransmitterPage;
	private ClinicAdminNavigationBar clinicAdminNavigationBar;

	private CustomerUtilities customerUtils;
	private PatientUtilities patientUtils;
	private LoginUtilities loginUtils;

	private PatientDBUtilities patientDBUtils;
	private CustomerDBUtilities customerDBUtils;
	private TransmissionUtilities transmissionUtils;
	
	private Customer customerObj;
	private Patient patient;
	private PayloadGenerator payloadGenerator;
	private NGQPatientApp ngqPatientApp;
	private APIResponse response;

	private String dateOfBirth, deviceModelNum, payloadFile, sessionDateTime, ngqPayloadPath, transmission_mail_subject;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.EMAIL);
		super.initialize(context);

		// Initialize test-specific page objects
		customerUtils = new CustomerUtilities(report, webDriver);
		patientUtils = new PatientUtilities(report, webDriver);
		navigationBar = new ClinicNavigationBar(webDriver, report);
		patientListPage = new PatientListPage(webDriver, report);
		loginUtils = new LoginUtilities(report, email, webDriver);
		transmitterProfilePage = new TransmitterProfilePage(webDriver, report);
		patientDataPage = new PatientProfileAndDeviceData(webDriver, report, database);
		directAlertMobileAppTransmitterPage = new DirectAlertMobileAppTransmitterPage(webDriver, report);
		clinicAdminNavigationBar = new ClinicAdminNavigationBar(webDriver, report);
		
		patientDBUtils = new PatientDBUtilities(report, database);
		customerDBUtils = new CustomerDBUtilities(report, database);
		transmissionUtils = new TransmissionUtilities(log, database, report, fileManager, remoteMachine);
		
		ngqPayloadPath = FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_NGQ");
		transmission_mail_subject = FrameworkProperties.getProperty("TRANSMISSION_MAIL_IDENTIFIER");
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);

		// Create Customer
		customerObj = new Customer();
		customerObj.setAddAllDevices(true);
		Customer customer = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
				: customerUtils.automatedCustomerSetupAndLogin(customerObj, email, true);
		if (customer == null) {
			report.logStep(ReportLevel.FAIL, "Failed to authorize application");
		}

		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "clinic navigation panel is not loaded");
		}
		report.logStepWithScreenshot(ReportLevel.INFO, "clinic navigation panel is loaded successfully");
		
		navigationBar.viewClinicAdministrationPage();
		if (!clinicAdminNavigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "clinic administration page is not loaded");
		}
		report.logStepWithScreenshot(ReportLevel.INFO, "clinic administration page is loaded successfully");
		
		clinicAdminNavigationBar.viewDirectAlertMobileAppTramsmitterPage();
		if (!directAlertMobileAppTransmitterPage.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Direct Alert settings -> Mobile App Transmitter page is not loaded");
		}
		report.logStepWithScreenshot(ReportLevel.INFO, "Direct Alert settings -> Mobile App Transmitter page is loaded successfully");
		
		directAlertMobileAppTransmitterPage.editPage();
		directAlertMobileAppTransmitterPage.selectRedAlertContactDuringOfficeHours(ContactType.EMAIL);
		directAlertMobileAppTransmitterPage.selectRedAlertContactAfterOfficeHours(ContactType.EMAIL);
		directAlertMobileAppTransmitterPage.selectYellowAlertContactAfterOfficeHours(ContactType.EMAIL);
		directAlertMobileAppTransmitterPage.selectYellowAlertContactDuringOfficeHours(ContactType.EMAIL);
		report.logStepWithScreenshot(ReportLevel.INFO, "Updated communication channel to email for red/yellow alerts...saving changes");
		
		directAlertMobileAppTransmitterPage.savePage();
		directAlertMobileAppTransmitterPage.waitMessagePopup();
		directAlertMobileAppTransmitterPage.handlePopup(true);
		directAlertMobileAppTransmitterPage.waitMessagePopup();
		directAlertMobileAppTransmitterPage.handlePopup(true);
		
		// add patient & perform transmission
		patient = new Patient();
		patient.setDeviceType(Patient.DeviceType.NGQ.getDeviceType());
		List<String> tempList = patientDBUtils.getIcdOrNgqDeviceList(DeviceModelType.NGQ);

		patient.setDeviceModelNum(tempList.get(CommonUtils.getRandomNumber(0, tempList.size() - 1)));
		if (!patientUtils.enrollPatient(patient, false, null, null, true, false, false)) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Patient enrollment failed");
		}
		report.logStepWithScreenshot(ReportLevel.INFO, "New patient with device serial number - "
				+ patient.getDeviceSerialNum() + " is enrolled successfully");

		customerDBUtils.setDDTVersion(TantoTransmissionType.FUA, patient.getDeviceSerialNum());
		
		dateOfBirth = DateUtility.changeDateFormat(patient.getDateOfBirth(),
				FrameworkProperties.getProperty("WEBAPP_DEFAULT_DATE_FORMAT"),
				FrameworkProperties.getProperty("PAYLOAD_ZIP_DATE_FORMAT"));
		deviceModelNum = patientDBUtils.getDeviceDetails(patient.getDeviceSerialNum()).get("device_model_num");

	    sessionDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
	    
		HashMap<String, String> payloadData = new HashMap<>(
				Map.ofEntries(Map.entry(PayloadAttribute.DEVICESERIAL.getRowName(), patient.getDeviceSerialNum()),
						Map.entry(PayloadAttribute.SESSIONDATEANDTIME.getRowName(), sessionDateTime),
						Map.entry(PayloadAttribute.DEVICEMODEL.getRowName(), deviceModelNum)));
		
		payloadGenerator = new PayloadGenerator(log, payloadData,
				System.getProperty("user.dir") + FrameworkProperties.getProperty("DEFAULT_NGQ_TRANSMISSION_TESTDATA"));
		payloadFile = payloadGenerator.generateNGQPayload();
		webDriver.staticWait(webDriver.minBrowserTimeout() * 30);
	}

	@Test
	public void recordUploadTest() {

		ngqPatientApp = new NGQPatientApp(this, MobilityOS.ANDROID);
		
		// Perform reg&bond and verify
		ngqPatientApp.firstTimeRegAndBond(patient.getDeviceSerialNum(), dateOfBirth);
		report.assertCondition(ngqPatientApp.getAzureId() != null, true,
				"Reg & Bond of patient with device serial '" + patient.getDeviceSerialNum() + "' is successful",
				"Reg & Bond of patient with device serial '" + patient.getDeviceSerialNum() + "' is failed");

		// DB validation
		webDriver.staticWait(webDriver.minBrowserTimeout() * 30);
		validateBonding(ngqPatientApp);

		// Azure verification
		/**
		 * PushNotificationTracker pushNotificationTracker = new
		 * PushNotificationTracker(log, report, azure);
		 * 
		 * NotificationStatus pushNotificationStatus =
		 * pushNotificationTracker.getPushNotificationStatus( regBond.getAzureId(),
		 * "tuv", Microservice.PATIENT_PROFILE_SERVICE, startTime);
		 * 
		 * report.assertValue(pushNotificationStatus, NotificationStatus.NoTargetFound,
		 * "Push notification sent to notification hub for azureId: " +
		 * regBond.getAzureId() + " | Notification Status: " +
		 * pushNotificationStatus.toString(), "Push notification could not be found for
		 * azureId:" + regBond.getAzureId());
		 **/

		// UI Validation
		webDriver.refreshPage();
		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}
		navigationBar.viewPatientList();
		if (!patientListPage.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load patient list page");
		}
		patientListPage.showHideColumn(true, ColumnCheckboxes.CONNECTIVITY);

		patientListPage.filterPatientByColumn(PatientFilterType.ALL);
		report.logStepWithScreenshot(ReportLevel.INFO, "Changed filter to 'All' on patient list page");

		String deviceSerialNum = patient.getDeviceSerialNum();
		
		report.assertWithScreenshot(
				patientListPage.searchPatientBy(deviceSerialNum, PatientSearchType.DEVICE), true,
				"Found patient with device serial: " + deviceSerialNum,
				"Failed to located patient with device serial: " + patient.getDeviceSerialNum());
		report.assertWithScreenshot(AssertionLevel.SOFT, patientListPage.IsPatientPaired(), true,
				"Green Tick Icon Under Connectivity Column Present.",
				"Green Tick Icon Under Connectivity Column NOT Present.");

		int transmissionBeforeSessionUpload = getTransmissionCount(this, deviceSerialNum, TelemetryType.FUA);
		
		response = ngqPatientApp.sessionRecordUpload(TelemetryType.FUA, 200, 
				FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_NGQ") + "\\" + payloadFile);

		report.assertCondition(response.getStatusCode() == HttpStatus.SC_OK, true, 
				"Session record API ran successfully",
				"Session record API failed");

		// Transmission mail validation
		String[] deviceInfo = transmissionUtils.deviceInfo(ngqPayloadPath);
		String dateSet = deviceInfo[2];
		String timeSet = deviceInfo[3];
		String updatedDate = DateUtility.changeDateFormat(dateSet, null, null);
		String updatedTime = DateUtility.changeTimeFormat(timeSet);
		log.info("updatedTime: " + updatedTime);
		log.info("updatedDate: " + updatedDate);
		
		report.assertCondition(AssertionLevel.SOFT,
				transmissionUtils.transmissionMailVerification(patient.getDeviceSerialNum(),
						patient.getDeviceModelNum(), updatedDate, updatedTime, email.getEmailCount(transmission_mail_subject)),
				true,
				"Transmission Mail Received.\nFollowing Details were verified:\n" + "Device Serial - "
						+ patient.getDeviceSerialNum() + "\nDevice Model - " + patient.getDeviceModelNum()
						+ "\nTransmission Date - " + updatedDate + "\nUpdated Time - " + updatedTime,
				"Transmission mail Not Received");
		
		webDriver.staticWait(webDriver.minBrowserTimeout() * 60);
		report.assertValue(getTransmissionCount(this, deviceSerialNum, TelemetryType.FUA), transmissionBeforeSessionUpload + 1,
				"Record is created in database for successful session upload",
				"No new record is created in database for session upload");
		

		webDriver.refreshPage();
		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar post refresh");
		}

		patientListPage.filterPatientByColumn(PatientFilterType.ALL);
		report.logStepWithScreenshot(ReportLevel.PASS, "Changed filter to 'All' on patient list page");

		report.assertWithScreenshot(
				patientListPage.searchPatientBy(patient.getDeviceSerialNum(), PatientSearchType.DEVICE), true,
				"Found patient with device serial: " + patient.getDeviceSerialNum(),
				"Failed to located patient with device serial: " + patient.getDeviceSerialNum());

		patientListPage.viewPatient();
		report.assertWithScreenshot(patientDataPage.validate(), true, "Succesfully navigated to patient profile page",
				"Failed to navigate to patient profile page");

		transmitterProfilePage.clickTransmitterSidebarOption();
		// Post-clicking transmitter option, takes time to update last transmission date
		webDriver.staticWait(webDriver.minBrowserTimeout() * 5);
		report.assertWithScreenshot(AssertionLevel.SOFT,
				transmitterProfilePage.verifyLastTransmissionDate(DeviceType.NGQ), true,
				"Session upload is Successful and validated on UI", "Session upload is not successful so UI validation failed");
	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		loginUtils.logout();
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.ALL_MICROSERVICES);
		}

		super.cleanup(result);
	}
}
