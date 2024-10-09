package com.mnet.test.smoke.web;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.pojo.clinic.admin.ClinicUser;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.customer.Customer.CustomerType;
import com.mnet.pojo.patient.Patient;
import com.mnet.pojo.patient.Patient.ScheduleType;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicAdminNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicUsersPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.EnrollmentCompletePage;
import com.mnet.webapp.pageobjects.clinic.patientlist.EnrollmentPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientFilterType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientSearchType;
import com.mnet.webapp.pageobjects.patient.BaselineDataEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.DirectAlertsNotificationsEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.FollowUpScheduleEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.PatientAndDeviceDataEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.TransmitterEnrollmentPage;
import com.mnet.webapp.utilities.CommonWebUtilities;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;

/**
 * Sanity test to create & edit customer and verify the profile
 * 
 * @author NAIKKX12
 *
 */
public class EnrollPatientInServiceProviderTest extends MITETest implements GraylogReporting {

	private ClinicNavigationBar clinicNavigationBar;
	private ClinicAdminNavigationBar clinicAdminNavigationBar;
	private ClinicUsersPage usersPage;
	private PatientListPage patientListPage;
	private EnrollmentPage enrollBtnPage;
	private PatientAndDeviceDataEnrollmentPage enrollPatientAndDeviceData;
	private TransmitterEnrollmentPage enrollTransmitter;
	private FollowUpScheduleEnrollmentPage enrollFollowUp;
	private DirectAlertsNotificationsEnrollmentPage enrollDirectAlerts;
	private BaselineDataEnrollmentPage enrollBaselineData;
	private EnrollmentCompletePage enrollmentComplete;

