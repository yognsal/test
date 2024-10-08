package com.mnet.mobility.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.database.utilities.PatientDBUtilities;
import com.mnet.framework.api.APIResponse;
import com.mnet.framework.azure.AzureIotHub;
import com.mnet.framework.azure.AzureManagedIdentity;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.email.Email;
import com.mnet.framework.email.EmailParser;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.reporting.TestStep.ScreenshotType;
import com.mnet.framework.utilities.DateUtility;
import com.mnet.framework.web.WebUtilities;
import com.mnet.middleware.utilities.PayloadExcelUtilities;
import com.mnet.middleware.utilities.PayloadGenerator;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityDeviceType;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.PatientApp.ContactTypeKey;
import com.mnet.mobility.utilities.validation.DeviceProvisioningValidation;
import com.mnet.pojo.clinic.admin.DirectAlertHomeTransmitter.ContactType;
import com.mnet.pojo.customer.Customer;
import com.mnet.pojo.mobility.PhoneData;
import com.mnet.pojo.patient.Patient;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicAdminNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.DirectAlertMobileAppTransmitterPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientFilterType;
import com.mnet.webapp.pageobjects.clinic.patientlist.PatientListPage.PatientSearchType;
import com.mnet.webapp.pageobjects.patient.DirectAlertsNotificationsProfilePage;
import com.mnet.webapp.pageobjects.patient.PatientNavigationBar;
import com.mnet.webapp.pageobjects.patient.PatientProfileAndDeviceData;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.PatientUtilities;
import com.mnet.webapp.utilities.SetupUtilities;
import com.mnet.webapp.utilities.StringPropertyParser;
import com.mnet.webapp.utilities.directalerts.DirectAlertsSelection;

/**
 * Provides functions for common operations across NGQ functional test cases
 * @author NAIKKX12
 */
public class NGQTestUtilities implements DeviceProvisioningValidation, DirectAlertsSelection {
	
	private ClinicNavigationBar navigationBar;
	private DirectAlertMobileAppTransmitterPage directAlertMobileAppTransmitterPage;
	private ClinicAdminNavigationBar clinicAdminNavigationBar;
	private PatientListPage patientListPage;
	
	private CustomerUtilities customerUtil;
	private PatientUtilities patientUtil;
	private PatientDBUtilities patientDBUtil;
	
	private FrameworkLog log;
	private EmailParser email;
	private WebUtilities driver;
	private TestReporter report;
	private DatabaseConnector database;
	
	private AzureManagedIdentity azureIdentity;
	private AzureIotHub iotHub;
	
	private MITETest currentTest;
	
	private String ACTIVATION_CODE_MAIL_SUBJECT = FrameworkProperties.getProperty("ACTIVATION_CODE_MAIL_IDENTIFIER");

	private String ACTIVATION_CODE_PREFIX = FrameworkProperties.getProperty("ACTIVATION_CODE_PREFIX");
	
	private static final String standardAlertString = "{\"globalAlertId\":\"<alert>\",\"rcNotificationValue\":\"STANDARD\"}";
	private static final String noneAlertString = "{\"globalAlertId\":\"<alert>\",\"rcNotificationValue\":\"NONE\"}";
	
	public enum DeviceFamily {
		CDHFA("CDHFA"), CDHFA_USA("CDHFA_USA"), CDDRA("CDDRA"), CDVRA("CDVRA"), CDVRA_USA("CDVRA_USA"), PACEMAKER_1232_USA("PACEMAKER_1232_USA"), PACEMAKER_3107_36_USA("PACEMAKER_3207_36_USA"), ICD_2257_40_USA("ICD_2257_40_USA");

		private String deviceFamily;

		private DeviceFamily(String deviceFamily) {
			this.deviceFamily = deviceFamily;
		}

		public String getDeviceFamily() {
			return this.deviceFamily;
		}

		private static final List<DeviceFamily> enumValues = Collections.unmodifiableList(Arrays.asList(values()));
		private static final int enumSize = enumValues.size();
		private static final Random random = new Random();

		public static DeviceFamily randomDeviceFamily() {
			return enumValues.get(random.nextInt(enumSize));
		}
	}
	
	public NGQTestUtilities(MITETest currentTest) {
		this.currentTest = currentTest;
		driver = currentTest.getWebDriver();
		database = currentTest.getDatabase();
		email = currentTest.getEmail();
		report = currentTest.getReport();
		log = currentTest.getLog();
		azureIdentity = currentTest.getAzure();
		
		navigationBar = new ClinicNavigationBar(driver, report);
		directAlertMobileAppTransmitterPage = new DirectAlertMobileAppTransmitterPage(driver, report);
		clinicAdminNavigationBar = new ClinicAdminNavigationBar(driver, report);
		patientListPage = new PatientListPage(driver, report);
		
		patientUtil = new PatientUtilities(report, driver);
		customerUtil = new CustomerUtilities(report, driver);
		
		patientDBUtil = new PatientDBUtilities(report, database);
		
		iotHub = (AzureIotHub) azureIdentity.getResource(FrameworkProperties.getProperty("AZURE_IOT_HUB"), AzureIotHub.class);
	}
	
