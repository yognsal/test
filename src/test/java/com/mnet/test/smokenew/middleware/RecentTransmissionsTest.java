package com.mnet.test.smoke.middleware;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.AssertionLevel;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.middleware.utilities.TransmissionUtilities;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.patient.Patient;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicAdminNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.DirectAlertMerlinAtHomePage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientFilterType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientSearchType;
import com.mnet.webapp.pageobjects.clinic.recenttransmissions.RecentTransmissionsPage;
import com.mnet.webapp.pageobjects.patient.PatientProfileAndDeviceData;
import com.mnet.webapp.pageobjects.patient.transmissions.AllTransmissionsPage;
import com.mnet.webapp.pageobjects.patient.transmissions.TransmissionsPage;
import com.mnet.webapp.pageobjects.patient.transmissions.TransmissionsPage.ReportName;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;
import com.mnet.webapp.utilities.PatientUtilities;

public class RecentTransmissionsTest extends MITETest implements GraylogReporting {

	private ClinicNavigationBar navigationBar;
	private ClinicAdminNavigationBar clinicAdminNavigationBar;
	private DirectAlertMerlinAtHomePage merlinAtHome;
	private PatientListPage patientListPage;
	private LoginUtilities loginUtils;
	private RecentTransmissionsPage recentTransmission;
	private TransmissionsPage transmission;
	private TransmissionUtilities transmissionUtil;
	private PatientProfileAndDeviceData patientAndDeviceData;
	private AllTransmissionsPage allTransmissionsPage;
	private String TRANSMISSION_PAYLOAD_PATH_UNITY;
	private PatientDBUtilities patientDB;
	private Customer customerInfo;
	private Patient patient1;
	private CustomerUtilities customerUtils;
	private PatientUtilities patientUtils;
	private CommonDBUtilities commonDBUtils;

	// Constant values
	private static final String useDatabase = "";
	private static final String driverType = "";
	private static final String transmissionType = "FUA";

