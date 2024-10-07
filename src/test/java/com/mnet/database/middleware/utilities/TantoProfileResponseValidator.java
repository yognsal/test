package com.mnet.middleware.utilities;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.utilities.XMLData;
import com.mnet.middleware.utilities.tanto.AlertControlsProfile;
import com.mnet.middleware.utilities.tanto.ComProfile;
import com.mnet.middleware.utilities.tanto.DeviceCheckProfile;
import com.mnet.middleware.utilities.tanto.FollowUpProfile;
import com.mnet.middleware.utilities.tanto.GDCProfile;
import com.mnet.middleware.utilities.tanto.MEDProfile;
import com.mnet.middleware.utilities.tanto.MaintenanceProfile;
import com.mnet.middleware.utilities.tanto.SpareProfile;
import com.mnet.middleware.utilities.tanto.SystemDataProfile;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;
import com.mnet.middleware.utilities.tanto.UnpairedProfile;
import com.mnet.pojo.xml.TantoComProfileResponse;
import com.mnet.pojo.xml.TantoPatientProfileResponse;

import lombok.Setter;

/**
 * Validates Tanto patient profile or ComProfile response based on overrides and applicable business logic. 
 * @version Spring 2023
 * @author Arya Biswas
 */
public class TantoProfileResponseValidator {
	
	private FrameworkLog log;
	private TantoDriver driver;
	private DatabaseConnector database;
	private TestReporter report;
	
	@Setter
	private boolean softAssert;
	@Setter
	private boolean reportFailure;
	
	private TantoSubProfile targetSubProfile;
	
	/**
	 * Initializes profile validation object. Uses default behavior of only reporting failures via soft assertion (valid switches are excluded from report).
	 * To customize the reporting behavior, use TantoProfileResponseValidator(TantoDriver, DatabaseConnector, TestReporter, FrameworkLog, boolean, boolean) instead.
	 * @param tantoDriver Tanto driver utility from test class.
	 * @param databaseConnector Database connection utility from test class.
	 * @param testReporter Reporting object from test class.
	 * @param frameworkLog Logger from test class.
	 */
	public TantoProfileResponseValidator(FrameworkLog frameworkLog, TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter) {
		this(frameworkLog, tantoDriver, databaseConnector, testReporter, true, true);
	}
	
	/**
	 * Initializes profile validation object with custom reporting behavior.
	 * @param tantoDriver Tanto driver utility from test class.
	 * @param databaseConnector Database connection utility from test class.
	 * @param testReporter Reporting object from test class.
	 * @param frameworkLog Logger from test class.
	 * @param useSoftAssert If true, triggers soft assertion on profile switch mismatch. If false, reports switch mismatches as a warning, but takes no further action.
	 * @param onlyReportFailures If true, only adds switch mismatches to report. If false, adds both switch matches and mismatches to report.
	 */
	public TantoProfileResponseValidator(FrameworkLog frameworkLog, TantoDriver tantoDriver, DatabaseConnector databaseConnector,
			TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		log = frameworkLog;
		driver = tantoDriver;
		database = databaseConnector;
		report = testReporter;
		softAssert = useSoftAssert;
		reportFailure = onlyReportFailures;
	}
	
	/**
	 * Represents a subprofile in a Tanto profile response.
	 * To represent all subprofiles applicable to the profile response (or ComProfile / error response), use ALL.
	 */
	public enum TantoSubProfileType {
		ALL,
		SYSTEM_DATA(SystemDataProfile.class),
		FOLLOW_UP(FollowUpProfile.class),
		DEVICE_CHECK(DeviceCheckProfile.class),
		ALERT_CONTROLS(AlertControlsProfile.class),
		GDC(GDCProfile.class),
		MAINTENANCE(MaintenanceProfile.class),
		MED(MEDProfile.class),
		UNPAIRED(UnpairedProfile.class),
		SPARE(SpareProfile.class),
		COMPROFILE(ComProfile.class);
		
		private Class<? extends TantoSubProfile> profileClass;
		
		private TantoSubProfileType() { }
		
		private TantoSubProfileType(Class<? extends TantoSubProfile> profile) {
			profileClass = profile;
		}
		
		public Class<? extends TantoSubProfile> getProfileClass() {
			return profileClass;
		}
	}
	
	/**
	 * Validates all switches in one or more subprofiles of a Tanto profile response based on business logic (overrides, DB values, etc).
	 */
	public boolean validateSubProfile(String deviceModel, String deviceSerial, String transmitterSWVersion, String profileVersion, 
			XMLData profileResponse, TantoSubProfileType... subprofiles) {
		List<TantoSubProfileType> profileList = new ArrayList<TantoSubProfileType>(Arrays.asList(subprofiles));
		
		if (profileList.contains(TantoSubProfileType.ALL)) {
			if (profileResponse instanceof TantoPatientProfileResponse) {
				profileList = new ArrayList<TantoSubProfileType>(Arrays.asList(TantoSubProfileType.values()));
				
				profileList.remove(TantoSubProfileType.ALL);
				profileList.remove(TantoSubProfileType.COMPROFILE);
			} else if (profileResponse instanceof TantoComProfileResponse) {
				profileList = new ArrayList<TantoSubProfileType>(Arrays.asList(TantoSubProfileType.COMPROFILE));
			}
 		}
		
		boolean isValidResponse = true;
		
		for (TantoSubProfileType subprofile : profileList) {
			setTargetSubProfile(subprofile.getProfileClass(), deviceModel, deviceSerial, transmitterSWVersion, profileVersion, profileResponse);
			
			isValidResponse = targetSubProfile.validateAllSwitches() ? isValidResponse : false;
		}
		
		return isValidResponse;
	}
	
	/**
	 * Validates specified switch(es) from Tanto profile response based on business logic (overrides, DB values, etc).
	 */
	public boolean validateProfileSwitch(String deviceModel, String deviceSerial, String transmitterSWVersion, String profileVersion, 
			XMLData profileResponse, TantoProfileSwitch... profileSwitches) {
		boolean validationResult = true;
		
		for (TantoProfileSwitch profileSwitch : profileSwitches) {
			setTargetSubProfile(profileSwitch.getSubprofile(), deviceModel, deviceSerial, transmitterSWVersion, profileVersion, profileResponse);
		
			validationResult = validationResult && targetSubProfile.validateSwitch(profileSwitch);
		}
		
		return validationResult;
	}
	
	/*
	 * Helper functions
	 */
	
	private void setTargetSubProfile(Class<? extends TantoSubProfile> profileClass, String deviceModel, String deviceSerial, String transmitterSWVersion, String profileVersion, XMLData profileResponse) {		
		try {
			targetSubProfile = (TantoSubProfile) profileClass.getConstructors()[0].newInstance(driver, database, report, softAssert, reportFailure);
		} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
			String err = "Failed to construct new instance of subprofile: " + profileClass.toString() 
							+ " \n Cause: " + e.getMessage();
			log.error(err);
			throw new RuntimeException(err);
		}
		
		targetSubProfile.setDeviceModel(deviceModel);
		targetSubProfile.setDeviceSerial(deviceSerial);
		targetSubProfile.setTransmitterSWVersion(transmitterSWVersion);
		targetSubProfile.setProfileVersion(profileVersion);
		targetSubProfile.setProfileResponse(profileResponse);
	}
}