	/** Create/ read existing Customer */
	@Deprecated
	public Customer getTestCustomer(boolean existingCustomer) {
		Customer customer = new Customer();
		customer.setAddAllDevices(true);
		return SetupUtilities.setupCustomer(currentTest, customer, 1, true);
	}
	
	/** Create/ read existing Customer */
	public Customer getTestCustomer(Customer customer, int instance, boolean loginToClinic) {
		if(customer == null) {
			customer = new Customer();
			customer.setAddAllDevices(true);
		}
		return SetupUtilities.setupCustomer(currentTest, customer, instance, loginToClinic);
	}
	
	/** Create patient */
	@Deprecated
	public Patient getTestPatient(Patient patient) {
		if(patient == null) {
			patient = new Patient();
			patient.setDeviceType(Patient.DeviceType.NGQ.getDeviceType());
		}
		if (patient.getDeviceType() == Patient.DeviceType.NGQ.getDeviceType()) {
			patient.setDeviceModelNum(PatientAppDBUtilities.getNGQDevice(currentTest, null).get("device_full_description"));
		}

		if (patientUtil.EnrollPatient(patient)) {
			report.logStep(TestStep.builder().message("New patient with device serial number - "
					+ patient.getDeviceSerialNum() + " is enrolled successfully").screenshotType(ScreenshotType.SCROLLING).build());
			patient.setDateOfBirth(DateUtility.changeDateFormat(patient.getDateOfBirth(),
					FrameworkProperties.getProperty("WEBAPP_DEFAULT_DATE_FORMAT"),
					FrameworkProperties.getProperty("PAYLOAD_ZIP_DATE_FORMAT")));
			
			//Added below method to fetch alert mapping based on UI
			if (patient.getDeviceType().equals("NGQ") || patient.getDeviceModelNum().contains("CDHFA")
					|| patient.getDeviceModelNum().contains("CDDRA") || patient.getDeviceModelNum().contains("CDVRA")) {
				getUIAlertMapping(currentTest);

				navigationBar.viewPatientList();
				if (!patientListPage.validate()) {
					report.logStep(TestStep.builder().failMessage("Patient List page not loaded")
							.screenshotType(ScreenshotType.SCROLLING).build());
					return null;
				}
			}
			
			return patient;
		}
		return null;
	}
	
	/** Create patient or read existing patient*/
	public Patient getTestPatient(Patient patient, int instance) {
		if(patient == null) {
			patient = new Patient();
			patient.setDeviceType(Patient.DeviceType.NGQ.getDeviceType());
			patient.setDeviceModelNum(PatientAppDBUtilities.getNGQDevice(currentTest, null).get("device_full_description"));
		}
		
		Patient setupPatient = SetupUtilities.setupPatient(currentTest, patient, instance);
		setupPatient.setDateOfBirth(DateUtility.changeDateFormat(setupPatient.getDateOfBirth(),
						FrameworkProperties.getProperty("WEBAPP_DEFAULT_DATE_FORMAT"),
						FrameworkProperties.getProperty("PAYLOAD_ZIP_DATE_FORMAT")));
		
		//Added below method to fetch alert mapping based on UI and a check to only do it for NGQ devices
		if (patient.getDeviceType().equals("NGQ") || patient.getDeviceModelNum().contains("CDHFA")
				|| patient.getDeviceModelNum().contains("CDDRA") || patient.getDeviceModelNum().contains("CDVRA")
				|| setupPatient.getDeviceModelNum().contains("CDHFA")
				|| setupPatient.getDeviceModelNum().contains("CDDRA")
				|| setupPatient.getDeviceModelNum().contains("CDVRA")) {
			getUIAlertMapping(currentTest);

			navigationBar.viewPatientList();
			if (!patientListPage.validate()) {
				report.logStep(TestStep.builder().failMessage("Patient List page not loaded")
						.screenshotType(ScreenshotType.SCROLLING).build());
				return null;
			}
		}
		
		
		return setupPatient;
	}
	