	private LoginUtilities loginUtils;
	private CustomerUtilities customerUtils;
	private CommonWebUtilities commonWebUtils;
	private CommonDBUtilities commonDBUtils;
	private Customer customerInfo;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.DATABASE);
		super.initialize(context);

		// Initialize test-specific page objects
		clinicNavigationBar = new ClinicNavigationBar(webDriver, report);
		clinicAdminNavigationBar = new ClinicAdminNavigationBar(webDriver, report);
		loginUtils = new LoginUtilities(report, email, webDriver);
		usersPage = new ClinicUsersPage(webDriver, report);
		patientListPage = new PatientListPage(webDriver, report);
		enrollBtnPage = new EnrollmentPage(webDriver, report);
		enrollPatientAndDeviceData = new PatientAndDeviceDataEnrollmentPage(webDriver, report, database);
		enrollTransmitter = new TransmitterEnrollmentPage(webDriver, report);
		enrollFollowUp = new FollowUpScheduleEnrollmentPage(webDriver, report);
		enrollDirectAlerts = new DirectAlertsNotificationsEnrollmentPage(webDriver, report);
		enrollBaselineData = new BaselineDataEnrollmentPage(webDriver, report);
		enrollmentComplete = new EnrollmentCompletePage(webDriver, report);
		commonDBUtils = new CommonDBUtilities(report, database);
		customerUtils = new CustomerUtilities(report, webDriver);
		commonWebUtils = new CommonWebUtilities(report, webDriver);
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);

		customerInfo = new Customer();
		customerInfo.setCustomerType(CustomerType.SERVICE_PROVIDER);
		customerInfo = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
				: customerUtils.automatedCustomerSetupAndLogin(customerInfo, email, true);
		if(customerInfo == null) {
			report.logStep(ReportLevel.FAIL, "Failed to authorize application");
		}
	}

	@Test
	public void enrollPatientInServiceProviderTest() {

		if (!clinicNavigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}

		commonWebUtils.pageRefreshWithWaits();

		if (!clinicNavigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar post refresh");
		}
		clinicNavigationBar.viewClinicAdministrationPage();
		report.assertWithScreenshot(clinicAdminNavigationBar.validate(), true,
				"Navigated and loaded Clinic Administration webpage succesfully.",
				"Failed to navigate and load the Clinic Administration webpage");
		clinicAdminNavigationBar.viewClinicUsersPage();
		report.assertWithScreenshot(usersPage.validate(), true,
				"Loaded Clinic Administration -> Clinic Users page succesfully.",
				"Failed to navigate and load the page : Clinic Administration -> Clinic Users'");
		ClinicUser user = new ClinicUser(null, null, null, null, null, null, null, null, null,
				String.valueOf(CommonUtils.getRandomNumber(10, 99)),
				String.valueOf(CommonUtils.getRandomNumber(100, 999)),
				String.valueOf(CommonUtils.getRandomNumber(1111111, 9999999)));
		user.setPatientDataEntry(true);
		report.assertWithScreenshot(
				usersPage.editUser(customerInfo.getMainContact_FirstName(), customerInfo.getMainContact_LastName(),
						user),
				true, "User with first name " + user.getFirstName() + " is edited successfully",
				"Failed to edit user with first name " + user.getFirstName() + "'");

		// Enroll patient
		Patient patient = new Patient();
		clinicNavigationBar.viewPatientList();
		report.assertWithScreenshot(patientListPage.validate(), true, "Successfully navigated to patient list page",
				"Failed to load patient list page");
		enrollBtnPage.clickNewPatientBtn();
		report.assertWithScreenshot(enrollBtnPage.ValidateNewPatientPage(), true,
				"Successfully navigated to New Patient Page", "Failed to navigate to New Patient Page");
		enrollBtnPage.clickEnrollBtn();
		report.assertWithScreenshot(enrollPatientAndDeviceData.validateManualEnrollmentPanel(), true,
				"Successfully navigated to Manual Enrollment Page", "Failed to navigate to Manual Enrollment Page");
		enrollPatientAndDeviceData.fillPatientDetails(patient);
		report.logStepWithScreenshot(ReportLevel.INFO,
				"Patient Created with following details: " + " Device Serial Num - " + patient.getDeviceSerialNum()
						+ " Device Model - " + patient.getDeviceModelNum() + " Firstname - " + patient.getFirstName()
						+ " Lastname - " + patient.getLastName() + " Patient ID - " + patient.getPatientId() + " DOB - "
						+ patient.getDateOfBirth());
		enrollPatientAndDeviceData.continueBtn();
		report.assertWithScreenshot(enrollTransmitter.ValidateTrasmitterDetailsPanel(), true,
				"Successfully navigated to Transmitter Details Page", "Failed to navigate to Transmitter Details Page");
		enrollTransmitter.fillTransmitterDetails(patient);
		report.logStepWithScreenshot(ReportLevel.INFO,
				"Transmitter Details set are: Transmitter Serial Num - " + patient.getTransmitterSerialNum());
		enrollTransmitter.clickContinueButtonAfterPatient();
		report.assertWithScreenshot(enrollFollowUp.ValidateFollowUpSchedulePanel(), true,
				"Successfully navigated to Follow-up Schedule Page", "Failed to navigate to Follow-up Schedule Page");
		enrollFollowUp.setFollowUpSchedule(ScheduleType.SMART, patient);
		report.logStepWithScreenshot(ReportLevel.INFO, "Follow-up Schedule successfully set for the patient");
		enrollFollowUp.clickContinueButton();
		report.assertWithScreenshot(enrollDirectAlerts.ValidateDirectAlertNotificationPanel(), true,
				"Successfully navigated to DirectAlerts Notification Page",
				"Failed to navigate to DirectAlerts Notification Page");
		enrollDirectAlerts.directAlerts();
		report.logStepWithScreenshot(ReportLevel.INFO, "DirectAlterts Notifications successfully set for the patient");
		enrollDirectAlerts.clickContinueButton();
		report.assertWithScreenshot(enrollBaselineData.ValidateBaselineClinicalDataPanel(), true,
				"Successfully navigated to Baseline Clinical Data Page",
				"Failed to navigate to Baseline Clinical Data Page");
		enrollBaselineData.baselineClinicalData();
		enrollBaselineData.clickContinueButtonAfterPatient();
		report.assertWithScreenshot(enrollmentComplete.ValidateEnrollmentCompletePanel(), true, "Enrollment Complete",
				"Failed to enroll patient");
		enrollmentComplete.clickFinishButton();
		report.assertWithScreenshot(enrollmentComplete.validate(), true, "Successfully Finished", "Failed to Finish");
		patientListPage.filterPatientByColumn(PatientFilterType.ALL);
		report.logStepWithScreenshot(ReportLevel.PASS, "Changed filter to All on patient list page");
		report.assertWithScreenshot(
				patientListPage.searchPatientBy(patient.getDeviceSerialNum(), PatientSearchType.DEVICE), true,
				"Found patient with device serial: " + patient.getDeviceSerialNum(),
				"Failed to located patient with device serial: " + patient.getDeviceSerialNum());
	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		loginUtils.logout();
		if(!manualCustomerSetup) {
			commonDBUtils.updateMFA(customerInfo.getMainContact_EmailID());
		}
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.ALL_MICROSERVICES);
		}

		super.cleanup(result);
	}
}