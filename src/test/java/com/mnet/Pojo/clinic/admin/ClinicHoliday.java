package com.mnet.pojo.clinic.admin;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.utilities.CommonUtils;

import lombok.Data;

/**
 * Pojo class for Holiday on Clinic Hours/ Holiday page
 * 
 * @author NAIKKX12
 *
 */
@Data
public class ClinicHoliday {
	private String name;
	private String oooDate;
	private String backInOfficeDate;

	/**
	 * Default constructor
	 */
	public ClinicHoliday() {
		name = CommonUtils.randomAlphanumericString(5);
		SimpleDateFormat formatter = new SimpleDateFormat(FrameworkProperties.getProperty("WEBAPP_DEFAULT_DATE_FORMAT"));
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, CommonUtils.getRandomNumber(1, 10));
		oooDate = formatter.format(cal.getTime());
		cal.add(Calendar.DATE, CommonUtils.getRandomNumber(2, 10));
		backInOfficeDate = formatter.format(cal.getTime());
	}
	
	/**
	 * Parameterized constructor with mandate field only
	 */
	public ClinicHoliday(String name, String oooDate, String backInOfficeDate) {
		this.name = name;
		this.oooDate = oooDate;
		this.backInOfficeDate = backInOfficeDate;
	}
}