	/** Set contact channel in case of red/ yellow alerts */
	public void setClinicAlertContact(ContactType...contactTypes) {
		if (!navigationBar.validate()) {
			report.logStep(TestStep.builder().failMessage("clinic navigation panel is not loaded").screenshotType(ScreenshotType.SCROLLING).build());
		}
		report.logStep(TestStep.builder().message("clinic navigation panel is loaded successfully").screenshotType(ScreenshotType.SCROLLING).build());
		
		navigationBar.viewClinicAdministrationPage();
		if (!clinicAdminNavigationBar.validate()) {
			report.logStep(TestStep.builder().failMessage("clinic administration page is not loaded").screenshotType(ScreenshotType.SCROLLING).build());
		}
		report.logStep(TestStep.builder().message("clinic administration page is loaded successfully").screenshotType(ScreenshotType.SCROLLING).build());
		
		clinicAdminNavigationBar.viewDirectAlertMobileAppTramsmitterPage();
		if (!directAlertMobileAppTransmitterPage.validate()) {
			report.logStep(TestStep.builder().failMessage("Direct Alert settings -> Mobile App Transmitter page is not loaded").screenshotType(ScreenshotType.SCROLLING).build());
		}
		report.logStep(TestStep.builder().message("Direct Alert settings -> Mobile App Transmitter page is loaded successfully").screenshotType(ScreenshotType.SCROLLING).build());
		
		directAlertMobileAppTransmitterPage.editPage();
		directAlertMobileAppTransmitterPage.selectRedAlertContactDuringOfficeHours(contactTypes[0]);
		directAlertMobileAppTransmitterPage.selectRedAlertContactAfterOfficeHours(contactTypes[1]);
		directAlertMobileAppTransmitterPage.selectYellowAlertContactAfterOfficeHours(contactTypes[2]);
		directAlertMobileAppTransmitterPage.selectYellowAlertContactDuringOfficeHours(contactTypes[3]);
		report.logStep(TestStep.builder().message("Updated communication channel to email for red/yellow alerts...saving changes").screenshotType(ScreenshotType.SCROLLING).build());
		
		directAlertMobileAppTransmitterPage.savePage();
		directAlertMobileAppTransmitterPage.waitMessagePopup();
		directAlertMobileAppTransmitterPage.handlePopup(true);
		directAlertMobileAppTransmitterPage.waitMessagePopup();
		directAlertMobileAppTransmitterPage.handlePopup(true);
	}
	
	/** Generate NGQ payload based on payloaddata supplied */
	public String generateNGQPayload(HashMap<String, String> payloadData) {
		PayloadGenerator payloadGenerator = new PayloadGenerator(log, payloadData,
				System.getProperty("user.dir") + FrameworkProperties.getProperty("DEFAULT_NGQ_TRANSMISSION_TESTDATA"));
		return payloadGenerator.generateNGQPayload();
	}
	
