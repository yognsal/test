package com.mnet.pojo.mobility;

import java.util.Map;

import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.pojo.mobility.ale.WorkflowStatus;

import lombok.Getter;

/**
 * Represents an App Life Event (ALE) i.e device-to-cloud message sent by a Patient App instance.
 * @author Arya Biswas
 * @version Fall 2023
 */
public abstract class AppLifeEvent {
		
	/**Identifying name of ALE to passed in JSON content.*/
	@Getter
	private String type;
	@Getter
	private String patientAppALEId;
	@Getter
	private long appTime;
	@Getter
	private AppLifeEventResult result;
	
	private String encodedContent;
	
	/**Values to be replaced from JSON templated content.*/
	protected Map<String, Object> templateValues;
	
	public AppLifeEvent(String aleType, AppLifeEventResult aleResult, Map<String, Object> replaceValues) {
		type = aleType;
		patientAppALEId = (String) replaceValues.get("patient_app_ale_id");
		appTime = (long) replaceValues.get("app_time");
		result = aleResult;
		
		templateValues = replaceValues;
	}
	
	/**Represents the outcome of an App Life Event (ALE) telemetry.*/
	@Getter
	public enum AppLifeEventResult {
		FAILURE(1, "Failed"),
		SUCCESS(2, "Success"),
		MODEL_RECONCILIATION_FAILURE(9, "Model Reconcilation Failure"),
		SERVICE_APP_STATE(19, "Service App State"),
		SERVER_BOND_LOST(20, "Server Bond Lost"),
		IMD_BOND_LOST(21, "IMD Bond Lost"),
		OS_BOND_LOST(22, "OS Bond Lost"),
		UNKNOWN(0, "Unknown");
		
		/**Result code associated with the ALE.*/
		int code;
		/**Plain text interpretation of result code.*/
		String description;
		
		private AppLifeEventResult(int resultCode, String resultDescription) {
			code = resultCode;
			description = resultDescription;
		}
		
		public boolean isBondLost() {
			return ((this == SERVER_BOND_LOST) 
					|| (this == IMD_BOND_LOST) 
					|| (this == OS_BOND_LOST)) ? true : false;
		}
	}
	
	/**Returns database code associated with this app life event.*/
	public abstract String getALETypeCode();
	
	/**Base64 encoded JSON content for the ALE.*/
	public final String getEncodedContent() {
		if (encodedContent == null) {
			encodedContent = MobilityUtilities.base64Encode(
					MobilityUtilities.updateJSON(this.getClass().getSimpleName(), templateValues));
		}
		
		return encodedContent;
	}
	
	/**
	 * @return true if and only if sending this ALE will result in the generation of a Backup VVI Alert transmission.
	 */
	public boolean isBVVI() {
		return (this instanceof WorkflowStatus) && (result == AppLifeEventResult.SERVICE_APP_STATE);
	}
	
}
