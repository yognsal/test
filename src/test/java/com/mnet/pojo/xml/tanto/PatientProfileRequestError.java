package com.mnet.pojo.xml.tanto;

import com.mnet.framework.utilities.XMLData;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JAXB data object representing a Tanto communications profile XML request.
 * Constructor taking all possible arguments is auto-generated.
 * @author Arya Biswas
 */
@XmlRootElement(name = "PatientProfileRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @AllArgsConstructor @NoArgsConstructor
public class PatientProfileRequestError extends XMLData {
	
	private String TransmitterProfileID;
	private String ICDModelNumber;
	private String ICDSerialNumber;
	private String TransmitterModelNumber;
	private String TransmitterSerialNumber;
	private String TransmitterSWversion;
	private String TransmitterRequestType;
	private String ProfileVersion;
}
