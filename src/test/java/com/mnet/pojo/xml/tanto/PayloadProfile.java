package com.mnet.pojo.xml.tanto;

import com.mnet.framework.utilities.XMLData;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JAXB data object representing a Tanto patient profile XML request.
 * Constructor taking all possible arguments is auto-generated.
 * @author Arya Biswas
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @AllArgsConstructor @NoArgsConstructor
public class PayloadProfile extends XMLData {
	
	@XmlAttribute(name = "Type")
	private String Type;
	
	private GenerateSchedule GenerateSchedule;
	private UploadSchedule UploadSchedule;
	private Controls Controls;
}
