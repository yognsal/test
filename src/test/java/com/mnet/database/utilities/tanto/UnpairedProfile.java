package com.mnet.middleware.utilities.tanto;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.UnpairedSwitch;

public class UnpairedProfile extends TantoPatientSubProfile {
	
	private static final String UNPAIRED_PROFILE_CODE = "1384";
	
	public UnpairedProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, UNPAIRED_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		String responseSwitchValue = driver.getXMLElement(profileResponse, TantoPayloadProfileType.UNPAIRED, profileSwitch.toString());

		return reportSwitchValidation(profileSwitch, responseSwitchValue, getSwitchOverride(profileSwitch));
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		return UnpairedSwitch.values();
	}
	
	@Override
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		return null;
	}
	
	@Override
	protected String getMODDefault(TantoProfileSwitch profileSwitch) {
		return getModelExclusion();
	}
	
}
