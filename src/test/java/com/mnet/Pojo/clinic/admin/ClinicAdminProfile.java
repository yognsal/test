package com.mnet.pojo.clinic.admin;

import com.mnet.framework.utilities.CommonUtils;

import lombok.Data;

/**
 * Pojo class for clinic profile (Clinic Administration -> Clinic Profile)
 * 
 * @author NAIKKX12
 *
 */
@Data
public class ClinicAdminProfile {
	//ClinicDetails
	private String clinicName;
	private String location;
	private OnCallContactMode onCallMode;
	
	//Clinic Address & Contact
	private String mainPhone_CountryCode;
	private String mainPhone_CityCode;
	private String mainPhone_Number;
	private String registeredEmailID;
	private String securityStamp;
	
	//Clinic Regional setting
	private String timeZone;
	private String language;
	private String dateFormat;
	private String timeFormat;
	private String numberFormat;
	private String weightUnits;
		
	public enum OnCallContactMode {
		EMAIL("Email"), FAX("Fax"), TEXT_MESSAGE("Text Message"), NONE ("None"), PHONE ("Phone");

		private String contactMode;

		private OnCallContactMode(String contactMode) {
			this.contactMode = contactMode;
		}

		public String getOnCallMode() {
			return this.contactMode;
		}
	}

	/**
	 * Default constructor
	 */
	public ClinicAdminProfile() {
		mainPhone_Number = String.valueOf(CommonUtils.getRandomNumber(11111111, 9999999));
	}
	
	/**
	 * Parameterized constructor only considering mandate fields on page
	 */
	public ClinicAdminProfile(String clinicName, String location, String mainPhone_CountryCode, String mainPhone_CityCode, String mainPhone_Number, String registeredEmailID, String securityStamp, String timeZone) {
		this.clinicName = clinicName;
		this.location = location;
		this.mainPhone_CountryCode = mainPhone_CountryCode;
		this.mainPhone_CityCode = mainPhone_CityCode;
		this.mainPhone_Number = mainPhone_Number;
		this.registeredEmailID = registeredEmailID;
		this.securityStamp = securityStamp;
		this.timeZone = timeZone;
	}
}