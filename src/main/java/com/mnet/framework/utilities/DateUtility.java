package com.mnet.framework.utilities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;

import lombok.Getter;

/**
 * Utility to modify the date and time as per the requirement
 * 
 * @version May 2023
 * @author Rishab Kotwal (KOTWARX2)
 */
public class DateUtility {

	private static String PAYLOAD_ZIP_DATE_FORMAT = FrameworkProperties.getProperty("PAYLOAD_ZIP_DATE_FORMAT");
	private static String WEBAPP_DEFAULT_DATE_FORMAT = FrameworkProperties.getProperty("WEBAPP_DEFAULT_DATE_FORMAT");
	private static String DATABASE_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	public enum Timezones {
		PST("America/Los_Angeles"), IST("Asia/Kolkata"), BST("Europe/London"), UTC("UTC");
		
		private String timezone;
		
		private Timezones(String timezone) {
			this.timezone = timezone;
		}
		
		public String getTimezone() {
			return this.timezone;
		}
	}
	
	public enum DateTimeFormat {
		WEBAPP (WEBAPP_DEFAULT_DATE_FORMAT),
		PAYLOAD (PAYLOAD_ZIP_DATE_FORMAT),
		DATABASE (DATABASE_DATE_TIME_FORMAT);
		
		@Getter
		private String format;
		
		private DateTimeFormat(String format) {
			this.format = format;
		}
	}

	/**
	 * Function to modify current date to a future date.
	 * 
	 * @param days - User can pass by how many days they need to add to current date
	 * @return modified date
	 * @author KOTWARX2
	 */
	public static String modifiedDate(int days) {
		return modifiedDate(days, null);
	}
	
	public static String modifiedDate(int days, String dateFormat) {
		LocalDate dateObj = LocalDate.now();
		LocalDate newDate;
		newDate = dateObj.plusDays((long) days);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat == null ? WEBAPP_DEFAULT_DATE_FORMAT : dateFormat);
		String date = newDate.format(formatter);

