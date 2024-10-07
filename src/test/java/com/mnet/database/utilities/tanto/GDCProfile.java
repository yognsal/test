package com.mnet.middleware.utilities.tanto;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoAttributeCategory;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.GDCSwitch;

/**
 * Business logic validation for Tanto GDC subprofile.
 * @author Arya Biswas
 * @version Summer 2023
 * @implNote Only uses system default values as per Auto6091 (PCN00027349).
 */
public class GDCProfile extends TantoPatientSubProfile {

	private static final String GDC_PROFILE_CODE = "506";
	
	public GDCProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, GDC_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		GDCSwitch currentSwitch = getGDCSwitch(profileSwitch);
		
		String responseSwitchValue = null;
		
		switch (currentSwitch) {
			case GS_Interval:
				responseSwitchValue = driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.GDC, TantoAttributeCategory.GENERATE_SCHEDULE, currentSwitch.toString());
				break;
			case US_Interval:
				responseSwitchValue = driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.GDC, TantoAttributeCategory.UPLOAD_SCHEDULE, currentSwitch.toString());
				break;
			default:
				responseSwitchValue = driver.getXMLElement(profileResponse, TantoPayloadProfileType.GDC, currentSwitch.toString());
				break;
		}
		
		return reportSwitchValidation(profileSwitch, responseSwitchValue, getSwitchOverride(profileSwitch));
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		return GDCSwitch.values();
	}
	
	/*
	 * Helper functions
	 */
	
	private GDCSwitch getGDCSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof GDCSwitch)) {
			throw new RuntimeException("Invalid switch for FollowUp profile: " + profileSwitch.toString());
		}
		
		return (GDCSwitch) profileSwitch;
	}
}
