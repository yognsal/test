package com.mnet.pojo.mobility;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents an encrypted transmission payload received from a patient app instance.
 * 
 * @author Arya Biswas
 * @version Fall 2023
 */
@Getter @AllArgsConstructor
public class EncryptedPayload {

	private TelemetryType payloadLabel;
	private int workflowId;
	/**fileName is in format deviceSerial_epochTime.zip*/
	private String fileName;
	private String transmissionKey, randomSeed, payloadHashValue, encryptedSessionRecord;
	
	/**Represents a telemetry type supported by mobility services.*/
	public enum TelemetryType {
		FUA("1066"),
		FUP("1067"),
		FUU("1068"),
		FUD("1069"),
		FUS("2258"),
		MED("1071"),
		BVVI("1073"),
		SYN("2259"),
		ARP("2748");
		
		/**Telemetry_Type_Cd in lookup.code*/
		@Getter
		private String code;
		
		private TelemetryType(String lookupCode) {
			code = lookupCode;
		}
	}
}
