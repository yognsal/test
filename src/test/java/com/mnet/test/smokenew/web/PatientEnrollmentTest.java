package com.mnet.test.smoke.web;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.AssertionLevel;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.patient.Patient;
import com.mnet.pojo.patient.Patient.ScheduleType;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.patientlist.EnrollmentCompletePage;
import com.mnet.webapp.pageobjects.clinic.patientlist.EnrollmentPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientFilterType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientSearchType;
import com.mnet.webapp.pageobjects.patient.BaselineDataEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.BaselineDetailsProfilePage;
import com.mnet.webapp.pageobjects.patient.DirectAlertsNotificationsEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.DirectAlertsNotificationsProfilePage;
import com.mnet.webapp.pageobjects.patient.FollowUpScheduleEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.FollowUpScheduleProfilePage;
import com.mnet.webapp.pageobjects.patient.PatientAndDeviceDataEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.PatientProfileAndDeviceData;
import com.mnet.webapp.pageobjects.patient.TransmitterEnrollmentPage;
import com.mnet.webapp.pageobjects.patient.TransmitterProfilePage;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;
import com.mnet.webapp.utilities.PatientUtilities;
import com.mnet.webapp.utilities.directalerts.DirectAlertsSelection;

/**
 * Sample web test demonstrating enrollment of patient and verifying the patient
 * is created or not.
 * 
 * @version March 2023
 * @author Rishab Kotwal
 */
public class PatientEnrollmentTest extends MITETest implements GraylogReporting, DirectAlertsSelection {

