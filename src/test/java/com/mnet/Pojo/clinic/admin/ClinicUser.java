package com.mnet.pojo.clinic.admin;

import com.mnet.framework.utilities.CommonUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * Pojo class for new user to be added on Clinic Administration -> Clinic Users
 * page
 * 
 * @author NAIKKX12
 *
 */
@AllArgsConstructor
@Data
public class ClinicUser {
	
	public static ClinicUser empty() {
		return new ClinicUser(null, null, null, null, null, null, null, null, null, null, null, null);
	}

	// Mandate fields
	private String firstName;
	private String middleName;
	private String lastName;
	private String userId;
	private String password;
	private String confirmPwd;
	private UserType userType;
	private String mainPhone_AreaCode;
	private String mainPhone_Number;
	private String securityStamp;
	private String email;
	private String alternateEmail;
	private String mainPhone_CountryCode;
	private String country;
	private boolean isAdministrator;
	private Department department;
	private Credential credentials;

	// Optional fields
	private boolean patientDataEntry;
	private boolean selectDepartment;
	private String secondPhone_CountryCode;
	private String secondPhone_AreaCode;
	private String secondPhone_Number; 
	private boolean archiveTransmission;

	public enum UserType {
		PHYSICIAN("Physician"), ALLIED_PROFESSIONAL("Allied Professional"), ASSISTANT("Assistant");
		
		@Getter
		private String usrType;

		private UserType(String userType) {
			this.usrType = userType;
		}
		
		public static UserType getEnum(String userType) {
			for (UserType type : UserType.values()) {
				if (type.getUsrType().equals(userType)) {
					return type;
				}
			}
			return null;
		}
	}
	
	public enum Department{
		NODATA(""), CARDIOLOGY("Cardiology"), INTENSIVE_CARE("Intensive Care");
		
		private String departmentType;

		private Department(String departmentType) {
			this.departmentType = departmentType;
		}

		public String getDepartmentType() {
			return this.departmentType;
		}
	}
	
	public enum Credential{
		NODATA(""), DO("DO"), LPN("LPN"), LVN("LVN"), MD("MD"), MSN("MSN");
		
		private String credentialType;

		private Credential(String credentialType) {
			this.credentialType = credentialType;
		}

		public String getCredentialType() {
			return this.credentialType;
		}
	}

	/**
	 * Default constructor
	 */
	public ClinicUser() {
		firstName = "FN" + CommonUtils.randomAlphanumericString(6);
		lastName = "LN" + CommonUtils.randomAlphanumericString(6);
		userId = "uid" + CommonUtils.randomAlphanumericString(5);
		password = "pwd" + CommonUtils.randomAlphanumericString(5).toUpperCase()
				+ String.valueOf(CommonUtils.getRandomNumber(0, 99));
		confirmPwd = password;
		userType = UserType.PHYSICIAN;
		mainPhone_AreaCode = String.valueOf(CommonUtils.getRandomNumber(11, 99));
		mainPhone_Number = String.valueOf(CommonUtils.getRandomNumber(1111111, 9999999));
		securityStamp = CommonUtils.randomAlphanumericString(6);
		email = CommonUtils.generateRandomEmail();
		alternateEmail = email;
		mainPhone_CountryCode = String.valueOf(CommonUtils.getRandomNumber(11, 99));
	}

	/**
	 * Parameterized constructor with mandate fields only
	 */
	public ClinicUser(String firstName, String lastName, String userId, String password, String confirmPwd, UserType userType, String securityStamp, String email, String alternateEmail, String mainPhone_CountryCode, String mainPhone_AreaCode, String mainPhone_Number) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.userId = userId;
		this.password = password;
		this.confirmPwd = confirmPwd;
		this.userType = userType;
		this.email = email;
		this.alternateEmail = alternateEmail;
		this.securityStamp = securityStamp;
		this.mainPhone_AreaCode = mainPhone_AreaCode;
		this.mainPhone_CountryCode = mainPhone_CountryCode;
		this.mainPhone_Number = mainPhone_Number;
	}
}
