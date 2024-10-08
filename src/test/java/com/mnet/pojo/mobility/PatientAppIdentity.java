package com.mnet.pojo.mobility;

import com.mnet.mobility.utilities.MobilityUtilities;

import lombok.Getter;

/**
 * Represents a patient app identity generated on patient validation.
 * 
 * @author Arya Biswas
 * @version Fall 2023
 */
@Getter
public class PatientAppIdentity {

	private String patientAppIdentityRecord, publicPem;
	private long userRecordId;
	
	public PatientAppIdentity(String patientAppIdentityRecord, long userRecordId) {
		this.patientAppIdentityRecord = patientAppIdentityRecord;
		this.userRecordId = userRecordId;
		
		publicPem = MobilityUtilities.base64Decode(patientAppIdentityRecord);
	}
}
