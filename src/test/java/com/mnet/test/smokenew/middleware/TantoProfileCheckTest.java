package com.mnet.test.smoke.middleware;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.middleware.UnixConnector;
import com.mnet.framework.reporting.TestReporter.AssertionLevel;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.XMLData;
import com.mnet.middleware.utilities.TantoProfilingUtility;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.patient.Patient;
import com.mnet.pojo.patient.Patient.DeviceType;
import com.mnet.pojo.tanto.Tanto;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicHeader;
import com.mnet.webapp.pageobjects.clinic.ClinicLoginPage;
import com.mnet.webapp.pageobjects.clinic.ClinicLogoutPage;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.ColumnCheckboxes;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientFilterType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientSearchType;
import com.mnet.webapp.pageobjects.patient.PatientProfileAndDeviceData;
import com.mnet.webapp.pageobjects.patient.TransmitterProfilePage;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;
import com.mnet.webapp.utilities.PatientUtilities;

public class TantoProfileCheckTest extends MITETest implements GraylogReporting {

	ClinicLoginPage loginPage;
	ClinicNavigationBar navigationBar;
	ClinicHeader header;
	PatientListPage patientListPage;
	PatientProfileAndDeviceData patientData;
	ClinicLogoutPage logoutPage;
	LoginUtilities loginUtils;
	TransmitterProfilePage transmitter;
	private Patient patient;
	private TantoProfilingUtility tantoUtility;
	private boolean setupComplete = false;
	private Customer customerInfo;
	private CustomerUtilities customerUtils;
	private CommonDBUtilities commonDBUtils;
	private int iteration = 0;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.DATABASE);
		relativeDataDirectory = "smoke";
		super.initialize(context);

		// Initialize test-specific page objects
		loginPage = new ClinicLoginPage(webDriver, report);
		navigationBar = new ClinicNavigationBar(webDriver, report);
		header = new ClinicHeader(webDriver, report);
		patientListPage = new PatientListPage(webDriver, report);
		logoutPage = new ClinicLogoutPage(webDriver);
		loginUtils = new LoginUtilities(report, email, webDriver);
		transmitter = new TransmitterProfilePage(webDriver, report);
		patientData = new PatientProfileAndDeviceData(webDriver, report, database);
		commonDBUtils = new CommonDBUtilities(report, database);
		customerUtils = new CustomerUtilities(report, webDriver);
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		if(iteration % 3 == 0) {
			remoteMachine = new UnixConnector(log);
			tantoUtility = new TantoProfilingUtility(log, remoteMachine, fileManager, report);
		}
		super.setup(parameters);
		
		if (!setupComplete) {
			customerInfo = new Customer();
			Customer customer = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
					: customerUtils.automatedCustomerSetupAndLogin(customerInfo, email, true);
			if(customer == null) {
				report.logStep(ReportLevel.FAIL, "Failed to authorize application");
			}

			if (!navigationBar.validate()) {
				report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
			}
			navigationBar.viewPatientList();
			if (!patientListPage.validate()) {
				report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load patient list page");
			}

			patientListPage.showHideColumn(true, ColumnCheckboxes.CONNECTIVITY);
			setupComplete = true;
		}
	}

	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void tantoProfileCheckTest(String driverType, String profileType, String transmitterSWVersion,
			String transmitterModelExtension) {
		report.logStep(ReportLevel.INFO, "Executing test for following profiling configuration - Driver Type: "
				+ driverType + " and ProfileType: " + profileType);

		report.logStep(ReportLevel.INFO, "Enrolling Patient");
		PatientUtilities setupUtils = new PatientUtilities(report, webDriver);
		patient = new Patient();
		report.assertWithScreenshot(setupUtils.EnrollPatient(patient), true, "Patient enrollment successful",
				"Failed to enroll patient");

		Tanto tanto = new Tanto();
		tanto.setDriverType(driverType);
		tanto.setProfileType(profileType);
		tanto.setTransmitterSWVersion(transmitterSWVersion);
		tanto.setTransmitterModelExtension(transmitterModelExtension);

		XMLData profileResponse = tantoUtility.tantoProfileResponse(patient, tanto);

		if (profileResponse.isFailure()) {
			report.logStep(ReportLevel.FAIL, "Failed to perform profiling");
		}

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
		report.assertWithScreenshot(AssertionLevel.SOFT, patientListPage.IsPatientPaired(), true,
				"Green Tick Icon Under Connectivity Column Present.",
				"Green Tick Icon Under Connectivity Column NOT Present.");

		report.logStep(ReportLevel.INFO, "Viewing the patient...");
		patientListPage.viewPatient();
		report.assertWithScreenshot(patientData.validate(), true, "Succesfully navigated to patient profile page",
				"Failed to navigate to patient profile page");

		transmitter.clickTransmitterSidebarOption();
		if(!transmitter.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load transmitter profile page");
		}
		report.assertWithScreenshot(AssertionLevel.SOFT, transmitter.verifyLastTransmissionDate(DeviceType.Unity), true,
				"Profiling Successful", "Profiling not successful");
		PatientDBUtilities databaseUtils = new PatientDBUtilities(report, database);
		report.assertCondition(AssertionLevel.SOFT, databaseUtils.verifyPairedFlag(patient.getTransmitterSerialNum()),
				true, "Pairing successful and paired flag set to true in database",
				"Pairing un-successful and paired flag is false in database");

	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		if(iteration % 3 == 2) {
			remoteMachine.closeConnection();
		}
		iteration++;
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.TANTO_ROUTING_SERVICE,
					Microservice.TANTO_PATIENT_PROFILE_SERVICE, Microservice.TANTO_COMM_PROFILE_SERVICE);
		}

		super.cleanup(result);
	}

	@Override
	@AfterTest
	public void reportAndTeardown(ITestContext context) {
		report.logStep(ReportLevel.INFO, "Logging out...");
		loginUtils.logout();
		if(!manualCustomerSetup) {
			commonDBUtils.updateMFA(customerInfo.getMainContact_EmailID());
		}
		super.reportAndTeardown(context);
	}

}