	@Override
	@BeforeTest
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.REMOTE_MACHINE);
		attributes.add(TestAttribute.DATABASE);
		relativeDataDirectory = "smoke";
		super.initialize(context);

		// Initialize test-specific page objects
		navigationBar = new ClinicNavigationBar(webDriver, report);
		patientListPage = new PatientListPage(webDriver, report);
		recentTransmission = new RecentTransmissionsPage(webDriver, report);
		transmissionUtil = new TransmissionUtilities(log, database, report, fileManager, remoteMachine);
		loginUtils = new LoginUtilities(report, email, webDriver);
		transmission = new TransmissionsPage(webDriver, report, log);
		patientAndDeviceData = new PatientProfileAndDeviceData(webDriver, report);
		allTransmissionsPage = new AllTransmissionsPage(webDriver, report);
		patientDB = new PatientDBUtilities(report, database);
		clinicAdminNavigationBar = new ClinicAdminNavigationBar(webDriver, report);
		merlinAtHome = new DirectAlertMerlinAtHomePage(webDriver, report);
		customerUtils = new CustomerUtilities(report, webDriver);
		patientUtils = new PatientUtilities(report, webDriver);
		commonDBUtils = new CommonDBUtilities(report, database);
		TRANSMISSION_PAYLOAD_PATH_UNITY = FrameworkProperties
				.getProperty("TRANSMISSION_PAYLOAD_PATH_UNITY");
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);
		report.logStep(ReportLevel.INFO, "Setting Up for the test...");

		customerInfo = new Customer();
		customerInfo = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
				: customerUtils.automatedCustomerSetupAndLogin(customerInfo, email, true);
		if(customerInfo == null) {
			report.logStep(ReportLevel.FAIL, "Failed to authorize application");
		}

		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar post refresh");
		}

		navigationBar.viewClinicAdministrationPage();
		report.assertWithScreenshot(clinicAdminNavigationBar.validate(), true,
				"Navigated and loaded Clinic Administration webpage succesfully.",
				"Failed to navigate and load Clinic Administration page");
		clinicAdminNavigationBar.viewClinicHomeTransmitterPage();
		report.assertWithScreenshot(merlinAtHome.validate(), true,
				"Navigated and loaded Merlin At Home Page succesfully.",
				"Failed to navigate and load Merlin At Home Page");

		if (merlinAtHome.editHomeTrasmitter(true)) {
			merlinAtHome.selectEmailForRedAlerts();
			merlinAtHome.saveHomeTrasmitter();
			merlinAtHome.waitForConfirmPopup();
			merlinAtHome.confirmSettingChanges();
			merlinAtHome.waitForConfirmPopup();
			merlinAtHome.informSettingChanges();
		}

		if (!loginUtils.logout()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to logout");
		}

	}

	@Test
	public void recentTransmissionTest() {

		if (!loginUtils.login(customerInfo.getMainContact_UserID(), customerInfo.getMainContact_password())) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to login");
		}
		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}

		patient1 = new Patient();
		if (!patientUtils.EnrollPatient(patient1)) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to enroll patient for transmission");
		}
		int emailCount = email.getEmailCount(FrameworkProperties.getProperty("TRANSMISSION_MAIL_IDENTIFIER"));

		report.assertCondition(AssertionLevel.SOFT, transmissionUtil.performTransmissionForPatient(patient1, customerInfo, useDatabase, driverType,
						transmissionType, patient1.getTransmitterModel().getTransModel().split("-")[0]),
				true, "Tranmission Processed by ETL and is in the DB", "Transmission was not processed by ETL and is hence not reflecting in the DB");

		// Taking the latest payload zip generated and fetching the date and time from
		// them.
		String[] deviceInfo = transmissionUtil.deviceInfo(TRANSMISSION_PAYLOAD_PATH_UNITY);
		String dateSet = deviceInfo[2];
		String timeSet = deviceInfo[3];
		String updatedDate = DateUtility.changeDateFormat(dateSet, null, null);
		String updatedTime = DateUtility.changeTimeFormat(timeSet);
		log.info("updatedTime: " + updatedTime);
		log.info("updatedDate: " + updatedDate);
		

		report.assertCondition(AssertionLevel.SOFT,
				transmissionUtil.transmissionMailVerification(patient1.getDeviceSerialNum(),
						patient1.getDeviceModelNum(), updatedDate, updatedTime, emailCount),
				true,
				"Transmission Mail Received.\nFollowing Details were verified:\n" + "Device Serial - "
						+ patient1.getDeviceSerialNum() + "\nDevice Model - " + patient1.getDeviceModelNum()
						+ "\nTransmission Date - " + updatedDate + "\nUpdated Time - " + updatedTime,
				"Transmission mail Not Received");

		// Static wait for database to update after receiving transmission mail
		webDriver.staticWait(webDriver.minBrowserTimeout() * 10);

		String patientId = patientDB.getPatient("first_name", "last_name", patient1.getFirstName(), patient1.getLastName()).get("patient_id"); 
		report.assertCondition(AssertionLevel.SOFT,
				patientDB.verifyTransmissionMailReceived(patientId), true,
				"Transmission Mail Received Successfully", "Transmission Mail Not Received");

		report.logStep(ReportLevel.INFO, "Back to Patient List Page");
		navigationBar.viewPatientList();

		patientListPage.filterPatientByColumn(PatientFilterType.ALL);
		report.logStepWithScreenshot(ReportLevel.PASS, "Changed filter to 'All' on patient list page");

		report.logStep(ReportLevel.INFO, "Searching for patient...");

		report.assertWithScreenshot(
				patientListPage.searchPatientBy(patient1.getDeviceSerialNum(), PatientSearchType.DEVICE), true,
				"Found patient with device serial: " + patient1.getDeviceSerialNum(),
				"Failed to located patient with device serial: " + patient1.getDeviceSerialNum());

		patientListPage.viewPatient();
		report.assertWithScreenshot(patientAndDeviceData.validate(), true,
				"Succesfully navigated to patient profile page", "Failed to navigate to patient profile page");

		if(!allTransmissionsPage.selectTransmissionByDate(updatedDate)) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Problem loading All Transmissions page");
		}

		report.assertWithScreenshot(transmission.validate(), true,
				"Successfully navigated to transmission page for Device Serial: " + patient1.getDeviceSerialNum(),
				"Failed to navigate to transmission page for Device Serial: " + patient1.getDeviceSerialNum());

		transmission.reportSelectionAndVerification(ReportName.FASTPATH_SUMMARY);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for FastPath Summary",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.EPISODES_SUMMARY);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Episodes Summary",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.DIAGNOSTICS_SUMMARY);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Diagnostics Summary",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.ALERT_SUMMARY);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Alert Summary",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.EPISODES_EGM);
		report.logStepWithScreenshot(ReportLevel.INFO,
				"Successfully Navigated to Episodes and EGM overlay and verified presence of PDF");

		transmission.reportSelectionAndVerification(ReportName.EXTENDED_EPISODES);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Extended Episodes",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.EXTENDED_DIAGNOSTICS);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Extended Diagnostics",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.HEART_IN_FOCUS);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Heart In Focus",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.CORVUE);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Corvue",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.DIRECT_TREND_REPORT);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Direct Trend Report",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.TEST_RESULTS);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Test Results",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.PARAMETER);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Parameter",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.WRAP_UP_OVERVIEW);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated to iFrame and 'Download' button is clickable confirming the presence of PDF Report for Wrap-Up Overview",
				"Failed to verify presence of PDF Report");
		transmission.switchToParentFrame();

		transmission.reportSelectionAndVerification(ReportName.TEXT_REPORT);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyTextReportHeading(), true,
				"Successfully Navigated to Text Alert Summary Container",
				"Failed to verify presence of Text Alert Summary Container");
		transmission.closeOverlay();

		transmission.reportSelectionAndVerification(ReportName.MERGED_REPORT);
		report.assertWithScreenshot(AssertionLevel.SOFT, transmission.verifyDownloadButtonAvailability(), true,
				"Successfully Navigated Merged Report Container and verified that Download is enabled",
				"Failed to verify presenceMerged Report");
		transmission.switchToParentFrame();
		transmission.clickCancelButton();

		report.logStep(ReportLevel.INFO, "Navigating to Recent Transmissions Tab");
		navigationBar.viewRecentTransmissions();
		if (!recentTransmission.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load Recent transmissions Page");
		}
		report.logStep(ReportLevel.INFO, "Verifying presence of transmission on Recent Transmissions Tab");
		report.assertWithScreenshot(
				recentTransmission.verifyTransmissionPresentInRTTab(updatedDate + ", " + updatedTime), true,
				"Transmission Present in Recent Transmissions Tab",
				"Unable to view Transmission in Recent Transmissions Tab");

	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		report.logStep(ReportLevel.INFO, "Logging out...");
		loginUtils.logout();
		if(!manualCustomerSetup) {
			commonDBUtils.updateMFA(customerInfo.getMainContact_EmailID());
		}
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.TRANSMISSION_ROUTING_SERVICE);
		}

		super.cleanup(result);
	}

}