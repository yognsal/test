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
public class UploadSchedule extends XMLData {
	
	@XmlAttribute(name = "US_DateOfEvent")
	private String US_DateOfEvent;
	
	@XmlAttribute(name = "US_Interval")
	private String US_Interval;
	
	@XmlAttribute(name = "US_WeeklyEvent")
	private String US_WeeklyEvent;
	
	@XmlAttribute(name = "US_TimeOfEvent")
	private String US_TimeOfEvent;
	
	@XmlAttribute(name = "US_UnscheduledEvent")
	private String US_UnscheduledEvent;
}
