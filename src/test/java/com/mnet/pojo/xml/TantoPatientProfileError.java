package com.mnet.pojo.xml;

import com.mnet.framework.utilities.XMLData;
import com.mnet.pojo.xml.tanto.PatientProfileRequestError;

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
@XmlRootElement(name = "PatientProfileRequestFailure", namespace = "http://www.merlin.net/PatientProfileRequestFailure.xsd")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @AllArgsConstructor @NoArgsConstructor
public class TantoPatientProfileError extends XMLData {
	
	private String ReasonCode;
	private String Explanation;
	private PatientProfileRequestError PatientProfileRequest;
	
}
