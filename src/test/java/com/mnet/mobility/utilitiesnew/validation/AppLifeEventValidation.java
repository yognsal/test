package com.mnet.mobility.utilities.validation;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.AssertionLevel;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

/**
 * Provides convenience functions to check App Life Event (ALE) routing and perform database validation.
 * @author Arya Biswas
 * @version Fall 2023
 */
public interface AppLifeEventValidation {
	
	/**
	 * Convenience function to perform all validation
	 * @return true if and only if all ALE validation was successful.
	 */
	public default boolean validateAppLifeEvent(PatientApp patientApp, AppLifeEvent appLifeEvent) {
		boolean isAleStored = isAppLifeEventStored(patientApp, appLifeEvent);
		
		TestReporter report = patientApp.getCurrentTest().getReport();
		
		report.assertCondition(isAleStored, true,
				TestStep.builder().assertionLevel(AssertionLevel.SOFT)
				.message("App life event is stored in database with patient_app_ale_id: " + appLifeEvent.getPatientAppALEId())
				.failMessage("App life event could not be found in applifeevents.app_life_events with patient_app_ale_id: " + appLifeEvent.getPatientAppALEId())
				.build());
		
		return isAleStored;
	}
	
	/**
	 * @return true if and only if app life event is stored in the applifeevent DB table.
	 */
	public default boolean isAppLifeEventStored(PatientApp patientApp, AppLifeEvent appLifeEvent) {
		return (PatientAppDBUtilities.getAppLifeEventById(patientApp, appLifeEvent).size() != 0) ? true : false;
	}
	
	/**
	 * Determines if any ALE messages of a given type were routed 
	 * via the corresponding service bus queue within a given timespan.
	 */
	public default boolean isAppLifeEventRouted(Class<? extends AppLifeEvent> aleType, long startTime, long endTime) {
		// TODO: Validate via Microsoft metrics API
		return true;
	}
}
