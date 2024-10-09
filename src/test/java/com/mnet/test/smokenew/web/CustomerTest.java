package com.mnet.test.smoke.web;

import java.util.ArrayList;
import java.util.List;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CustomerDBUtilities;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.AssertionLevel;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.pojo.customer.Customer;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.customer.AllCustomerPage;
import com.mnet.webapp.pageobjects.customer.AllCustomerPage.CustomerSearchType;
import com.mnet.webapp.pageobjects.customer.CustomerNavigationBar;
import com.mnet.webapp.pageobjects.customer.CustomerProfilePage;
import com.mnet.webapp.pageobjects.customer.NewCustomerPage;
import com.mnet.webapp.pageobjects.customer.TechSupportPage;

/**
 * Sanity test to create & edit customer and verify the profile
 * 
 * @author NAIKKX12
 *
 */
public class CustomerTest extends MITETest implements GraylogReporting {

	NewCustomerPage newCustomerPage;
	TechSupportPage techSupportPage;
	CustomerNavigationBar customerNavigationBar;
	AllCustomerPage allCustomerPage;
	CustomerProfilePage customerProfilePage;
	private String revisedTimeZone = "(GMT) Lisbon";

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.DATABASE);
		super.initialize(context);

		// Initialize test-specific page objects
		customerNavigationBar = new CustomerNavigationBar(webDriver, report);
		allCustomerPage = new AllCustomerPage(webDriver, report);
		newCustomerPage = new NewCustomerPage(webDriver, report);
		techSupportPage = new TechSupportPage(webDriver);
		customerProfilePage = new CustomerProfilePage(webDriver, report);
	}

	@Test
	public void customerTest() {
		report.assertWithScreenshot(techSupportPage.validate(), true, "Loaded Merlin.net PCN - Tech support page succesfully",
				"Failed to navigate & load Merlin.net PCN - Tech support page");
		// Navigate to all abbott cusotmer page and add customer
		customerNavigationBar.viewAllAbbottCustomerPage();
		allCustomerPage.waitForAllCustomerPageLoading();
		report.assertWithScreenshot(allCustomerPage.validate(), true, "Loaded All Abbott Customer page succesfully",
				"Failed to navigate & load All Abbott Customer page");
		Customer customer = new Customer();
		
		allCustomerPage.navigateToAddCustomerPage();
		report.assertWithScreenshot(newCustomerPage.validate(), true, "Loaded new customer creation page succesfully",
				"Failed to navigate & load Add Customer Page");
		// Add customer
		customer.setRegisteredEmailID(CommonUtils.generateRandomEmail());
		customer.setMainContact_EmailID(customer.getRegisteredEmailID());
		newCustomerPage.setCustomerHeadquarters(customer.getCustomerName(), customer.getCustomerType());
		report.logStepWithScreenshot(ReportLevel.INFO, "Set customer name: " + customer.getCustomerName()
				+ " & customertype: " + customer.getCustomerType().getCustType());
		List<String> address = new ArrayList<String>();
		address.add(customer.getPrimaryAddress());
		newCustomerPage.setClinicAddress(address, customer.getCountry(), null, null, null);
		report.logStepWithScreenshot(ReportLevel.INFO, "Set primary address: " + customer.getPrimaryAddress()
				+ " & country: " + customer.getCountry());
		newCustomerPage.setMainPhone(null, null, customer.getMainPhone());
		report.logStepWithScreenshot(ReportLevel.INFO, "Set main phone: " + customer.getMainPhone());
		newCustomerPage.setEmail(null, customer.getRegisteredEmailID());
		report.logStepWithScreenshot(ReportLevel.INFO, "Set email id: " + customer.getRegisteredEmailID());
		newCustomerPage.setLocale(customer.getTimeZone(), customer.getLegalJuridiction(),
				customer.getClinicLanguage());
		report.logStepWithScreenshot(ReportLevel.INFO,
				"Set timezone: " + customer.getPrimaryAddress() + ", juridiction: "
						+ customer.getLegalJuridiction() + "  & language: " + customer.getClinicLanguage());
		newCustomerPage.setClinicMainContact(customer.getMainContact_UserID(),
				customer.getMainContact_FirstName(), null, customer.getMainContact_LastName(),
				customer.getMainContact_password(), customer.getMainContact_confirmPwd(), null,
				customer.getMainContact_EmailID());
		report.logStepWithScreenshot(ReportLevel.INFO, "Set user ID: " + customer.getMainContact_UserID()
				+ ", first name: " + customer.getMainContact_FirstName() + ", last name: "
				+ customer.getMainContact_LastName() + ", password as well confirmed password: "
				+ customer.getMainContact_password() + " & email id: " + customer.getMainContact_EmailID());
		report.logStepWithScreenshot(ReportLevel.INFO, "Clicking Save button and confirming customer creation....................");
		newCustomerPage.saveNewCustomer();
		report.logStepWithScreenshot(ReportLevel.INFO, "Message popped up asking to add secondary location; cancelling the message");
		newCustomerPage.waitAddCustomerLocationConfirmDialog();
		newCustomerPage.cancelAddLocationMessage();
		report.logStepWithScreenshot(ReportLevel.INFO, "Message popped up informing new customer creation");
		newCustomerPage.waitAddCustomerInfoDialog();
		newCustomerPage.confirmAddCustomerInfoMesage();
		if(!allCustomerPage.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load All customers page");
		}
		
		boolean result = allCustomerPage.searchCustomerBy(customer.getCustomerName(),
				CustomerSearchType.CUSTOMER_NAME);
		report.assertWithScreenshot(result, true,
				"Successfully added customer with name " + customer.getCustomerName() + "'",
				"Failed to add customer with name " + customer.getCustomerName() + "'");
		
		// open profile page for newly added customer and verify details
		allCustomerPage.openCustomerProfile(customer.getCustomerName());
		report.assertWithScreenshot(customerProfilePage.validate(), true,
				"Loaded customer profile page successfully for " + customer.getCustomerName(),
				"Failed to navigate & load customer profile page for " + customer.getCustomerName());
		
		report.assertWithScreenshot(AssertionLevel.SOFT,
				customerProfilePage.verifyHeadQuarter(customer.getCustomerName(), customer.getCustomerType()),
				true, "Customer Name & type are as expected", "Customer Name & Type didn't match with provided value");
		report.assertWithScreenshot(AssertionLevel.SOFT,
				customerProfilePage.verifyAddress(address, customer.getCountry(), null, null, null), true,
				"Customer address & country are as expected",
				"Customer adress & country didn't match with provided value");
		report.assertWithScreenshot(AssertionLevel.SOFT,
				customerProfilePage.verifyMainPhone(null, null, customer.getMainPhone()), true,
				"Main phone is as expected", "Main phone didn't match with provided value");
		report.assertWithScreenshot(AssertionLevel.SOFT,
				customerProfilePage.verifyLocale(customer.getTimeZone(), customer.getLegalJuridiction(),
						customer.getClinicLanguage()),
				true, "Customer timezone, jurisdiction & language are as expected",
				"Customer timezone, jurisdiction & language didn't match with provided value");
		report.assertWithScreenshot(AssertionLevel.SOFT,
				customerProfilePage.verifyMainContact(customer.getMainContact_UserID(),
						customer.getMainContact_FirstName(), null, customer.getMainContact_LastName(), null,
						customer.getMainContact_EmailID()),
				true, "Main contact details are as expected", "Main contact details didn't match with provided value");

		CustomerDBUtilities databaseUtils = new CustomerDBUtilities(report, database);
		result = databaseUtils.customerExists(customer.getCustomerName());
		report.assertCondition(result, true,
				"Customer with name " + customer.getCustomerName() + " is created in database",
				"No customer is created in database with name " + customer.getCustomerName() + "'");
		
		// Edit customer profile and verify if new changes are saved
		customer.setTimeZone(revisedTimeZone);
		customer.setPrimaryAddress(CommonUtils.randomAlphanumericString(5));
		customer.setMainPhone(String.valueOf(CommonUtils.getRandomNumber(1111111, 9999999)));
		customer.setMainContact_LastName("LN" + CommonUtils.randomAlphanumericString(5));
		report.logStep(ReportLevel.INFO,
				"Opening profile of customer with name - " + customer.getCustomerName() + " to edit.....");
		customerProfilePage.editProfilePage();
		report.assertWithScreenshot(customerProfilePage.validate(), true,
				"Loaded customer profile page succesfully for " + customer.getCustomerName(),
				"Failed to navigate & load customer profile page for " + customer.getCustomerName());
		address = new ArrayList<String>();
		address.add(customer.getPrimaryAddress());
		customerProfilePage.editAddress(address, null, null, null, null);
		report.logStepWithScreenshot(ReportLevel.INFO, "Edited primary address: " + customer.getPrimaryAddress());
		customerProfilePage.editMainPhone(null, null, customer.getMainPhone());
		report.logStepWithScreenshot(ReportLevel.INFO, "Edited main phone: " + customer.getMainPhone());
		customerProfilePage.editLocale(customer.getTimeZone(), null, null);
		report.logStepWithScreenshot(ReportLevel.INFO, "Edited timezone: " + customer.getTimeZone());
		report.logStep(ReportLevel.INFO, "Clicking Save button and confirming customer changes....................");
		customerProfilePage.saveProfilePage();
		customerProfilePage.waitForConfirmationPopup();
		customerProfilePage.clickOKOnConfirmationDialog();
		if (allCustomerPage.searchCustomerBy(customer.getCustomerName(), CustomerSearchType.CUSTOMER_NAME)) {
			allCustomerPage.openCustomerProfile(customer.getCustomerName());
			report.assertWithScreenshot(customerProfilePage.validate(), true,
					"Loaded customer profile page succesfully for " + customer.getCustomerName(),
					"Failed to navigate & load customer profile page for " + customer.getCustomerName());
			report.assertWithScreenshot(AssertionLevel.SOFT,
					customerProfilePage.verifyAddress(address, null, null, null, null), true,
					"Edited customer address is as expected",
					"Edited customer adress didn't match with provided value");
			report.assertWithScreenshot(AssertionLevel.SOFT,
					customerProfilePage.verifyMainPhone(null, null, customer.getMainPhone()), true,
					"Edited main phone is as expected", "Edited main phone didn't match with provided value");
			report.assertWithScreenshot(AssertionLevel.SOFT,
					customerProfilePage.verifyLocale(customer.getTimeZone(), null, null), true,
					"Edited customer timezone is as expected",
					"Edited customer timezone didn't match with provided value");
		} else {
			report.assertWithScreenshot(false, true,
					"Search for " + customer.getCustomerName() + " on customer list page is successful",
					"Failed to search " + customer.getCustomerName() + "'on customer list page");
		}
	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.ALL_MICROSERVICES);
		}
		super.cleanup(result);
	}
}