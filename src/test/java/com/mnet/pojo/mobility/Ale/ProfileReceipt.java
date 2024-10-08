package com.mnet.pojo.mobility.ale;

import java.util.Map;

import org.apache.groovy.parser.antlr4.util.StringUtils;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.MobilityUtilities.ProfileType;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

/**
 * ALE used to communicate that profiles have been received 
 * by the Patient App and should not be retried subsequently.
 * 
 * @author Arya Biswas
 * @version Q1 2024
 */
public class ProfileReceipt extends AppLifeEvent {
	
	private static final String PROFILE_RECEIPT_CODE = "4002";
	
	private static String workflowResultContent = "{\n\t\"workflowId\": <workflow_id>,"
			+ "\n\t\"contentVersion\": <content_version>,"
			+ "\n\t\"result\": <result_code>,"
			+ "\n\t\"resultDescription\": \"<result_description>\"\n}";
	
	/**
	 * @param patientApp Active Patient App instance.
	 * @param aleResult Desired result code for ALE.
	 * @param profiles List of workflow profiles to acknowledge receipt for.
	 */
	public ProfileReceipt(PatientApp patientApp, AppLifeEventResult aleResult, ProfileType... profiles) {
		super("profileReceipt", aleResult,
				Map.of("workflow_result", getWorkflowResult(patientApp, aleResult, profiles),
						"app_time", System.currentTimeMillis(),
						"azure_id", patientApp.getAzureId(),
						"patient_app_ale_id", MobilityUtilities.getUUID(),
						"user_record_id", patientApp.getIdentity().getUserRecordId()));
	}
	
	/*
	 * Local helper functions
	 */
	
	private static String getWorkflowResult(PatientApp patientApp, AppLifeEventResult aleResult, ProfileType... profiles) {
		String workflowResult = "";
		
		for (ProfileType profile : profiles) {
			if (!StringUtils.isEmpty(workflowResult)) {
				workflowResult += ",\n";
			}
			
			workflowResult += workflowResultContent.replace("<workflow_id>", Integer.toString(profile.getProfileCode()))
													.replace("<content_version>", 
															Integer.toString(
																	PatientAppDBUtilities.getContentVersion(
																		patientApp.getCurrentTest(), 
																		patientApp.getDeviceSerial(), 
																		profile.getProfileCode())))
													.replace("<result_code>", Integer.toString(aleResult.getCode()))
													.replace("<result_description>", aleResult.getDescription());
		}
		
		return workflowResult;
	}
	
	@Override
	public String getALETypeCode() {
		return PROFILE_RECEIPT_CODE;
	}
}
