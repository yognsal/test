package com.mnet.middleware.utilities.tanto;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.AssertionLevel;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.framework.utilities.XMLData;
import com.mnet.middleware.utilities.TantoDriver;

import lombok.Setter;

/**
 * Represents a subprofile in a Tanto profile response (including ComProfile).
 * @version Spring 2023
 * @author Arya Biswas
 */
public abstract class TantoSubProfile {

	protected TantoDriver driver;
	protected DatabaseConnector database;
	protected TestReporter report;
	protected boolean softAssert;
	protected boolean onlyReportFailure;
	
	@Setter
	protected String deviceSerial;
	@Setter
	protected String deviceModel;
	@Setter
	protected String transmitterSWVersion;
	@Setter
	protected String profileVersion;
	@Setter
	protected XMLData profileResponse;
	
	public TantoSubProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		driver = tantoDriver;
		database = databaseConnector;
		report = testReporter;
		softAssert = useSoftAssert;
		onlyReportFailure = onlyReportFailures;
	}
	
	/**
	 * Validates the given switch according to the applicable overrides / business logic.
	 */
	public abstract boolean validateSwitch(TantoProfileSwitch profileSwitch);
	
	/**
	 * Defines switch override behavior as per business logic. 
	 */
	protected abstract String getSwitchOverride(TantoProfileSwitch profileSwitch);
	
	/**
	 * Returns system default value of switch as defined in database.
	 */
	protected abstract String getDefaultSwitchValue(TantoProfileSwitch profileSwitch);
	
	/**
	 * Returns an array of all switches represented in the given subprofile.
	 */
	protected abstract TantoProfileSwitch[] getAllSwitches();
	
	/**
	 * Validates all switches in the given subprofile using the applicable overrides / business logic.
	 */
	public boolean validateAllSwitches() {
		TantoProfileSwitch[] allSwitches = getAllSwitches();
		
		boolean subprofileValid = true;
		
		for (TantoProfileSwitch currentSwitch : allSwitches) {
			subprofileValid = validateSwitch(currentSwitch) ? subprofileValid : false;
		}
		
		return subprofileValid;
	}
	
	/**
	 * Adds evaluation of switch value to test report.
	 */
	protected boolean reportSwitchValidation(TantoProfileSwitch profileSwitch, String responseSwitchValue, String expectedSwitchValue) {	
		boolean isSwitchValid = responseSwitchValue.equals(expectedSwitchValue);
		
		String switchName = profileSwitch.toString();
		String subprofileName = profileSwitch.getClass().getSimpleName();
		
		if ((onlyReportFailure && !isSwitchValid) || (!onlyReportFailure)) {
			String passMessage = "Profile response contains expected value for " + subprofileName + " " + switchName + " = " + expectedSwitchValue;
			String failureMessage = "Profile response did not contain expected value for " + subprofileName + " " + switchName + 
					" | Profile response: " + responseSwitchValue + " | Expected switch value: " + expectedSwitchValue;
			
			if (softAssert) {
				report.assertCondition(isSwitchValid, true, 
						TestStep.builder().assertionLevel(AssertionLevel.SOFT).message(passMessage).failMessage(failureMessage).build());
			} else {
				report.logStep(TestStep.builder().reportLevel(ReportLevel.WARNING).failMessage(failureMessage).build());
			}
		}
		
		return isSwitchValid;
	}
}
