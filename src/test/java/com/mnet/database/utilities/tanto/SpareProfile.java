package com.mnet.middleware.utilities.tanto;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoDriver.TantoAttributeCategory;
import com.mnet.middleware.utilities.TantoDriver.TantoPayloadProfileType;
import com.mnet.middleware.utilities.tanto.enums.DatabaseField;
import com.mnet.middleware.utilities.tanto.enums.FollowUpSwitch;
import com.mnet.middleware.utilities.tanto.enums.SpareSwitch;

/**
 * Business logic validation for Tanto GDC subprofile.
 * @author Arya Biswas
 * @version Summer 2023
 */
public class SpareProfile extends TantoPatientSubProfile {
	
	// TODO: NGQMOD switch logic

	private static final String SPARE_PROFILE_CODE = "517";
	private static final String TRANSMITTER_SCRIPT_SW_VERSION_DEFAULT = "1";
	private static final String NGQ_FOLLOWUP_CLEAR_ALL_CODE = "44";
	private static final String NGQ_FOLLOWUP_CLEAR_DIAG_CODE = "45";
	private static final String NGQ_FOLLOWUP_CLEAR_EPIS_EGM_CODE = "46";
	private static final String NGQ_FOLLOWUP_CLEAR_NONE_CODE = "47";
	private static final String NGQ_DIM_VERSION = "0301";
	private static final String NGQ_MODEL_PREFIX = "CD";
	private static final LocalDateTime BASELINE_SPARE_UTC = LocalDateTime.of(2000, 01, 01, 0, 0);
	
	private String transmitterScriptTypeCode;
	
	public SpareProfile(TantoDriver tantoDriver, DatabaseConnector databaseConnector, TestReporter testReporter, boolean useSoftAssert, boolean onlyReportFailures) {
		super(tantoDriver, databaseConnector, testReporter, SPARE_PROFILE_CODE, useSoftAssert, onlyReportFailures);
	}
	
	@Override
	public boolean validateSwitch(TantoProfileSwitch profileSwitch) {
		SpareSwitch currentSwitch = getSpareSwitch(profileSwitch);
		
		String responseSwitchValue = null;
		String switchName = currentSwitch.toString();
		
		if (switchName.contains("GS")) {
			responseSwitchValue = driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.SPARE, TantoAttributeCategory.GENERATE_SCHEDULE, currentSwitch.toString());
		} else if (switchName.contains("US")) {
			responseSwitchValue = driver.getXMLAttribute(profileResponse, TantoPayloadProfileType.SPARE, TantoAttributeCategory.UPLOAD_SCHEDULE, currentSwitch.toString());
		} else {
			responseSwitchValue = driver.getXMLElement(profileResponse, TantoPayloadProfileType.SPARE, currentSwitch.toString());
		}
		
		return reportSwitchValidation(profileSwitch, responseSwitchValue, getSwitchOverride(profileSwitch));
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
		
		// Interpret with single decimal precision
		if (profileSwitch.toString().contains("REAL")) {
			DecimalFormat realFormat = new DecimalFormat("0.0");
			return realFormat.format(Long.parseLong(override)); 
		}
		
