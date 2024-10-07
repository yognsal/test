package com.mnet.middleware.utilities.tanto;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoAttributeCategory;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.DatabaseField;
import com.mnet.middleware.utilities.tanto.enums.DeviceCheckSwitch;

/**
 * Business logic validation for Tanto Device Check subprofile.
 * @author Arya Biswas
 * @version Summer 2023
 */
public class DeviceCheckProfile extends TantoPatientSubProfile {
		
	private static final String DEVICE_CHECK_PROFILE_CODE = "505";
	
	public DeviceCheckProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, DEVICE_CHECK_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		DeviceCheckSwitch currentSwitch = getDeviceCheckSwitch(profileSwitch);
		
		String responseSwitchValue = currentSwitch.isXmlAttribute() ? 
				driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.DEVICE_CHECK, TantoAttributeCategory.GENERATE_SCHEDULE, currentSwitch.toString()) :
				driver.getXMLElement(profileResponse, TantoPayloadProfileType.DEVICE_CHECK, currentSwitch.toString());
		
		return reportSwitchValidation(profileSwitch, responseSwitchValue, getSwitchOverride(profileSwitch));
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		return DeviceCheckSwitch.values();
	}
	
	@Override
	protected String getSwitchOverride(TantoProfileSwitch profileSwitch) {
		String override = super.getSwitchOverride(profileSwitch);
		
		// Attributes / manual overrides should be interpreted as-is
		if ((attributeValue != null)
				|| (modOverride != null)
				|| (transmitterOverride != null)
				|| (deviceOverride != null)) {
			return override;
		}
		
		DeviceCheckSwitch currentSwitch = getDeviceCheckSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case UNSCH_DCHK_PREF:
				override = (defaultValue == null) ? interpretIfLockout(override) : override;
				break;
			case SCHED_DCHK_PREF:
				if (!isDeviceCapable(DatabaseField.RF_CAPABLE_FLG)) {
					return "Disable";
				}
				break;
			default:
				break;
		}
		
		return interpretIfFlag(override);
	}

	@Override
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		DeviceCheckSwitch currentSwitch = getDeviceCheckSwitch(profileSwitch);
		
		String defaultValue = super.getAttributeValue(profileSwitch);
		
		switch (currentSwitch) {
			case GS_TimeOfEvent:
				return parseGSTimeOfEvent(defaultValue);
			default:
				return defaultValue;
		}
	}
	
	/*
	 * Helper functions
	 */
	
	private DeviceCheckSwitch getDeviceCheckSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof DeviceCheckSwitch)) {
			throw new RuntimeException("Invalid switch for DeviceCheck profile: " + profileSwitch.toString());
		}
		
		return (DeviceCheckSwitch) profileSwitch;
	}
	
	/**Converts system default for GS_TimeOfEvent from local clinic time to UTC.
	 * @param systemDefault DB default local clinic time in the format HHmm.*/
	private String parseGSTimeOfEvent(String systemDefault) {
		
		int hours = Integer.parseInt(systemDefault.substring(0, 2));
		int minutes = Integer.parseInt(systemDefault.substring(2));
		
		return getUTCTime(hours, minutes);
	}
}