	/** Gets the phone data from IOT Hub -> Device Twin Properties */
	public PhoneData getPhoneDataFromIOTHub(String deviceId) {

		PhoneData phoneData = PhoneData.of(MobilityDeviceType.NGQ, MobilityOS.ANDROID);
		phoneData.setAppModel((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.appModel"));
		phoneData.setLocale((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.locale"));
		phoneData.setAppVer((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.appVer"));
		phoneData.setOs((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.os"));
		phoneData.setOsVer((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.osVer"));
		phoneData.setModel((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.model"));
		phoneData.setImeiNumber((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.IMEINumber"));
		phoneData.setServerRegion((String) iotHub.getDeviceTwinProperty(deviceId, "tags.phoneData.serverRegion"));
		// TODO: Add any more fields if required / pending
		return phoneData;
	}

	/**
	 * Gets invalid reesponse in case of wrong or invalid device serial or date of birth
	 * @param breakAfterAzureRequest  - Set this to true if invalid response is required after running requestAzureData method
	 * @param validatePatientResponse - set this to true if only validatePatient api response is required.
	 */
	public APIResponse invalidResponse(boolean breakAfterAzureRequest, boolean validatePatientResponse, NGQPatientApp ngqPatientApp, String deviceSerialForAzureData, String deviceSerialForValidatePatient, String dateOfBirth) {
		APIResponse response = null;
		if (!validatePatientResponse) {
			response = ngqPatientApp.requestAzureData(deviceSerialForAzureData);
			if (breakAfterAzureRequest) {
				if (response != null) {
					return response;
				} else {
					report.logStep(TestStep.builder().reportLevel(ReportLevel.ERROR).message("Failed to exeucte requestAzureData API").build());
				}
			}
		}
		return response = ngqPatientApp.validatePatient(deviceSerialForValidatePatient, dateOfBirth);
	}

	/** Fetches the activation code from email */
	public int getActivationCodeFromMail(EmailParser email) {
		String emailBody = getActivationMailContent(email).getMailBodyPreview();
		return Integer.parseInt(StringUtils.substringBefore(StringUtils.substringAfter(emailBody, ACTIVATION_CODE_PREFIX),"\n").trim());
	}

	/** Fetches the email content for Activation code mail */
	public Email getActivationMailContent(EmailParser email) {
		Email mail = email.getEmailWithText(ACTIVATION_CODE_MAIL_SUBJECT,
				email.getEmailCount(ACTIVATION_CODE_MAIL_SUBJECT));
		if (mail == null) {
			report.logStep(TestStep.builder().reportLevel(TestStep.ReportLevel.FAIL).message("Email Content was null")
					.build());
			return null;
		}

		return mail;

	}

	/**Check if email and phone are masked in Validate API Response*/
	public boolean checkIfContactIsMasked(ContactTypeKey contactTypeKey, String contactInfo) {
		switch(contactTypeKey) {
		case EMAIL:
			String slicedString = contactInfo.split("@")[0];
			slicedString = slicedString.substring(1);
			slicedString = slicedString.substring(0, slicedString.length()-1);
			char[] emailString = slicedString.toCharArray();
			boolean result = true;
			for(char emailChar : emailString) {
				if(emailChar != '*') {
					result = false;
				}
			}
			return result;
		case PRIMARY_PHONE:
			slicedString = contactInfo.substring(0, contactInfo.length()-4);
			emailString = slicedString.toCharArray();
			result = true;
			for(char emailChar : emailString) {
				if(emailChar != '*') {
					result = false;
				}
			}
			return result;
		}
		return false;
	}
	
	/**
	 * This function will gete standard and none classfied alerts based on NGQ
	 * device family which we pass in as parameter and return them in encoded format
	 */
	public String getEncodedRcNotification(DeviceFamily deviceFamily, boolean ndaRetry) {
		
		String decodedRcNotification = getAlerts(deviceFamily, ndaRetry);
		if(decodedRcNotification != null) {
			return MobilityUtilities.base64Encode(decodedRcNotification); 
		}
		return null;
		
	}
	
	/**
	 * This method can be used to get the list of alert strings for specific NGQ device model based on the device family passed in as parameter
	 */
	public List<String> getAlertList(DeviceFamily deviceFamily) {
		List<String> alertStrings = new ArrayList<>();
		String DATA_PATH = FrameworkProperties.getSystemProperty("user.dir") + File.separator 
				+ FrameworkProperties.getProperty("DATA_PATH") + File.separator;
		PayloadExcelUtilities excelUtilities = new PayloadExcelUtilities(DATA_PATH+"DeviceFamilyAlertsClassification.xlsx", log);
		List<String> standardAlertCodes = excelUtilities.getColumnValues(deviceFamily.getDeviceFamily()+"_Standard");
		List<String> nonAlertCodes = excelUtilities.getColumnValues(deviceFamily.getDeviceFamily()+"_None");
		
		for(int i = 1; i < standardAlertCodes.size(); i++) {
			alertStrings.add(standardAlertCodes.get(i));
		}
		
		for(int i = 1; i < nonAlertCodes.size(); i++) {
			alertStrings.add(nonAlertCodes.get(i));
		}
		
		excelUtilities.closeStream();
		return alertStrings;	
	}
	
	/**
	 * Local Function to read standard and none classfied alerts based on NGQ device family (CDHFA, CDDRA or CDVRA) which we pass as parameter.
	 * @implNote: If we need to perform NDARETRY, set ndaRetry to true which will change all standard alerts to none.
	 * Nothing will be changed on original data set so that NDASUCCESS can be performed wihtout any issues.
	 */
	private String getAlerts(DeviceFamily deviceFamily, boolean ndaRetry) {
		List<String> alertStrings = new ArrayList<>();
		String DATA_PATH = FrameworkProperties.getSystemProperty("user.dir") + File.separator 
				+ FrameworkProperties.getProperty("DATA_PATH") + File.separator;
		PayloadExcelUtilities excelUtilities = new PayloadExcelUtilities(DATA_PATH+"DeviceFamilyAlertsClassification.xlsx", log);
		List<String> standardAlertCodes = excelUtilities.getColumnValues(deviceFamily.getDeviceFamily()+"_Standard");
		List<String> nonAlertCodes = excelUtilities.getColumnValues(deviceFamily.getDeviceFamily()+"_None");
		
		for(int i = 1; i < standardAlertCodes.size(); i++) {
			alertStrings.add(standardAlertString.replace("<alert>", standardAlertCodes.get(i)));
		}
		
		for(int i = 1; i < nonAlertCodes.size(); i++) {
			alertStrings.add(noneAlertString.replace("<alert>", nonAlertCodes.get(i)));
		}
		
		excelUtilities.closeStream();
		String alerts = alertStrings.toString();
		
		if(ndaRetry) {
			alerts = alerts.replace("STANDARD", "NONE");
		}
		
		return alerts;	
	}

}