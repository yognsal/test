package com.mnet.test.smoke.web;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.patient.Patient;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicHeader;
import com.mnet.webapp.pageobjects.clinic.ClinicLoginPage;
import com.mnet.webapp.pageobjects.clinic.ClinicLogoutPage;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.utilities.LoginUtilities;
import com.mnet.webapp.utilities.PatientUtilities;
import com.mnet.webapp.utilities.SetupUtilities;

public class multiplePatients extends MITETest implements GraylogReporting{
	
	private ClinicLoginPage loginPage;
	private ClinicNavigationBar navigationBar;
	private ClinicHeader header;
	private ClinicLogoutPage logoutPage;
	private LoginUtilities loginUtils;
	private PatientUtilities patientUtils;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.EMAIL);
		super.initialize(context);

		// Initialize test-specific page objects
		loginPage = new ClinicLoginPage(webDriver, report);
		navigationBar = new ClinicNavigationBar(webDriver, report);
		logoutPage = new ClinicLogoutPage(webDriver);
		patientUtils = new PatientUtilities(report, webDriver);
		loginUtils = new LoginUtilities(report, email, webDriver);
	}
	
	@Test
	private void createPatients() {
		
		int totalPatient = Integer.parseInt(FrameworkProperties.getProperty("PATIENT_COUNT"));
		String username = FrameworkProperties.getProperty("USERNAME");
		String password = FrameworkProperties.getProperty("PASSWORD");
		if(username.isEmpty() && password.isEmpty()) {
			Customer customer = new Customer();
			customer.setAddAllDevices(true);
			customer = SetupUtilities.setupCustomer(this, customer, 1, true);
		}else {
			loginUtils.login(username, password);
		}
		
		if(!navigationBar.validate()) {
			return;
		}
		
		for(int i = 0; i < totalPatient; i++) {
			Patient patient = new Patient();
			patientUtils.EnrollPatient(patient);
		}
		
		
	}
	
	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		loginUtils.logout();
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.ALL_MICROSERVICES);
		}
		super.cleanup(result);
	}


}
