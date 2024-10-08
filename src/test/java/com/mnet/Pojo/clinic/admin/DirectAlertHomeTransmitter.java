package com.mnet.pojo.clinic.admin;

import lombok.Data;

/**
 * pojo class for Clinic Administration -> Home Transmitter page
 * 
 * @author NAIKKX12
 *
 */
@Data
public class DirectAlertHomeTransmitter {

	private String contactTextMsg;
	private String contactMainPhoneAreaCode;
	private ContactType medicalTeamContact;
	private AlertType medicalTeamAlert;
	
	public enum ContactType {
		EMAIL("Email"), TEXT_MESSAGE("Text message"), PHONE("Phone"), NONE("None");

		private String contactType;

		ContactType(String contactType) {
			this.contactType = contactType;
		}

		public String getContactType() {
			return contactType;
		}
	}
	
	public enum AlertType {
		OFF("Off"), RED("Red Alerts"), YELLOW("Yellow Alerts"), BOTH("Both Red and Yellow Alerts");

		private String alertType;

		AlertType(String alertType) {
			this.alertType = alertType;
		}

		public String getAlertType() {
			return alertType;
		}
	}
}