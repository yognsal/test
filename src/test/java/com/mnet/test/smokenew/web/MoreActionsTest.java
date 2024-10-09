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
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.recenttransmissions.RecentTransmissionsPage;
import com.mnet.webapp.pageobjects.clinic.recenttransmissions.RecentTransmissionsPage.columnSelection;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;

public class MoreActionsTest extends MITETest implements GraylogReporting {

	private ClinicNavigationBar navigationBar;
	private LoginUtilities loginUtils;
	private RecentTransmissionsPage recentTransmission;
	private Customer customerInfo;
	private CustomerUtilities customerUtils;
	private CommonDBUtilities commonDBUtils;

	@Override
	@BeforeTest
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.DATABASE);
		super.initialize(context);

		// Initialize test-specific page objects
		navigationBar = new ClinicNavigationBar(webDriver, report);
		recentTransmission = new RecentTransmissionsPage(webDriver, report);
		loginUtils = new LoginUtilities(report, email, webDriver);
		customerUtils = new CustomerUtilities(report, webDriver);
		commonDBUtils = new CommonDBUtilities(report, database);
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
	}

	@Test
	public void moreActionsTest() {

		report.logStep(ReportLevel.INFO, "Setting up the test");
		if (!navigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}
		navigationBar.viewRecentTransmissions();
		if (!recentTransmission.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to Recent Transmissions Page");
		}

		recentTransmission.openAddRemoveColumnBox();
		if (!recentTransmission.validateAddRemoveColumnBox()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to open Add / Remove Column box");
		}
		recentTransmission.testSetup();

		recentTransmission.openAddRemoveColumnBox();
		if (!recentTransmission.validateAddRemoveColumnBox()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to open Add / Remove Column box");
		}
		recentTransmission.selectColumns(true, columnSelection.LOCATION, columnSelection.LATEST_COMMENTS);
		recentTransmission.clickDonebutton();
		if (!recentTransmission.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to Recent transmission page post selecting columns");
		}

		report.assertWithScreenshot(recentTransmission.verifyColumnSelection(), true,
				"Location and Latest Comments columns are selected and added in Recent Transmssions Table",
				"Location and Latest Comments columns are not selected from more action -> add or remove columns");

		recentTransmission.openAddRemoveColumnBox();
		if (!recentTransmission.validateAddRemoveColumnBox()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to open Add / Remove Column box");
		}
		report.assertWithScreenshot(recentTransmission.verifyColumnSelection(), true,
				"Location and Latest Comments Selected", "Location and Latest Comments Not Selected");

		report.logStep(ReportLevel.INFO, "De-selecting Location and Latest Comments Columns");
		recentTransmission.selectColumns(false, columnSelection.LOCATION, columnSelection.LATEST_COMMENTS);
		recentTransmission.clickDonebutton();
		if (!recentTransmission.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL,
					"Failed to Recent transmission page post de-selecting columns");
		}
		report.assertWithScreenshot(recentTransmission.verifyColumnDeSelection(), true,
				"Location and Latest Comments columns are De-Selected and not present in Recent Transmssions Table",
				"Location and Latest Comments columns are still Present in Recent transmissions table");

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
