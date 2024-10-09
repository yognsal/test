package com.mnet.test.smoke.web;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.customer.Customer.CustomerType;
import com.mnet.webapp.utilities.FunctionalTestCustomerUtility;

public class CreateCustomer extends MITETest {

	private FunctionalTestCustomerUtility functionalTestCustomerUtility;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		super.initialize(context);

		functionalTestCustomerUtility = new FunctionalTestCustomerUtility(report, webDriver);
	}

	@Test
	/**
	 * This test will run without VPN connection.
	 * 
	 * @implNote make sure your email id and email id+1 (eg user+1@abbott.com) is
	 *           currently not in use by any other clinic.
	 */
	public void createCustomer() {

		boolean result = true;
		Customer directCustomer = new Customer();
		directCustomer.setAddAllDevices(true);
		if (!functionalTestCustomerUtility.addCustomer(log, directCustomer, "Smoke_Direct_Customer")) {
			report.logStepWithScreenshot(ReportLevel.ERROR, "Failed to create Direct Customer");
			result = false;
		} else {
			report.logStep(ReportLevel.INFO, "Customer User ID: " + directCustomer.getMainContact_UserID());
			report.logStep(ReportLevel.INFO, "Customer Password: " + directCustomer.getMainContact_password());
		}

		Customer spCustomer = new Customer();
		spCustomer.setMainContact_EmailID(CommonUtils.generateValidEmail(1));
		spCustomer.setCustomerType(CustomerType.SERVICE_PROVIDER);
		spCustomer.setAddAllDevices(true);
		if (!functionalTestCustomerUtility.addCustomer(log, spCustomer, "Smoke_SP_Customer")) {
			report.logStepWithScreenshot(ReportLevel.ERROR, "Failed to create SP Customer");
			result = false;
		} else {
			report.logStep(ReportLevel.INFO, "Customer User ID: " + spCustomer.getMainContact_UserID());
			report.logStep(ReportLevel.INFO, "Customer Password: " + spCustomer.getMainContact_password());
		}

		if (!result) {
			report.logStep(ReportLevel.FAIL, "Failed to setup customers for smoke");
		}
	}

}
