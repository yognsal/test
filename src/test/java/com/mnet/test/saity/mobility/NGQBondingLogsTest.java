package com.mnet.test.sanity.mobility;

import java.util.List;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities.DeviceModelType;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ScreenshotType;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.NGQPatientApp;
import com.mnet.mobility.utilities.validation.BondingValidation;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.mobility.AppLifeEvent.AppLifeEventResult;
import com.mnet.pojo.mobility.ale.IMDBondingStatus;
import com.mnet.pojo.patient.Patient;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.utilities.PatientUtilities;
import com.mnet.webapp.utilities.SetupUtilities;

/**
 * NGQ Bonding logs - basic sanity
 * 
 * @author NAIKKX12
 *
 */
public class NGQBondingLogsTest extends MITETest implements GraylogReporting, BondingValidation {

	private ClinicNavigationBar navigationBar;
	
	private PatientUtilities patientUtils;

	private PatientDBUtilities patientDBUtils;
	
	private Customer customerObj;
	private Patient patient;
	private NGQPatientApp ngqPatientApp;

	private String dateOfBirth;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.AZURE);
		super.initialize(context);

		// Initialize test-specific page objects
		patientUtils = new PatientUtilities(report, webDriver);
		navigationBar = new ClinicNavigationBar(webDriver, report);
		
		patientDBUtils = new PatientDBUtilities(report, database);
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);
		
		// Create Customer
		customerObj = new Customer();
		customerObj.setAddAllDevices(true);
		customerObj = SetupUtilities.setupCustomer(this, customerObj, 1, true);
		if (customerObj == null) {
			report.logStep(TestStep.builder().failMessage("Failed to create/ read test customer & authorize application").build());
		}

		if (!navigationBar.validate()) {
			report.logStep(TestStep.builder().message("clinic navigation panel is not loaded").screenshotType(ScreenshotType.SCROLLING).build());
		}
		report.logStep(TestStep.builder().message("clinic navigation panel is loaded successfully").screenshotType(ScreenshotType.SCROLLING).build());
		
		// add patient & perform transmission
		patient = new Patient();
		patient.setDeviceType(Patient.DeviceType.NGQ.getDeviceType());
		List<String> tempList = patientDBUtils.getIcdOrNgqDeviceList(DeviceModelType.NGQ);

		patient.setDeviceModelNum(tempList.get(CommonUtils.getRandomNumber(0, tempList.size() - 1)));
		if (!patientUtils.enrollPatient(patient, false, null, null, true, false, false)) {
			report.logStep(TestStep.builder().message("Patient enrollment failed").screenshotType(ScreenshotType.SCROLLING).build());
		}
		report.logStep(TestStep.builder().message("New patient with device serial number - "
				+ patient.getDeviceSerialNum() + " is enrolled successfully").screenshotType(ScreenshotType.SCROLLING).build());

		dateOfBirth = DateUtility.changeDateFormat(patient.getDateOfBirth(),
				FrameworkProperties.getProperty("WEBAPP_DEFAULT_DATE_FORMAT"),
				FrameworkProperties.getProperty("PAYLOAD_ZIP_DATE_FORMAT"));
	}

	@Test
	public void bondingLogsTest() {
		
		ngqPatientApp = new NGQPatientApp(this, MobilityOS.ANDROID);
		ngqPatientApp.setApiValidation(true);
		ngqPatientApp.requestAzureData(patient.getDeviceSerialNum());
		ngqPatientApp.validatePatient(patient.getDeviceSerialNum(), dateOfBirth);
		ngqPatientApp.provision();
		String fileName = MobilityUtilities.getBondingLogFile(this, CommonUtils.getRandomNumber(1, 2));
		ngqPatientApp.preBondingLogs(fileName);
		report.assertCondition(verifyBondingLogInDB(ngqPatientApp), true, TestStep.builder().message("DB check for prebondng logs API").build());
		report.assertCondition(verifyBondingLogInAzure(ngqPatientApp), true, TestStep.builder().message("Azure check for prebondng logs API").build());
		ngqPatientApp.sendD2CMessage(new IMDBondingStatus(ngqPatientApp, AppLifeEventResult.SUCCESS));
		ngqPatientApp.postBondingLogs(fileName);
		this.getWebDriver().staticWait(this.getWebDriver().minBrowserTimeout() * 60);
		report.assertCondition(verifyBondingLogInDB(ngqPatientApp), true, TestStep.builder().message("DB check for postbondng logs API").build());
		report.assertCondition(verifyBondingLogInAzure(ngqPatientApp), true, TestStep.builder().message("Azure check for postbondng logs API").build());
	}
}
