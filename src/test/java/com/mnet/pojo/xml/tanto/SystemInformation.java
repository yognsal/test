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
@XmlRootElement(name = "SystemInformation")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @AllArgsConstructor @NoArgsConstructor
public class SystemInformation extends XMLData {
	
	@XmlAttribute(name = "SchemaVersion")
	private String SchemaVersion;
	
	@XmlAttribute(name = "DeviceModel")
	private String DeviceModel;
	
	@XmlAttribute(name = "DeviceSerialNumber")
	private String DeviceSerialNumber;
	
	@XmlAttribute(name = "ProfileVersion")
	private String ProfileVersion;
	
	@XmlAttribute(name = "UTCServerTime")
	private String UTCServerTime;
	
	@XmlAttribute(name = "ProfileDate")
	private String ProfileDate;
	
	@XmlAttribute(name = "NumberOfProfiles")
	private String NumberOfProfiles;
	
	@XmlAttribute(name = "PatientNotifyWindowStart")
	private String PatientNotifyWindowStart;
	
	@XmlAttribute(name = "PatientNotifyWindowEnd")
	private String PatientNotifyWindowEnd;
}
