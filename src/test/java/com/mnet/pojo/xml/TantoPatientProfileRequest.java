package com.mnet.pojo.xml;

import org.apache.commons.lang3.StringUtils;

import com.mnet.framework.utilities.XMLData;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JAXB data object representing a Tanto patient profile XML request.
 * Constructor taking all possible arguments is auto-generated.
 * @author Arya Biswas
 */
@XmlRootElement(name = "GetPatientProfile")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @NoArgsConstructor
public class TantoPatientProfileRequest extends XMLData {
	
	private String TransmitterModelNumber;
	private String TransmitterSerialNumber;
	private String TransmissionType;
	private String TimeofFollowup;
	private String TransmitterSWversion;
	private String ProfileVersion;
	private String TransmitterScriptSWVersion;
	private String TransmitterScriptContentVersion;
	/**@implNote null or "W"*/
	private String TransmitterModelExtension;
	
	/**
	 * Convenience constructor with mandatory parameters for Tanto 8.x profile requests.
	 * For optional parameters, use TantoPatientProfile(String, String, String, String, String, String, String, String, String).
	 */
	public TantoPatientProfileRequest(String transmitterModelNumber, String transmitterSerialNumber,
			String transmitterSWversion, String profileVersion) {
		this(transmitterModelNumber, transmitterSerialNumber, null, null, transmitterSWversion, profileVersion, null, null, null);
	}
	
	/**
	 * Convenience constructor with mandatory parameters for Tanto 9.x profile requests.
	 * For optional parameters, use TantoPatientProfile(String, String, String, String, String, String, String, String, String).
	 */
	public TantoPatientProfileRequest(String transmitterModelNumber, String transmitterSerialNumber,
			String transmitterSWversion, String profileVersion, String transmitterScriptSWVersion, String transmitterScriptContentVersion) {
		this(transmitterModelNumber, transmitterSerialNumber, null, null, transmitterSWversion, profileVersion, 
				transmitterScriptSWVersion, transmitterScriptContentVersion, null);
	}
	
	/**
	 * Constructor for Tanto patient profile request accepting all mandatory and optional parameters.
	 * @implNote TransmitterScriptSWVersion: null or "1" (generally)
	 * @implNote TransmitterScriptContentVersion: null or "0" (generally)
	 * @implNote TransmitterModelExtension: null or "W"
	 */
	public TantoPatientProfileRequest(String transmitterModelNumber, String transmitterSerialNumber, 
			String transmissionType, String timeOfFollowup, String transmitterSWversion, String profileVersion, 
			String transmitterScriptSWVersion, String transmitterScriptContentVersion, String transmitterModelExtension) {
		if (StringUtils.isEmpty(transmissionType)) {
			transmissionType = null;
			timeOfFollowup = null;
		}
		
		if (StringUtils.isEmpty(transmitterScriptSWVersion)) {
			transmitterScriptSWVersion = null;
			transmitterScriptContentVersion = null;
		}
		
		if (StringUtils.isEmpty(transmitterModelExtension)) {
			transmitterModelExtension = null;
		}
		
		TransmitterModelNumber = transmitterModelNumber;
		TransmitterSerialNumber = transmitterSerialNumber;
		TransmissionType = transmissionType;
		TimeofFollowup = timeOfFollowup;
		TransmitterSWversion = transmitterSWversion;
		ProfileVersion = profileVersion;
		TransmitterScriptSWVersion = transmitterScriptSWVersion;
		TransmitterScriptContentVersion = transmitterScriptContentVersion;
		TransmitterModelExtension = transmitterModelExtension;
	}

}
