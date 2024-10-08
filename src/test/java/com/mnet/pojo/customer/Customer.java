package com.mnet.pojo.customer;

import java.util.List;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.email.EmailParser;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.CommonUtils.StringType;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * pojo class for new customer details (Intermal link -> All Abbott Customers ->
 * Add new customer)
 * 
 * @author NAIKKX12
 *
 */

@AllArgsConstructor
@Data
public class Customer {
	
	public static Customer empty() {
		return new Customer(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
	}

	// Mandate fields
	private String customerName;
	private CustomerType customerType;
	private String primaryAddress;
	private String mainPhone;
	private String country;
	private String registeredEmailID;
	private String timeZone;
	//TODO: spell check Jurisdiction** as well datatype to enum
	private String legalJuridiction;
	private String clinicLanguage;
	private String mainContact_UserID;
	private String mainContact_password;
	private String mainContact_confirmPwd;
	private String mainContact_FirstName;
	private String mainContact_LastName;
	private String mainContact_EmailID;
	private ClinicLanguage language;

	// Optional fields
	private List<String> clinicInfoAddress;
	private String clinicInfoCity;
	private String clinicInfoState;
	private String clinicInfoZipcode;
	private String secondaryPhoneNum;
	private String secondaryAreaCode;
	private String FaxPhoneNum;
	private String FaxAreaCode;
	private List<String> devicesToAdd;
	private boolean addAllDevices;
	private boolean exportTransmission;
	private boolean activatorClinic;
	private String mainPhoneCountryCode;
	private String mainPhoneAreaCode;
	private String mainContact_MiddleName;
	private String textMessage;
	// Electrophysiology features
	private boolean exportTransmissionData;
	private boolean orderTransmitter;
	private boolean lockoutDirectAlerts;
	private ExportToEHR exportToEHR;
	// General features
	private boolean recordPatientDataCollectionConsent;
	private boolean allowMobileDirectAlertNotification;
	private String secondaryPhoneCountryCode;
	
	//Identity Management Checkbox (For France Clinic Only)
	private boolean identityManagementSystem;

	private boolean removeNonBluetoothDirectAlert;
	private boolean removeDirectAlert;
	private boolean removeBluetoothDirectAlert;
	private boolean removeICMDirectAlert;
	private boolean removeAllDevices;

	public enum CustomerType {
		DIRECT("Direct"), SERVICE_PROVIDER("Service Provider");

		private String custType;

		private CustomerType(String custType) {
			this.custType = custType;
		}

		public String getCustType() {
			return this.custType;
		}
	}
	
	public enum ClinicLanguage {
		ENGLISH_UK("English (UK)"), FRENCH("French"), GERMAN("German");
		
		private String language;
		
		private ClinicLanguage(String clinicLang) {
			this.language = clinicLang;
		}
		
		public String getclinicLanguage() {
			return this.language;
		}
	}
	
	public enum ExportToEHR {
		NO_EXPORT("No export"), AUTO_MANUAL("Auto/Manual"), MANUAL("Manual");

		private String exportOption;

		private ExportToEHR(String exportOption) {
			this.exportOption = exportOption;
		}

		public String getExportOption() {
			return this.exportOption;
		}
	}

	public enum FeatureControl {
		Application (1), ClinicalTrials (2), NonBluetoothDirectAlerts(6), DirectAlertsNotifications(7), BluetoothDirectAlerts (8),ICMDirectAlerts (9), Devices (10), BluetoothDirectAlerts2(10) ,ICMDirectAlerts2(12);
		
		private int featureControlIndex;

		private FeatureControl(int featureControlIndex) {
			this.featureControlIndex = featureControlIndex;
		}

		public int getFeatureControlIndex() {
			return this.featureControlIndex;
		}
	}
	
	public enum LegalJurisdiction {
		USA("USA"), Canada("Canada"), EU("European Union"), AustraliaNZ("Australia / New Zealand"), Japan("Japan");

		private String jurisdiction;

		private LegalJurisdiction(String jurisdiction) {
			this.jurisdiction = jurisdiction;
		}

		public String getJurisdiction() {
			return this.jurisdiction;
		}
	}

	/**
	 * Default constructor
	 */
	public Customer() {
		customerName = "auto" + CommonUtils.randomAlphanumericString(6);
		customerType = CustomerType.DIRECT;
		primaryAddress = CommonUtils.randomAlphanumericString(8);
		mainPhone = String.valueOf(CommonUtils.getRandomNumber(1111111, 9999999));
		country = "Spain";
		//Adding this workaround due to defect PCN00043023
		clinicInfoState = CommonUtils.randomString(10, StringType.LOWER);
		if(!Boolean.parseBoolean(FrameworkProperties.getProperty("MANUAL_CUSTOMER_SETUP"))) {
			registeredEmailID = EmailParser.getMFAEmailID();
		}else {
			registeredEmailID = FrameworkProperties.getProperty("EMAIL_USER_ID");
		}
		timeZone = "(GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi";
		legalJuridiction = LegalJurisdiction.EU.getJurisdiction();
		clinicLanguage = "English (US)";

		mainContact_UserID = "id" + CommonUtils.randomAlphanumericString(5).toLowerCase();
		mainContact_password = "pwd" + CommonUtils.randomAlphanumericString(5).toUpperCase()
				+ String.valueOf(CommonUtils.getRandomNumber(0, 99));
		mainContact_confirmPwd = mainContact_password;
		mainContact_FirstName = "FN" + CommonUtils.randomAlphanumericString(4);
		mainContact_LastName = "LN" + CommonUtils.randomAlphanumericString(4); 
		mainContact_MiddleName = CommonUtils.randomAlphanumericString(4);
		mainContact_EmailID = registeredEmailID;
	}

	public Customer(String customerName,  CustomerType customerType,  String primaryAddress,  String mainPhone,  String country,  String registeredEmailID,  String timeZone,  LegalJurisdiction legalJurisdiction,  String clinicLanguage,  String mainContact_UserID,  String mainContact_password,  String mainContact_confirmPwd,  String mainContact_FirstName,  String mainContact_LastName,  String mainContact_EmailID) {
		this.customerName = customerName;
		this.customerType = customerType;
		this.primaryAddress = primaryAddress;
		this.mainPhone = mainPhone;
		this.country = country;
		this.registeredEmailID = registeredEmailID;
		this.timeZone = timeZone;
		this.legalJuridiction = legalJurisdiction == null ? null : legalJurisdiction.getJurisdiction();
		this.clinicLanguage = clinicLanguage;

		this.mainContact_UserID = mainContact_UserID;
		this.mainContact_password = mainContact_password;
		this.mainContact_confirmPwd = mainContact_confirmPwd;
		this.mainContact_FirstName = mainContact_FirstName;
		this.mainContact_LastName = mainContact_LastName;
		this.mainContact_EmailID = mainContact_EmailID;
	}
}
