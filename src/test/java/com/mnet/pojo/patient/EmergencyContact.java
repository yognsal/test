package com.mnet.pojo.patient;

import com.mnet.framework.utilities.CommonUtils;

import lombok.Data;

@Data
public class EmergencyContact {
	private boolean sendDirectAlerts;
	private String countryCode;
	private String areaCityCode;
	private String phoneNumber;
	private String firstName;
	private String lastName;
	private String address1;
	private String address2;
	private String address3;
	private Countries country;
	private String state;
	private String city;
	private String zipCode;
	
	public enum Countries{
		USA("USA"), GERMANY("Germany"), MONACO("Monaco"),LIECHTENSTEIN("Liechtenstein");
		                                               
		
		private String country;

		private Countries(String country) {
			this.country = country;
		}

		public String getCountry() {
			return this.country;
		}
	}
	
	public enum States{
		ARIZONA("ARIZONA"), CALIFORNIA("CALIFORNIA"), FLORIDA("FLORIDA");
		private String state;

		private States(String state) {
			this.state = state;
		}

		public String getState() {
			return this.state;
		}
	}
	
	public EmergencyContact() {
		sendDirectAlerts = false;
		phoneNumber = String.valueOf(CommonUtils.getRandomNumber(1000000, 9999999));
		firstName = CommonUtils.randomStringSimple(CommonUtils.getRandomNumber(1, 29));
		lastName = CommonUtils.randomStringSimple(CommonUtils.getRandomNumber(1, 29));
		address1 = CommonUtils.randomStringSimple(CommonUtils.getRandomNumber(1, 99));
		address2 = CommonUtils.randomStringSimple(CommonUtils.getRandomNumber(1, 99));
		address3 = CommonUtils.randomStringSimple(CommonUtils.getRandomNumber(1, 99));
		city = CommonUtils.randomStringSimple(CommonUtils.getRandomNumber(1, 29));
		zipCode =String.valueOf(CommonUtils.getRandomNumber(10000, 99999)) + "-"
				+ String.valueOf(CommonUtils.getRandomNumber(1000, 9999));
	}

	public EmergencyContact(boolean sendDirectAlerts, String areaCityCode, String phoneNumber, String firstName,
			String lastName, String address1, String address2, String address3, String city, String zipCode) {
		this.sendDirectAlerts = sendDirectAlerts;
		this.areaCityCode = areaCityCode;
		this.phoneNumber = phoneNumber;
		this.firstName = firstName;
		this.lastName = lastName;
		this.address1 = address1;
		this.address2 = address2;
		this.address3 = address3;
		this.city = city;
		this.zipCode = zipCode;
	}
}
