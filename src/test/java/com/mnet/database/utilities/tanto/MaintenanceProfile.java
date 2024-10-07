package com.mnet.middleware.utilities.tanto;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoAttributeCategory;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.MaintenanceSwitch;

/**
 * Business logic validation for Tanto Maintenance subprofile.
 * @author Arya Biswas
 * @version Summer 2023
 */
public class MaintenanceProfile extends TantoPatientSubProfile {
	
	private static final String MAINTENANCE_PROFILE_CODE = "507";
	private static final String US_INTERVAL_ER_DEFAULT = "336";
	
	public MaintenanceProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, MAINTENANCE_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		MaintenanceSwitch currentSwitch = getMaintenanceSwitch(profileSwitch);
		
		String responseSwitchValue = currentSwitch.isXmlAttribute() ? 
				driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.MAINTENANCE, TantoAttributeCategory.UPLOAD_SCHEDULE, currentSwitch.toString()) :
				driver.getXMLElement(profileResponse, TantoPayloadProfileType.MAINTENANCE, currentSwitch.toString());
		
		return reportSwitchValidation(profileSwitch, responseSwitchValue, getSwitchOverride(profileSwitch));
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		return MaintenanceSwitch.values();
	}
	
	@Override
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		MaintenanceSwitch currentSwitch = getMaintenanceSwitch(profileSwitch);
		
		if (currentSwitch != MaintenanceSwitch.US_Interval) {
			return null;
		}
		
		return (hasMODTransmitter()) ? 
				US_INTERVAL_ER_DEFAULT : super.getAttributeValue(profileSwitch);
	}
	
	/*
	 * Helper functions
	 */
	
	private MaintenanceSwitch getMaintenanceSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof MaintenanceSwitch)) {
			throw new RuntimeException("Invalid switch for FollowUp profile: " + profileSwitch.toString());
		}
		
		return (MaintenanceSwitch) profileSwitch;
	}
	
	
}
