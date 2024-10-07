package com.mnet.middleware.utilities.tanto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.ComProfileSwitch;

public class ComProfile extends TantoSubProfile {

	private static final int COMPROFILE_NONLEGACY_PRODUCT_VERSION = Integer.parseInt(FrameworkProperties.getProperty("COMPROFILE_NONLEGACY_PRODUCT_VERSION"));
	private static final String BROKER_DOMAIN_OVERRIDE_9X = FrameworkProperties.getProperty("BROKER_DOMAIN_OVERRIDE_9X");
	private static final String NONLEGACY_APPLICATION_RELEASE = "9";
	private static final String LEGACY_APPLICATION_RELEASE = "6";
	
	private String applicationReleaseId;
	private Boolean is9xTransmitter;
	
	public ComProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		ComProfileSwitch currentSwitch = getComProfileSwitch(profileSwitch);
		
		String responseSwitchValue = driver.getXMLElement(profileResponse, TantoPayloadProfileType.COMPROFILE, currentSwitch.toString());
		
		return reportSwitchValidation(profileSwitch, responseSwitchValue, getSwitchOverride(profileSwitch));
	}
	
	@Override
	protected String getSwitchOverride(TantoProfileSwitch profileSwitch) {
		String patientOverride = getPatientOverride(profileSwitch);
		
		return (patientOverride != null) ? patientOverride : getDefaultSwitchValue(profileSwitch);
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		ComProfileSwitch[] switches = ComProfileSwitch.values();
		
		if (getApplicationReleaseId().equals(NONLEGACY_APPLICATION_RELEASE)) {
			return switches;
		}
		
		List<ComProfileSwitch> allSwitchList = Arrays.asList(switches);
		List<TantoProfileSwitch> validSwitchList = new ArrayList<TantoProfileSwitch>();
		
		for (ComProfileSwitch value : allSwitchList) {
			if (!value.isNonLegacyOnly()) {
				validSwitchList.add(value);
			}
		}
		
		return validSwitchList.toArray(TantoProfileSwitch[]::new);
	}
	
	@Override
	protected String getDefaultSwitchValue(TantoProfileSwitch profileSwitch) {
		ComProfileSwitch currentSwitch = getComProfileSwitch(profileSwitch);
		
		if (currentSwitch == ComProfileSwitch.BrokerDomain && is9xTransmitter()) {
			return BROKER_DOMAIN_OVERRIDE_9X;
		}
		
		return database.executeQuery("select arp.default_value from pcscfgdb.application_release_property arp" +
					" join pcscfgdb.property p on p.property_id = arp.property_id" +
					" where p.property_name = '" + currentSwitch.toString() + "'" +
					" and arp.application_release_id = '" + getApplicationReleaseId() + "'").getFirstCellValue();
	}
	
	/*
	 * Local DB functions
	 */
	
	private String getPatientOverride(TantoProfileSwitch profileSwitch) {
		return database.executeQuery("select po.patient_override_value from pcscfgdb.patient_override po" +
				" join pcscfgdb.application_release_property arp on arp.app_release_property_id = po.app_release_property_id" +
				" join pcscfgdb.property p on p.property_id = arp.property_id " +
				" join patients.patient pa on pa.merlin_net_id = po.merlin_net_id " +
				" join patients.patient_device pd on pd.patient_id = pa.patient_id " +
				" join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
				" where p.property_name = '" + profileSwitch.toString() + "'" +
				" and arp.application_release_id = '" + getApplicationReleaseId() + "'" +
				" and pd.device_serial_num = '" + deviceSerial + "'" +
				" and dp.device_model_num = '" + deviceModel + "'").getFirstCellValue();
	}
	
	private String getApplicationReleaseId() {
		if (applicationReleaseId != null) {
			return applicationReleaseId;
		}
		
		QueryResult queryResult = database.executeQuery("select spv.product_ver_number from system.software_product_version spv" +
				" join customers.customer_application ca on ca.webapp_version_id = spv.software_product_version_id" +
				" join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
				" join patients.patient_device pd on cap.patient_id = pd.patient_id " +
				" join devices.device_product dp on dp.device_product_id = pd.device_product_id " +
				" where pd.device_serial_num = '" + deviceSerial + "'" +
				" and dp.device_model_num = '" + deviceModel + "'");
		
		applicationReleaseId = (Integer.parseInt(queryResult.getFirstCellValue()) >= COMPROFILE_NONLEGACY_PRODUCT_VERSION) 
									? NONLEGACY_APPLICATION_RELEASE : LEGACY_APPLICATION_RELEASE;
		
		return applicationReleaseId;
	}
	
	/*
	 * Helper functions
	 */
	
	private ComProfileSwitch getComProfileSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof ComProfileSwitch)) {
			throw new RuntimeException("Invalid switch for ComProfile: " + profileSwitch.toString());
		}
		
		return (ComProfileSwitch) profileSwitch;
	}
	
	private boolean is9xTransmitter() {
		if (is9xTransmitter != null) {
			return is9xTransmitter;
		}
		
		String[] softwareVersion = transmitterSWVersion.substring(transmitterSWVersion.indexOf("PR_") + 3).split("\\.");
		
		is9xTransmitter = (softwareVersion[0].equals("9")) ? true : false;
		
		return is9xTransmitter;
	}
}
