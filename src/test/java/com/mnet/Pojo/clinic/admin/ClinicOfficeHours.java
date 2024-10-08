package com.mnet.pojo.clinic.admin;

import lombok.Data;

/**
 * POJO for clinic admin -> clinic hours/ holidays -> Office Hours
 * @author NAIKKX12
 *
 */
@Data
public class ClinicOfficeHours {
	private OfficeDay officeDay;
	private String openingTime;
	private OfficeTimeUnit openingTimeUnit;
	private String closingTime;
	private OfficeTimeUnit closingTimeUnit;
	
	public enum OfficeDay {
		MONDAY ("Monday"), TUESDAY ("Tuesday"), WEDNESDAY ("Wednesday"), THURSDAY ("Thursday"), FRIDAY ("Friday"), SATURDAY ("Saturday"), SUNDAY ("Sunday");
		
		private String day;

		private OfficeDay(String day) {
			this.day = day;
		}

		public String getOfficeDay() {
			return this.day;
		}
	}
	
	public enum OfficeTimeUnit {
		AM ("AM"), PM ("PM");
		
		private String unit;

		private OfficeTimeUnit(String unit) {
			this.unit = unit;
		}

		public String getOfficeTimeUnit() {
			return this.unit;
		}
	}
	
	/**
	 * Default constructor
	 */
	public ClinicOfficeHours() {
		officeDay = OfficeDay.MONDAY;
		openingTime = "08:00";
		openingTimeUnit = OfficeTimeUnit.AM;
		closingTime = "05:00";
		closingTimeUnit = OfficeTimeUnit.PM;
	}
	
	/**
	 * Parameterized constructor with mandate fields only
	 */
	public ClinicOfficeHours(OfficeDay officeDay, String openingTime, OfficeTimeUnit openingTimeUnit, String closingTime, OfficeTimeUnit closingTimeUnit) {
		this.officeDay = officeDay;
		this.openingTime = openingTime;
		this.openingTimeUnit = openingTimeUnit;
		this.closingTime = closingTime;
		this.closingTimeUnit = closingTimeUnit;
	}
}