		return date;
	}

	/**
	 * Function to modify current date to a past date.
	 * 
	 * @param days - User can pass by how many days they need to subtract from
	 *             current date
	 * @return modified date
	 * @author KOTWARX2
	 */
	public static String earlierDate(int days) {
		LocalDate dateObj = LocalDate.now();
		LocalDate newDate;
		newDate = dateObj.minusDays((long) days);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(WEBAPP_DEFAULT_DATE_FORMAT);
		String date = newDate.format(formatter);

		return date;
	}

	/**
	 * Function to change the date format to MM-DD-YYYY.
	 * 
	 * @param date - User needs to pass the date that needs to be re-formatted
	 * @return modified date
	 * @author KOTWARX2
	 */
	@Deprecated
	// Function call can be replaced with changeDateFormat(date, null, null)
	public static String changeDateFormat(String date) {
		Locale.setDefault(Locale.US);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(PAYLOAD_ZIP_DATE_FORMAT);
		DateTimeFormatter dtf_updated = DateTimeFormatter.ofPattern(WEBAPP_DEFAULT_DATE_FORMAT);
		LocalDateTime localDateTime = LocalDate.parse(date, dtf).atStartOfDay();
		String result = localDateTime.format(dtf_updated);

		return result;
	}
	
	/**
	 * Function to change date format based on source and target mentioned
	 */
	public static String changeDateFormat(String date, String sourceDateFormat, String targetDateformat) {
		Locale.setDefault(Locale.US);
		DateTimeFormatter dtf = sourceDateFormat == null ? DateTimeFormatter.ofPattern(PAYLOAD_ZIP_DATE_FORMAT) : DateTimeFormatter.ofPattern(sourceDateFormat);
		DateTimeFormatter dtf_updated = targetDateformat == null ? DateTimeFormatter.ofPattern(WEBAPP_DEFAULT_DATE_FORMAT) : DateTimeFormatter.ofPattern(targetDateformat);
		LocalDateTime localDateTime = LocalDate.parse(date, dtf).atStartOfDay();
		String result = localDateTime.format(dtf_updated);

		return result;
	}

	/**
	 * Function to change the time from 24hr format to 12hr format
	 * 
	 * @param time - User needs to pass the time that needs to be re-formatted
	 * @return modified time
	 * @author KOTWARX2
	 */
	public static String changeTimeFormat(String time) {

		String[] lst = time.split("-");
		String formattedTime = String.join(":", lst[0], lst[1]);
		String updatedTime = LocalTime.parse(formattedTime, DateTimeFormatter.ofPattern("HH:mm"))
				.format(DateTimeFormatter.ofPattern("hh:mm a"));

		return updatedTime;

	}

	/**
	 * Compare two dates using MM-DD-YYYY format (can be specified in properties
	 * file)
	 * 
	 * @param date1
	 * @param date2
	 * @param log
	 * @return
	 */
	public static boolean compareDates(String date1, String date2, FrameworkLog log) {
		return compareDates(date1, date2, null, log);
	}

	/**
	 * Compare two dates using MM-DD-YYYY format (can be specified in properties
	 * file)
	 */
	public static boolean compareDates(String date1, String date2, String dateFormat, FrameworkLog log) {
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat == null ? WEBAPP_DEFAULT_DATE_FORMAT : dateFormat);
		try {
			Date firstDate = formatter.parse(date1);
			Date secondDate = formatter.parse(date2);
			if (firstDate.compareTo(secondDate) >= 0) {
				return true;
			}
			return false;
		} catch (ParseException e) {
			log.error("DateParse error occured");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Convert the UTC date time into passed Timezone
	 */
	public static String convertUTCToTimezone(Timezones timezone, String utcTime) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATABASE_DATE_TIME_FORMAT);
		LocalDateTime ldt = LocalDateTime.parse(utcTime , formatter);
		ZoneId zoneId = ZoneId.of(timezone.getTimezone());
		ZonedDateTime zdt = ldt.atZone( zoneId );
		String output = zdt.format( DateTimeFormatter.ISO_LOCAL_DATE_TIME );
		//Default UTC format is <date>T<time>. Removing "T" to match with the format in database
		output = output.replace("T", " ");
		return output;
	}
	
	/**
	 * Converts Date Time provided in format yyyy-MM-dd HH:mm:ss to any timezone passed in as parameter.
	 * TODO: handle more date time formats
	 */
	public static String convertDateTimeToAnyTimezone(Timezones timezone, String dateTime, FrameworkLog log) {
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date date = formatter.parse(dateTime);
			TimeZone obj = TimeZone.getTimeZone(timezone.getTimezone());
			formatter.setTimeZone(obj);
			return formatter.format(date);
		} catch (ParseException e) {
			log.error("DateParse error occured");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
		
	}
	
	/**
	 * Function to get past dates (from current date) in YYYY-MM-DD format which is accepted by database.
	 */
	public static String getPastDates(int daysToSubtract) {
		Instant now = Instant.now();
		Instant pastDate = now.minus(daysToSubtract, ChronoUnit.DAYS);
		String date = pastDate.toString();

		return date.split("T")[0];
	}
	
	/**
	 * Function to convert date passed in as parameter into words (e.g. Tuesday, November 3, 2023)
	 */
	public static String convertDateToWords(String date) {
		return convertDateToWords(date, null);
	}
	
	/**
	 * Function to convert date passed in as parameter into words (e.g. Tuesday,
	 * November 3, 2023)
	 * @param dateFormat - date format to consider if specified
	 */
	public static String convertDateToWords(String date, String dateFormat) {
		Locale.setDefault(Locale.US);
		if (dateFormat == null) {
			dateFormat = WEBAPP_DEFAULT_DATE_FORMAT;
		}
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat);
		DateTimeFormatter dtf_updated = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
		LocalDateTime localDateTime = LocalDate.parse(date, dtf).atStartOfDay();
		String day = localDateTime.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
		String result = localDateTime.format(dtf_updated);
		return day + ", " + result;
	}
	
	//Date to verify, before date and after date
	public static boolean isDateInRange(String date, String startDate, String endDate) {
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern(WEBAPP_DEFAULT_DATE_FORMAT);
		LocalDate dateStartRange = LocalDate.parse(startDate, dtf);
		LocalDate dateEndRange = LocalDate.parse(endDate, dtf);
		LocalDate dateToVerify = LocalDate.parse(date, dtf);
		
		return dateToVerify.isAfter(dateStartRange) && dateToVerify.isBefore(dateEndRange);
	}
	
	/**
	 * Change epoch seconds to simple date format
	 * @param onlyDate if true then consider date only else date & time both
	 */
	public static String changeEpochToDate(long epochDate, boolean onlyDate) {
		Date date = new Date(epochDate);
		SimpleDateFormat sdf = onlyDate ? new SimpleDateFormat("EEEE, MMMM dd, yyyy")
				: new SimpleDateFormat("EEEE, MMMM dd, yyyy h:mm a");
		return sdf.format(date);
	}
	
	/** change date to epoch in ms */
	public static long changeDateToEpoch(String date, DateTimeFormat dateFormat, FrameworkLog log) {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat.getFormat() == null ? 
					WEBAPP_DEFAULT_DATE_FORMAT : dateFormat.getFormat());
			return formatter.parse(date).getTime();
		} catch (ParseException e) {
			log.error("DateParse error occured");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Function to convert Date(String format) to Date Format
	 */
	public static LocalDate convertStringToDate(String datex,FrameworkLog log) {
		try {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(WEBAPP_DEFAULT_DATE_FORMAT);
			LocalDate date = LocalDate.parse(datex, formatter);
			return date;
		}
		catch (Exception e) {
			log.error("DateParse error occured");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}		
	}
}
