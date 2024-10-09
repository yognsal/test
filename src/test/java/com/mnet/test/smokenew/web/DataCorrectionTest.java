package com.mnet.test.smoke.web;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.patient.Patient;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.patient.DataCorrectionPage;
import com.mnet.webapp.pageobjects.patient.PatientProfileAndDeviceData;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;
import com.mnet.webapp.utilities.PatientUtilities;

/**
 * Web test demonstrating Data Correction Functionality.
 * 
 * @version March 2023
 * @author Rishab Kotwal
 */

public class DataCorrectionTest extends MITETest implements GraylogReporting {
	private ClinicNavigationBar navigationBar;
	private PatientListPage patientListPage;
	private DataCorrectionPage dataCorrection;
	private LoginUtilities loginUtils;
	private PatientProfileAndDeviceData patientAndDeviceData;
	private Customer customerInfo;
	private Patient patient;
	private CommonDBUtilities commonDBUtils;
	private CustomerUtilities customerUtils;
	private PatientUtilities patientUtils;

	@Override
	@BeforeTest
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.DATABASE);
		super.initialize(context);

		// Initialize test-specific page objects
		navigationBar = new ClinicNavigationBar(webDriver, report);
		patientListPage = new PatientListPage(webDriver, report);
		dataCorrection = new DataCorrectionPage(webDriver, report);
		loginUtils = new LoginUtilities(report, email, webDriver);
		patientAndDeviceData = new PatientProfileAndDeviceData(webDriver, report, database);
		commonDBUtils = new CommonDBUtilities(report, database);
		customerUtils = new CustomerUtilities(report, webDriver);
		patientUtils = new PatientUtilities(report, webDriver);
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);
		
		customerInfo = new Customer();
		Customer customer = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
				: customerUtils.automatedCustomerSetupAndLogin(customerInfo, email, true);
		if(customer == null) {
			report.logStep(ReportLevel.FAIL, "Failed to authorize application");
		}

		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}

		patient = new Patient();
		if (!patientUtils.EnrollPatient(patient)) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to create patient during setup");
		}
	}

	@Test
	public void dataCorrectionTest() {
		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}

		patientListPage.viewPatient();
		report.assertWithScreenshot(patientAndDeviceData.validate(), true,
				"Succesfully navigated to patient profile page of: " + patient.getLastName(),
				"Failed to navigate to patient profile page");
		patientAndDeviceData.clickEditButton();
		if (!patientAndDeviceData.isPageInEditMode()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load page in Edit mode");
		}
		Patient newPatient = new Patient();
		patientAndDeviceData.editDeviceData(newPatient);
		patientAndDeviceData.clickSaveButton();
		dataCorrection.dataCorrection();
		report.logStepWithScreenshot(ReportLevel.INFO,
				"Data Correction Pop-Up after changing the serial number / Model of the device");
		dataCorrection.clickSave();
		report.assertWithScreenshot(patientAndDeviceData.isPageSaved(), true, "Saved changes successfully",
				"Failed to save changes");
		Patient outputPatient = patientAndDeviceData.getDeviceData(true);
		report.assertWithScreenshot(outputPatient.getDeviceSerialNum(), newPatient.getDeviceSerialNum(),
				"Succesfully verified the changes: Device Serial Number changed from " + patient.getDeviceSerialNum()
						+ " to " + outputPatient.getDeviceSerialNum(),
				"Failed to save updated changed");

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