		return override;
	}
	
	@Override
	public TantoProfileSwitch[] getAllSwitches() {
		return SpareSwitch.values();
	}
	
	@Override
	protected String getAttributeValue(TantoProfileSwitch profileSwitch) {
		SpareSwitch currentSwitch = getSpareSwitch(profileSwitch);
		
		switch (currentSwitch) {
			case GS_DateOfEvent:
			case US_DateOfEvent:
				return getUTCTime(BASELINE_SPARE_UTC, DateTimeFormat.YYYY_MM_DD);
			case GS_TimeOfEvent:
			case US_TimeOfEvent:
				return getUTCTime(BASELINE_SPARE_UTC, DateTimeFormat.HH_MI_SS);
			default:
				return super.getAttributeValue(profileSwitch);
		}
	}
	
	@Override
	protected String getMODOverride(TantoProfileSwitch profileSwitch) {
		if (!deviceModel.contains(NGQ_MODEL_PREFIX)) {
			return null;
		}
		
		SpareSwitch currentSwitch = getSpareSwitch(profileSwitch);
		
		switch (currentSwitch) { // processing rules as per Auto6316
			case SPARE_TEXT1:
			case SPARE_TEXT2:
				return getNGQTransmitterScript(currentSwitch);
			case SPARE_TEXT3:
				return getNGQProfiles();
			case SPARE_TEXT4:
				return getNGQExclusionList();
			case SPARE_INTEGER6:
				return TRANSMITTER_SCRIPT_SW_VERSION_DEFAULT;
			default:
				return null;
		}
	}
	
	@Override
	protected String getPatientOverride(TantoProfileSwitch profileSwitch) {
		return null;
	}
	
	@Override
	protected String getClinicOverride(TantoProfileSwitch profileSwitch) {
		return null;
	}
	
	/*
	 * Local database functions
	 */
	
	/**@implNote Reference UnpairedTantoPatientProfileService*/
	private String getNGQTransmitterScript(SpareSwitch currentSwitch) {
		DatabaseField databaseField = currentSwitch.getDatabaseField();
		
		if (databaseField == null) {
			throw new RuntimeException("Spare switch does not have database validation: " + currentSwitch.toString());
		}
		
		QueryResult transmitterScriptQuery = database.executeQuery("select " + databaseField.toString() + " from profile.transmitter_profile_script tps" +
				" where trans_script_type_cd = '" + getTransmitterScriptTypeCode() + "'");
		
		return Base64.getEncoder().encodeToString(transmitterScriptQuery.getFirstCellValue().getBytes());
	}
	
	/**@implNote JSONArray / JSONObject cannot be parameterized.*/
	@SuppressWarnings("unchecked")
	private String getNGQProfiles() {
		List<List<String>> ngqProfiles = database.executeQuery("select profile_cd, content_clob from profile.profile_default_container pdc" +
					" where content_clob like '%\"" + NGQ_DIM_VERSION + "\"%").getAllRows();
		
		JSONArray compiledProfiles = new JSONArray();
		
		for (List<String> profile : ngqProfiles) {
			JSONObject jsonProfile = new JSONObject();
			
			jsonProfile.put("id:", profile.get(0));
			jsonProfile.put("content:", profile.get(1));
			
			compiledProfiles.add(jsonProfile);
		}
		
		return Base64.getEncoder().encodeToString(compiledProfiles.toString().getBytes());
	}
	
	/*
	 * Helper functions
	 */
	
	private SpareSwitch getSpareSwitch(TantoProfileSwitch profileSwitch) {
		if (!(profileSwitch instanceof SpareSwitch)) {
			throw new RuntimeException("Invalid switch for DeviceCheck profile: " + profileSwitch.toString());
		}
		
		return (SpareSwitch) profileSwitch;
	}
	
	private String getTransmitterScriptTypeCode() {
		if (transmitterScriptTypeCode != null) {
			return transmitterScriptTypeCode;
		}
		
		boolean clearEpisFlag = (super.getMODOverride(FollowUpSwitch.CLEAR_EPIS_FLAG).equals("Enable")) ? true : false;
		boolean clearDiagFlag = (super.getMODOverride(FollowUpSwitch.CLEAR_DIAG_FLAG).equals("Enable")) ? true : false;
		boolean clearSegmFlag = (super.getMODOverride(FollowUpSwitch.CLEAR_SEGM_FLAG).equals("Enable")) ? true : false;
		
		if (clearDiagFlag && (clearEpisFlag || clearSegmFlag)) {
			transmitterScriptTypeCode = NGQ_FOLLOWUP_CLEAR_ALL_CODE;
		} else if (clearDiagFlag) {
			transmitterScriptTypeCode = NGQ_FOLLOWUP_CLEAR_DIAG_CODE;
		} else if (clearEpisFlag && clearSegmFlag) {
			transmitterScriptTypeCode = NGQ_FOLLOWUP_CLEAR_EPIS_EGM_CODE;
		} else {
			transmitterScriptTypeCode = NGQ_FOLLOWUP_CLEAR_NONE_CODE;
		}
		
		return transmitterScriptTypeCode;
	}
	
	private String getNGQExclusionList() {
		String[] excludedDevices = getModelExclusion().split(":");
		String ngqModelExclusion = "";
		
		for (String device : excludedDevices) {
			if (device.contains(NGQ_MODEL_PREFIX)) {
				ngqModelExclusion += device + ":";
			}
		}
		
		if (!ngqModelExclusion.equals("")) {
			ngqModelExclusion = ":" + ngqModelExclusion;
		}
		
		return Base64.getEncoder().encodeToString(ngqModelExclusion.getBytes());
	}

}
