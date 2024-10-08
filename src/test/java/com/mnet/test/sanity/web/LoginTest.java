package com.mnet.test.sanity.web;

import java.util.List;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.AssertionLevel;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.reporting.TestStep.ScreenshotType;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientFilterType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientSearchType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientStatusFilterType;
import com.mnet.webapp.pageobjects.patient.PatientProfileAndDeviceData;
import com.mnet.webapp.utilities.LoginUtilities;

/**
 * Sample web test demonstrating login and page navigation behavior.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class LoginTest extends MITETest implements GraylogReporting {
	
	ClinicNavigationBar navigationBar;
	PatientListPage patientListPage;
	PatientProfileAndDeviceData patientAndDeviceData;
	
	LoginUtilities loginHandler;
	
	/**
	 * Define all functionality to be used by the test class, including test attributes, page objects, and test-level utility classes.
	 * Test-agnostic setup is performed by MITETest after calling super.initialize().
	 */
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		// Initialize framework-level utilities
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		/* shortTestName can be optionally instantiated before super.initialize - this is used to create the target directory.
		 * If test case name is longer than SHORT_TEST_NAME_MAX_LENGTH in application properties, it will be truncated (so as not to exceed max system path length)*/
		shortTestName = "MyLoginTest";
		/* Directory relative to DATA_PATH (/src/test/resources/data) where test data sheet is located.
		 * If no path is provided, DATA_PATH is used for the test data path.*/
		relativeDataDirectory = "sanity/web";
		// Call only after MITETest data members / attributes have been initialized.
		super.initialize(context);
		
		// Initialize test-specific page objects
		navigationBar = new ClinicNavigationBar(webDriver, report);
		patientListPage = new PatientListPage(webDriver, report);
		patientAndDeviceData = new PatientProfileAndDeviceData(webDriver, report);
		
		// Initialize test-level utilities
		loginHandler = new LoginUtilities(report, email, webDriver);
	}

	/*
	 * Data provider fetches from an Excel sheet with the class name (LoginTest.xlsx) at src/test/resources.
	 * Each parameter corresponds to a column in the data sheet (except the first column, which defines whether to use a data row or not.
	 * Every data row marked with "y" in the first column is run as an iteration of this test.
	 */
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void loginTest(String username, String password, String deviceSerial) {
		/* 
		 * Test steps can be marked as PASS, FAIL, INFO, or ERROR. FAIL or hard assert will cause the test to terminate immediately.
		 * Test steps use ReportLevel.INFO and ScreenshotType.NONE by default.
		 * 
		 * Asserts are AssertionLevel.HARD by default and mark the test step as PASS or FAIL only.
		 * Assertions define the ReportLevel based on the assertion result and don't need to be passed a ReportLevel.
		 * If a failure message is not explictly defined, it will default to the pass message with "FAIL: " prepended
		 * 
		 * For webapp tests (TestAttribute.WEBAPP), test steps and assertions can be logged with or without screenshots.
		 * For conditions that would cause a test failure but should allow continuation of the remaining test steps, pass AssertionLevel.SOFT explicitly.
		 * 
		 * Test report is saved in C://TestReports (or equivalent path configured in src/test/resources/*.properties (property file used is defined in environment.properties).
		 */
		report.logStep(TestStep.builder()
				.reportLevel(ReportLevel.PASS)
				.message("This is a sample test step with multiple requirements tagged")
				.tags(List.of("Auto7791", "Auto7792"))
				.build());
		
		report.assertCondition(loginHandler.login(username, password), true, 
				TestStep.builder()
				.message("Succesfully logged into clinic '" + username + "'")
				.failMessage("Failed to login to clinic with credentials - username: " + username + " password: " + password)
				.build());
		
		report.logStep(TestStep.builder().message("Navigating to patient list page...").build());
		navigationBar.viewPatientList();
		
		report.assertCondition(patientListPage.validate(), true, 
				TestStep.builder().screenshotType(ScreenshotType.VIEWPORT)
				.message("Successfully navigated to patient list page")
				.failMessage("Failed to validate patient list page - missing page element: " + patientListPage.getPageValidationElement()).build());
		
		report.logStep(TestStep.builder().message("Filtering patients by status...").build());
		patientListPage.filterPatientByStatus(PatientStatusFilterType.ACTIVE_CLINIC_PATIENTS);
		report.logStep(TestStep.builder()
				.reportLevel(ReportLevel.PASS)
				.message("Changed filter to 'Active Clinic Patients' on patient list page")
				.build());
		
		report.logStep(TestStep.builder().message("Filtering patients by attribute...").build());
		patientListPage.filterPatientByColumn(PatientFilterType.ALL);
		report.logStep(TestStep.builder()
				.reportLevel(ReportLevel.PASS)
				.screenshotType(ScreenshotType.VIEWPORT)
				.message("Changed filter to 'All' on patient list page")
				.build());
		
		report.logStep(TestStep.builder().message("Searching for patient...").build());
		
		report.assertCondition(patientListPage.searchPatientBy(deviceSerial, PatientSearchType.DEVICE), true,
				TestStep.builder()
				.screenshotType(ScreenshotType.VIEWPORT)
				.message("Found patient with device serial: " + deviceSerial)
				.failMessage("Failed to locate patient with device serial: " + deviceSerial)
				.build());
		
		report.logStep(TestStep.builder().message("Navigating to patient profile page...").build());
		patientListPage.viewPatient();
		
		// AssertionLevel.SOFT can be used to assert a condition without failing the test immediately (test instead fails at the end of execution / on hard assert failure).
		report.assertCondition(patientAndDeviceData.validate(), true, 
				TestStep.builder()
				.assertionLevel(AssertionLevel.SOFT)
				.screenshotType(ScreenshotType.SCROLLING)
				.message("Succesfully navigated to patient profile page")
				.failMessage("Failed to validate patient profile page")
				.build());

		report.logStep(TestStep.builder().message("Logging out...").build());
		
		report.assertCondition(loginHandler.logout(), true, 
				TestStep.builder()
				.screenshotType(ScreenshotType.VIEWPORT)
				.message("Successfully logged out of clinic '" + username + "'")
				.failMessage("Failed to validate logout from clinic")
				.build());
	}
	
	/*
	 * Implement the GraylogReporting interface if microservice logs need to be fetched from Graylog after each test iteration.
	 * fetchGraylogReports() can leverage the default implementation - call this either on test failure or on every test iteration in cleanup(ITestResult), depending on the needs of the test.
	 */
	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, 
					Microservice.PATIENT_PROFILE_SERVICE, Microservice.PATIENT_MGMT_INTG_SERVICE);
		}
		
		super.cleanup(result);
	}
}
