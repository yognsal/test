package com.mnet.pojo.patient;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.github.javafaker.Faker;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.DateUtility;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Patient {

	public static Patient empty() {
		return new Patient(null, null, null, null, null, null, null);
	}
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	Random rand = new Random();

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	Faker faker = new Faker();

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	DateUtility du = new DateUtility();

	//TODO: Create enum of device Models
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	List<String> unityDeviceList = Arrays.asList("3257-40", "3249-40", "3231-40", "3261-40", "3361-40","3235-40");

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	List<String> ngqDeviceList = Arrays.asList("Gallant™ VR, CDVRA500Q", "Gallant™ DR, CDDRA500Q",
			"Gallant™ HF, CDHFA500Q", "Entrant™ VR, CDVRA300Q", "Entrant™ DR, CDDRA300Q", "Entrant™ HF, CDHFA300Q");

	private String deviceType;
	private String deviceModelNum;
	private String deviceSerialNum;
	private String implantDate;
	private String firstName;
	private String middleName;
	private String lastName;
	private String dateOfBirth;
	private String patientId;
	private String addr1;
	private String addr2;
	private String addr3;
	private String language;
	private String location;
	private String city;
	private String cityCode;
	private String country;
	private String state;
	private String zipcode;
	private String countryCode;
	private String phoneNumber;
	private String email;
	private String transmitterSerialNum;
	private String newTransmitterSerialNum;
	private TransmitterModel transmitterModel;
	private Gender gender; 
	
	//Follow-Up
	private ScheduleType scheduleType;
	private String permanentSmartDate;
	private TransmitFrequency smartTransmitFrequency;
	private TransmitDuration smartTransmitDuration;
	private String temporarySmartDate;
	private TransmitFrequency temporaryTransmitFrequency;
	private TransmitDuration temporaryTransmitDuration;
	private Week week;
	private Month month;
	private List<String> manualDates;

	//Additional Fields
	private boolean setOrderTransmitter;
	private LeadChamber leadChamber;
	
	//NGQ Specific Transmitter Info
	private String transmitterSWVersion;
	private String transmitterRegDate;
	
	//Setting Clinician
	private boolean setClinician;
	private String clinicianFirstName;
	private String clinicianLastName;
	
	//Direct Call Parameters
	private String notifyStartTime;
	private String notifyEndTime;
	private String directCallMethod;
	
	public enum Country{
		CANADA("Canada"), GERMANY("Germany");
		
		private String country;

		private Country(String country) {
			this.country = country;
		}

		public String getCountry() {
			return this.country;
		}
	}
	

	public enum Gender{
		NONE(""), MALE("Male"), FEMALE("Female"), OTHER("Other"), UNKNOWN("Unknown");
		
		private String gender;

		private Gender(String gender) {
			this.gender = gender;
		}

		public String getGender() {
			return this.gender;
		}
	}
	
	public enum TransmitterModel {
		NONE(null), EX1150("EX1150-RF"), EX1100("EX1100-Inductive");

		private String transModel;

		private TransmitterModel(String transModel) {
			this.transModel = transModel;
		}

		public String getTransModel() {
			return this.transModel;
		}
	}
	
	public enum DeviceType {
		Unity("Unity"), NGQ("NGQ");

		private String deviceType;

		private DeviceType(String deviceType) {
			this.deviceType = deviceType;
		}

		public String getDeviceType() {
			return this.deviceType;
		}
	}
	
	public enum ScheduleType {
		SMART("SmartSchedule™ calendar"), MANUAL("Manual entry calendar"), NONE("None");

		private String schedule;

		private ScheduleType(String schedule) {
			this.schedule = schedule;
		}

		public String getSchedule() {
			return this.schedule;
		}
	}
	
	public enum TransmitFrequency{
		WEEK("Week"), MONTHS_6("6 Months"), MONTHS_3("3 Months"), MONTHS_9("9 Months"), MONTHS_12("12 Months"), MONTH("Month"), WEEK_2("2 Weeks"), WEEK_2_Temp("2 weeks"), WEEK_TEMP("week");
		
		private String transmitFrequency;

		private TransmitFrequency(String transmitFrequency) {
			this.transmitFrequency = transmitFrequency;
		}

		public String getTransmitFrequency() {
			return this.transmitFrequency;
		}
		
	}
	
	public enum TransmitDuration{
		MONTHS_6("6 Months"), MONTHS_9("9 Months"), MONTHS_12("12 Months"), MONTHS_2("2 Months"), MONTH_1("1 month");
		
		private String transmitDuration;

		private TransmitDuration(String transmitDuration) {
			this.transmitDuration = transmitDuration;
		}

		public String gettransmitDuration() {
			return this.transmitDuration;
		}
	}
	
	public enum LeadChamber{
		ATRIUM("Atrium"), VENTRICLE("Ventricle");
		
		private String leadChamber;

		private LeadChamber(String leadChamber) {
			this.leadChamber = leadChamber;
		}

		public String getLeadChamber() {
			return this.leadChamber;
		}
	}
	
	/**
	 * Enum to maintain all patient related status codes
	 */
	public enum StatusCode{
		NEXT("200"), OVERDUE("204");
		
		private String statusCode;

		private StatusCode(String statusCode) {
			this.statusCode = statusCode;
		}

		public String getStatusCode() {
			return this.statusCode;
		}
	}
	
	/**
	 * Enum to maintain all patient related permanent scheduled week
	 */
	public enum Week{
		FISRT("1st"), SECOND("2nd"), TRHIRD("3rd"), FOURTH("4th");
		
		private String week;

		private Week(String week) {
			this.week = week;
		}

		public String getPermanentWeek() {
			return this.week;
		}
	}
		
	/**
	 * Enum to maintain all patient related permanent scheduled Month
	 */
	public enum Month{
		JAN("January"), FEB("February"), MAR("March"), APR("April"), MAY("May"), JUNE("June");
		
		private String month;

		private Month(String month) {
			this.month = month;
		}

		public String getPermanentMonth() {
			return this.month;
		}
	}

	public Patient() {
		deviceType = DeviceType.Unity.getDeviceType();
		deviceModelNum = unityDeviceList.get(rand.nextInt(unityDeviceList.size()));
		deviceSerialNum = String.valueOf(rand.nextInt(9000000) + 1000000);
		firstName = faker.name().firstName();
		lastName = faker.name().lastName();
		dateOfBirth = DateUtility.earlierDate(9125);
		patientId = String.valueOf(rand.nextInt(900000) + 100000);
		email = CommonUtils.generateRandomEmail();
		countryCode = String.valueOf(CommonUtils.getRandomNumber(10, 99));
		cityCode = String.valueOf(CommonUtils.getRandomNumber(100, 999));
		phoneNumber = String.valueOf(CommonUtils.getRandomNumber(1000000, 9999999));
		transmitterSerialNum = String.valueOf(CommonUtils.getRandomNumber(10000000, 99999999));
		transmitterModel = TransmitterModel.EX1150;
	}

	public Patient(String deviceType, String deviceModelNum, String deviceSerialNum, String fName, String lName,
			String dob, String pid) {
		this.deviceType = deviceType;
		this.deviceModelNum = deviceModelNum;
		this.deviceSerialNum = deviceSerialNum;
		this.firstName = fName;
		this.lastName = lName;
		this.dateOfBirth = dob;
		this.patientId = pid;
	}

}