	private ClinicNavigationBar navigationBar;
	private PatientListPage patientListPage;
	private LoginUtilities loginUtils;
	private EnrollmentPage enrollBtnPage;
	private PatientAndDeviceDataEnrollmentPage enrollPatientAndDeviceData;
	private TransmitterEnrollmentPage enrollTransmitter;
	private FollowUpScheduleEnrollmentPage enrollFollowUp;
	private DirectAlertsNotificationsEnrollmentPage enrollDirectAlerts;
	private BaselineDataEnrollmentPage enrollBaselineData;
	private EnrollmentCompletePage enrollmentComplete;
	private PatientProfileAndDeviceData patientAndDeviceData;
	private TransmitterProfilePage transmitter;
	private FollowUpScheduleProfilePage followUp;
	private DirectAlertsNotificationsProfilePage directAlerts;
	private BaselineDetailsProfilePage baselineData;
	private Customer customerInfo;
	private CommonDBUtilities commonDBUtils;
	private CustomerUtilities customerUtils;
	private PatientUtilities patientUtils;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.EMAIL);
		super.initialize(context);

		// Initialize test-specific page objects
		navigationBar = new ClinicNavigationBar(webDriver, report);
		patientListPage = new PatientListPage(webDriver, report);
		loginUtils = new LoginUtilities(report, email, webDriver);
		enrollBtnPage = new EnrollmentPage(webDriver, report);
		enrollPatientAndDeviceData = new PatientAndDeviceDataEnrollmentPage(webDriver, report, database);
		enrollTransmitter = new TransmitterEnrollmentPage(webDriver, report);
		enrollFollowUp = new FollowUpScheduleEnrollmentPage(webDriver, report);
		enrollDirectAlerts = new DirectAlertsNotificationsEnrollmentPage(webDriver, report);
		enrollBaselineData = new BaselineDataEnrollmentPage(webDriver, report);
		enrollmentComplete = new EnrollmentCompletePage(webDriver, report);
		patientAndDeviceData = new PatientProfileAndDeviceData(webDriver, report, database);
		transmitter = new TransmitterProfilePage(webDriver, report);
		followUp = new FollowUpScheduleProfilePage(webDriver, report);
		directAlerts = new DirectAlertsNotificationsProfilePage(webDriver, report);
		baselineData = new BaselineDetailsProfilePage(webDriver, report);
		commonDBUtils = new CommonDBUtilities(report, database);
		customerUtils = new CustomerUtilities(report, webDriver);
		patientUtils = new PatientUtilities(report, webDriver);
	}

	@Test
	public void patientEnrollmentTest() {

		customerInfo = new Customer();
		Customer customer = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
				: customerUtils.automatedCustomerSetupAndLogin(customerInfo, email, true);
		if(customer == null) {
			report.logStep(ReportLevel.FAIL, "Failed to authorize application");
		}

		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}

		Patient patient = new Patient();
		patientUtils.pageRefreshWithWaits();

		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}

		report.logStep(ReportLevel.INFO, "Navigating to patient list page...");

		navigationBar.viewPatientList();
		report.assertWithScreenshot(patientListPage.validate(), true, "Successfully navigated to patient list page",
				"Failed to load patient list page");

		report.logStep(ReportLevel.INFO, "Navigating to New Patient page");
		enrollBtnPage.clickNewPatientBtn();
		report.assertWithScreenshot(enrollBtnPage.ValidateNewPatientPage(), true,
				"Successfully navigated to New Patient Page", "Failed to navigate to New Patient Page");

		report.logStep(ReportLevel.INFO, "Navigating to manual enrollment page");
		enrollBtnPage.clickEnrollBtn();
		report.assertWithScreenshot(enrollPatientAndDeviceData.validateManualEnrollmentPanel(), true,
				"Successfully navigated to Manual Enrollment Page", "Failed to navigate to Manual Enrollment Page");

		report.logStep(ReportLevel.INFO, "Filling Patient Details");
		enrollPatientAndDeviceData.fillPatientDetails(patient);
		report.logStepWithScreenshot(ReportLevel.INFO,
				"Patient Created with following details: " + " Device Serial Num - " + patient.getDeviceSerialNum()
						+ " Device Model - " + patient.getDeviceModelNum() + " Firstname - " + patient.getFirstName()
						+ " Lastname - " + patient.getLastName() + " Patient ID - " + patient.getPatientId() + " DOB - "
						+ patient.getDateOfBirth());

		report.logStep(ReportLevel.INFO, "Navigating to Transmitter Details Page");
		enrollPatientAndDeviceData.continueBtn();
		report.assertWithScreenshot(enrollTransmitter.ValidateTrasmitterDetailsPanel(), true,
				"Successfully navigated to Transmitter Details Page", "Failed to navigate to Transmitter Details Page");

		report.logStep(ReportLevel.INFO, "Filling Transmitter Details");
		enrollTransmitter.fillTransmitterDetails(patient);
		report.logStepWithScreenshot(ReportLevel.INFO,
				"Transmitter Details set are: Transmitter Serial Num - " + patient.getTransmitterSerialNum());

		report.logStep(ReportLevel.INFO, "Navigating to Follow-up Schedule Page");
		enrollTransmitter.clickContinueButtonAfterPatient();
		report.assertWithScreenshot(enrollFollowUp.ValidateFollowUpSchedulePanel(), true,
				"Successfully navigated to Follow-up Schedule Page", "Failed to navigate to Follow-up Schedule Page");

		report.logStep(ReportLevel.INFO, "Setting up Follow-up Schedule");
		enrollFollowUp.setFollowUpSchedule(ScheduleType.SMART, patient);
		report.logStepWithScreenshot(ReportLevel.INFO, "Follow-up Schedule successfully set for the patient");

		report.logStep(ReportLevel.INFO, "Navigating to DirectAlerts Notification Page");
		enrollFollowUp.clickContinueButton();
		report.assertWithScreenshot(enrollDirectAlerts.ValidateDirectAlertNotificationPanel(), true,
				"Successfully navigated to DirectAlerts Notification Page",
				"Failed to navigate to DirectAlerts Notification Page");

		report.logStep(ReportLevel.INFO, "Setting up DirectAlerts Notification");
		enrollDirectAlerts.directAlerts();
		report.logStepWithScreenshot(ReportLevel.INFO, "DirectAlterts Notifications successfully set for the patient");

		report.logStep(ReportLevel.INFO, "Navigating to Baseline Clinical Data Page");
		enrollDirectAlerts.clickContinueButton();
		report.assertWithScreenshot(enrollBaselineData.ValidateBaselineClinicalDataPanel(), true,
				"Successfully navigated to Baseline Clinical Data Page",
				"Failed to navigate to Baseline Clinical Data Page");

		report.logStep(ReportLevel.INFO, "Filling Baseline Clinical Data");
		enrollBaselineData.baselineClinicalData();
		report.logStepWithScreenshot(ReportLevel.INFO, "Baseline Clinical Data Filled");

		enrollBaselineData.clickContinueButtonAfterPatient();
		if (!enrollmentComplete.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load Enrollment complete page");
		}
		enrollmentComplete.clickFinishButton();
		report.assertWithScreenshot(patientListPage.validate(), true, "Successfully Finished patient enrollmebt",
				"Failed to Finish");
		report.logStep(ReportLevel.INFO, "Back to Patient List Page");

		patientListPage.filterPatientByColumn(PatientFilterType.ALL);
		report.logStepWithScreenshot(ReportLevel.PASS, "Changed filter to 'All' on patient list page");

		report.logStep(ReportLevel.INFO, "Searching for patient...");
		report.assertWithScreenshot(
				patientListPage.searchPatientBy(patient.getDeviceSerialNum(), PatientSearchType.DEVICE), true,
				"Found patient with device serial: " + patient.getDeviceSerialNum(),
				"Failed to located patient with device serial: " + patient.getDeviceSerialNum());

		PatientDBUtilities databaseUtils = new PatientDBUtilities(report, database);

		report.assertCondition(databaseUtils.getPatient(patient.getFirstName(), patient.getLastName()).size() > 0, true,
				"Patient Created", "No Patient is created in database for '" + patient.getFirstName() + "'");
		report.logStep(ReportLevel.INFO, "Viewing the patient...");
		patientListPage.viewPatient();
		report.assertWithScreenshot(patientAndDeviceData.validate(), true,
				"Succesfully navigated to patient profile page", "Failed to navigate to patient profile page");

		Patient newPatient = new Patient();
		newPatient.setScheduleType(ScheduleType.MANUAL);
		report.logStep(ReportLevel.INFO, "Editing patient details");
		patientAndDeviceData.clickEditButton();
		if (!patientAndDeviceData.isPageInEditMode()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Page not in editable mode");
		}
		patientAndDeviceData.editPatientDetails(newPatient);
		patientAndDeviceData.clickSaveButton();
		report.assertWithScreenshot(patientAndDeviceData.isPageSaved(), true, "Edited patient details",
				"Failed to edit patient details");

		report.logStep(ReportLevel.INFO, "Editing Transmitter details");
		transmitter.clickTransmitterSidebarOption();
		transmitter.clickEditButton();
		if (!transmitter.isPageInEditMode()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Page not in editable mode");
		}
		transmitter.editTransmitterDetails(newPatient);
		transmitter.clickSaveButton();
		report.assertWithScreenshot(transmitter.isPageSaved(), true, "Edited transmitter details",
				"Failed to edit transmitter details");

		report.logStep(ReportLevel.INFO, "Editing Follow-up Schedule details");
		followUp.isFollowUpPageLoaded();
		followUp.clickEditButton();
		if (!followUp.isPageInEditMode()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Page not in editable mode");
		}
		followUp.editFollowUpDetails(newPatient);
		followUp.clickSaveBtn();
		report.assertWithScreenshot(followUp.isPageSaved(), true, "Edited Follow-up details",
				"Failed to edit Follow-up details");

		report.logStep(ReportLevel.INFO, "Editing DirectAlerts Notification details");
		directAlerts.isDirectAlertsPageLoaded();
		directAlerts.clickEditButton();
		if (!directAlerts.isPageInEditMode()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Page not in editable mode");
		}
		selectDirectAlerts(PageSelection.DIRECT_ALERTS_PROFILE, webDriver, AlertClassification.YELLOW, true,
				DirectAlerts.TACHY_THERAPY_DISABLED, DirectAlerts.DEVICE_PROGRAMMED_TO_EMERGENCY_PACING_VALUES,
				DirectAlerts.CHARGE_TIME_LIMIT_REACHED);
		directAlerts.clickSaveButton();
		report.assertWithScreenshot(directAlerts.isPageSaved(), true, "Edited Direct Alerts details",
				"Failed to edit Direct alerts details");

		report.logStep(ReportLevel.INFO, "Editing Baseline Clinical details");
		baselineData.clickBaselineDetailsSidebarTab();
		if (!baselineData.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load baseline clinical data page");
		}
		baselineData.clickEditButton();
		baselineData.editBaselineClinicalDetails();
		baselineData.clickSaveButton();
		report.logStepWithScreenshot(ReportLevel.INFO, "Edited Baseline Clinical Details details");

		report.logStep(ReportLevel.INFO, "Back to Patient List Page");
		navigationBar.viewPatientList();
		if (!patientListPage.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load patient list page");
		}

		report.assertWithScreenshot(
				patientListPage.searchPatientBy(patient.getDeviceSerialNum(), PatientSearchType.DEVICE), true,
				"Found patient with device serial: " + patient.getDeviceSerialNum(),
				"Failed to located patient with device serial: " + patient.getDeviceSerialNum());

		patientListPage.viewPatient();
		report.assertWithScreenshot(patientAndDeviceData.validate(), true,
				"Succesfully navigated to patient profile page", "Failed to navigate to patient profile page");

		report.logStep(ReportLevel.INFO, "Verifying the details after editing the data");
		// Verification of Data Changed after editing the details
		report.assertWithScreenshot(AssertionLevel.SOFT,
				patientAndDeviceData.verifyPatientNameText(newPatient.getFirstName()), true,
				"Succesfully changed patient name from " + patient.getFirstName() + " to " + newPatient.getFirstName(),
				"Failed to change patient name");

		transmitter.clickTransmitterSidebarOption();
		if (!transmitter.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load Transmitter profile page");
		}
		report.assertWithScreenshot(AssertionLevel.SOFT,
				transmitter.verifyTransmissionSerialNum(newPatient.getTransmitterSerialNum()), true,
				"Succesfully changed transmitter serial number from " + patient.getTransmitterSerialNum() + " to "
						+ newPatient.getTransmitterSerialNum(),
				"Failed to change transmitter serial number from " + patient.getTransmitterSerialNum() + " to "
						+ newPatient.getTransmitterSerialNum());

		followUp.isFollowUpPageLoaded();
		report.assertWithScreenshot(AssertionLevel.SOFT, followUp.verifyScheduleChange(ScheduleType.MANUAL), true,
				"Successfully changed from Smart schedule to Manual schedule",
				"Failed to make the changes on Follow Up Page");

		directAlerts.isDirectAlertsPageLoaded();
		report.assertWithScreenshot(AssertionLevel.SOFT,
				verifyDirectAlerts(PageSelection.DIRECT_ALERTS_PROFILE, webDriver, AlertClassification.YELLOW,
						DirectAlerts.TACHY_THERAPY_DISABLED, DirectAlerts.DEVICE_PROGRAMMED_TO_EMERGENCY_PACING_VALUES,
						DirectAlerts.CHARGE_TIME_LIMIT_REACHED),
				true, "Successfully made changes in direct alerts notifications page",
				"Failed to make the changes on Direct Alerts notifications page");

	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		loginUtils.logout();
		if(!manualCustomerSetup) {
			commonDBUtils.updateMFA(customerInfo.getMainContact_EmailID());
		}
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.PATIENT_PROFILE_SERVICE);
		}
		super.cleanup(result);
	}
}