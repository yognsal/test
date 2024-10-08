package com.mnet.mobility.utilities.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.enums.DevMedAttribute;
import com.mnet.mobility.utilities.profile.enums.SystemAttribute;
import com.mnet.pojo.customer.Customer;

/**
 * Object formation and business logic validation for NGQ/ICM Follow-up profile
 * @version 1.0
 * @author NAIKKX12
 */
public class SystemProfile extends AppProfile {
	
	private static final int SYSTEMPROFILECODE = 100;
	
	public SystemProfile(PatientApp patientApp) {
		super(patientApp, SYSTEMPROFILECODE);
	}
		
	/** Get all attribute names */
	public List<ProfileAttribute> getAllAttributes() {
		return Arrays.asList(SystemAttribute.values());
	}

	/** Validate requested attribute */
	public boolean validateAttribute(ProfileAttribute attribute) {
		Customer customer = customerInfo();
		Map<String, String> deviceInfo = deviceInfo();
		Object expectedValue;
		
		switch ((SystemAttribute) attribute) {
		case contentVersion:
			expectedValue = getContentVersion();
			break;
		case clinicInformation_clinicName:
			expectedValue = customer.getCustomerName();
			break;
		case clinicInformation_address_address1:
			expectedValue = customer.getClinicInfoAddress().get(0) == null ? "" : customer.getClinicInfoAddress().get(0);
			break;
		case clinicInformation_address_address2:
			expectedValue = customer.getClinicInfoAddress().get(1) == null ? "" : customer.getClinicInfoAddress().get(1);
			break;
		case clinicInformation_address_address3:
			expectedValue = customer.getClinicInfoAddress().get(2) == null ? "" : customer.getClinicInfoAddress().get(2);
			break;
		case clinicInformation_address_city:
			expectedValue = customer.getClinicInfoCity() == null ? "" : customer.getClinicInfoCity();
			break;
		case clinicInformation_address_state:
			expectedValue = customer.getClinicInfoState() == null ? "" : customer.getClinicInfoState();
			break;
		case clinicInformation_address_zip:
			expectedValue = customer.getClinicInfoZipcode() == null ? "" : customer.getClinicInfoZipcode();
			break;
		case clinicInformation_address_country:
			expectedValue = customer.getCountry();
			break;
		case clinicInformation_contactPhone_country:
			expectedValue = "+" + customer.getMainPhoneCountryCode();
			break;
		case clinicInformation_contactPhone_area:
			expectedValue = customer.getMainPhoneAreaCode();
			break;
		case clinicInformation_contactPhone_number:
			expectedValue = customer.getMainPhone();
			break;
		case deviceInformation_marketingName:
			expectedValue = deviceInfo.get("device_name");
			break;
		case deviceInformation_modelNumber:
			expectedValue = deviceInfo.get("device_model_num");
			break;
		case userNotificationWindow_notifyWindowStart:
		case userNotificationWindow_notifyWindowEnd:
		case debugProxyLogging_verbose:
		case appLogging_verbose:
		case backgroundConnectionAttemptIntervalControl_retryCount:
		case backgroundConnectionAttemptIntervalControl_retryDelay:
		case deviceInformation_unityApplicationID:
		case workflowPriority:
		case foregroundDeviceScanTimeout:
		case complianceNotificationDelay:
		case workflowSequences:
			expectedValue = getAttributeDefaultValue(attribute);
			break;
		case clearDataAndReset:
			expectedValue = getClearAndDataReset();
			break;
		default:
			report.logStep(
					TestStep.builder().failMessage((DevMedAttribute) attribute + " attribute not handled").build());
			return false;
		}
	
		if (expectedValue == null) {
			report.logStep(
					TestStep.builder().reportLevel(ReportLevel.WARNING).message("Expected value of '" + (SystemAttribute) attribute + "' attribute yet to be calculated").build());
			return true;
		}
		
		return compareAttributesAndReport(attribute, expectedValue);
	}
	
	/** Get customer object from database */
	private Customer customerInfo() {
		Customer customer = Customer.empty();

		String dbValue = database.executeQuery("select customerid from patients.v_patients_for_userrecord where deviceserialnum = '" + patientApp.getDeviceSerial() + "'").getFirstRow().get(0);
		List<String> info = database.executeQuery("select name, customer_address_id, main_phone_id from customers.customer c where customer_id = '" + dbValue + "'").getFirstRow();
		customer.setCustomerName(info.get(0));
		dbValue = info.get(2);
		
		info = database.executeQuery("select street_address, street_address2, street_address3, city, zip_code, country_cd, state_province from customers.customer_address ca where customer_address_id = '" + info.get(1) + "'").getFirstRow();
		customer.setClinicInfoAddress(Arrays.asList(info.get(0), info.get(1), info.get(2)));
		customer.setClinicInfoCity(info.get(3));
		customer.setClinicInfoZipcode(info.get(4));
		customer.setCountry(database.executeQuery("select code_desc from lookup.code c where code_id = '" + info.get(5) + "' and code_qualifier = 'Country_Cd'").getFirstRow().get(0));
		customer.setClinicInfoState(info.get(6));
		
		info = database.executeQuery("select country_code, area_code, phone_num from customers.customer_phone cp where customer_phone_id = '" + dbValue + "'").getFirstRow();
		
		customer.setMainPhoneCountryCode(info.get(0));
		customer.setMainPhoneAreaCode(info.get(1));
		customer.setMainPhone(info.get(2));
		
		return customer;
	}
	
	/** Get device information from database */
	private Map<String, String> deviceInfo(){
		String productId = database.executeQuery("select device_product_id from patients.patient_device pd where device_serial_num = '" + patientApp.getDeviceSerial() + "'").getFirstRow().get(0);
		List<String> info = database.executeQuery("select device_name, device_model_num from devices.device_product dp where device_product_id = '" + productId + "'").getFirstRow();
		
		return Map.of("device_name", info.get(0), "device_model_num", info.get(1)); 
	}
	
	/** Get ClearAndDataReset flag value */
	private boolean getClearAndDataReset() {
		List<Map<String, String>> dbContent = PatientAppDBUtilities.getPatientProfileContainer(patientApp.getCurrentTest(), patientApp.getDeviceSerial(), 0);
		List<Integer> profiles = new ArrayList<>();
		for (Map<String, String> map : dbContent) {
			profiles.add(Integer.parseInt(map.get("profile_cd")));
		}
		
		if (profiles.size() == 1 && profiles.get(0) == SYSTEMPROFILECODE) {
			return true;
		}
		
		dbContent = PatientAppDBUtilities.getPPortalUserDevice(patientApp.getCurrentTest(), "user_record_id", String.valueOf(patientApp.getIdentity().getUserRecordId()));
		if (dbContent.size() > 1) {
			boolean firstCheck = false, secondCheck = false;
			
			for (Map<String, String> map : dbContent) {
				if (map.get("azureid").equals(patientApp.getAzureId())){
					firstCheck = map.get("active_flg").equals("t") ? false : true;
				}
				if (secondCheck == false && map.get("azureid").equals(patientApp.getAzureId()) == false){
					secondCheck = map.get("active_flg").equals("t") ? true : false;
				}		
			}
			
			if (firstCheck && secondCheck) {
				return true;
			}
		}
		
		return false;
	}
}