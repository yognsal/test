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
import com.mnet.pojo.customer.Customer;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicHeader;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;

/**
 * 
 * Sanity testcase to verify First Time login functionality
 * 
 * @author NAIKKX12
 *
 */
public class FirstTimeLoginTest extends MITETest implements GraylogReporting {

	private ClinicHeader header;
	private Customer customerInfo;
	private CustomerUtilities customerUtils;
	private LoginUtilities loginUtils;
	private CommonDBUtilities commonDBUtils;

	/**
	 * Define all functionality to be used by the test class. Test-agnostic setup is
	 * performed by MITETest after calling super.initialize().
	 */
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.DATABASE);
		super.initialize(context);

		// Initialize test-specific page objects
		header = new ClinicHeader(webDriver, report);
		customerUtils = new CustomerUtilities(report, webDriver);
		loginUtils = new LoginUtilities(report, email, webDriver);
		commonDBUtils = new CommonDBUtilities(report, database); 
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);

	}

	@Test
	public void firstTimeLoginTest() {

		customerInfo = new Customer();
		customerInfo = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
				: customerUtils.automatedCustomerSetupAndLogin(customerInfo, email, true);
		if (customerInfo == null) {
			report.logStep(ReportLevel.FAIL, "Failed to authorize application");
		}
		
		report.assertWithScreenshot(header.validate(), true,
				"First time login is complete and user will be able to access the application",
				"First time login failed");

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
