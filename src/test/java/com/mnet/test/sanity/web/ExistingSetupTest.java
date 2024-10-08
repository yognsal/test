package com.mnet.test.sanity.web;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestStep;
import com.mnet.pojo.clinic.admin.ClinicUser;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.patient.Patient;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.utilities.SetupUtilities;

/**
 * Sanity for verifying how to read already existing test data
 * 
 * @author NAIKKX12
 *
 */
public class ExistingSetupTest extends MITETest implements GraylogReporting {

	private Customer customer;
	private Patient patient1, patient2;
	private ClinicUser user1, user2;
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.DATABASE);
		super.initialize(context);
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);
		// Create Customer
		customer = new Customer();
		customer.setAddAllDevices(true);
		customer = SetupUtilities.setupCustomer(this, customer, 1, false);
		if (customer == null) {
			report.logStep(TestStep.builder().failMessage("Failed to create/ read test customer & authorize application").build());
		}
		report.logStep(TestStep.builder().message("Customer: " + customer).build());
		
		patient1 = new Patient();
		patient1 = SetupUtilities.setupPatient(this, patient1, 1);
		if (patient1 == null) {
			report.logStep(TestStep.builder().failMessage("Failed to create/ read test patient 1").build());
		}
		report.logStep(TestStep.builder().message("patient1: " + patient1).build());
		
		patient2 = new Patient();
		patient2 = SetupUtilities.setupPatient(this, patient2, 2);
		if (patient2 == null) {
			report.logStep(TestStep.builder().failMessage("Failed to create/ read test patient 2").build());
		}
		report.logStep(TestStep.builder().message("patient2: " + patient2).build());
		
		user1 = new ClinicUser();
		user1 = SetupUtilities.setupUser(this, user1, 1);
		if (user1 == null) {
			report.logStep(TestStep.builder().failMessage("Failed to create/ read test user 1").build());
		}
		report.logStep(TestStep.builder().message("User1: " + user1).build());
		
		user2 = new ClinicUser();
		user2 = SetupUtilities.setupUser(this, user2, 2);
		if (user2 == null) {
			report.logStep(TestStep.builder().failMessage("Failed to create/ read test user 2").build());
		}
		report.logStep(TestStep.builder().message("User2: " + user2).build());
	}

	@Test
	public void procedure() {
		//TODO:
	}
}