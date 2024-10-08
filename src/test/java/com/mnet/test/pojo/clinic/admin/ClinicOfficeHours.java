package com.mnet.test.pojo.clinic.admin;

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
	private TimeUnit openingTimeUnit;
	private String closingTime;
	private TimeUnit closingTimeUnit;
	
	public enum OfficeDay {
		MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
	}
	
	public enum TimeUnit {
		AM, PM
	}
	
	public ClinicOfficeHours() {
		officeDay = OfficeDay.MONDAY;
		openingTime = "08:00";
		openingTimeUnit = TimeUnit.AM;
		closingTime = "05:00";
		closingTimeUnit = TimeUnit.PM;
	}
	
	public ClinicOfficeHours(OfficeDay officeDay, String openingTime, TimeUnit openingTimeUnit, String closingTime, TimeUnit closingTimeUnit) {
		this.officeDay = officeDay;
		this.openingTime = openingTime;
		this.openingTimeUnit = openingTimeUnit;
		this.closingTime = closingTime;
		this.closingTimeUnit = closingTimeUnit;
	}
}
