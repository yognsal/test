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
public class GenerateSchedule extends XMLData {
	
	@XmlAttribute(name = "GS_DateOfEvent")
	private String GS_DateOfEvent;
	
	@XmlAttribute(name = "GS_Interval")
	private String GS_Interval;
	
	@XmlAttribute(name = "GS_WeeklyEvent")
	private String GS_WeeklyEvent;
	
	@XmlAttribute(name = "GS_TimeOfEvent")
	private String GS_TimeOfEvent;
	
	@XmlAttribute(name = "GS_UnscheduledEvent")
	private String GS_UnscheduledEvent;
	
}
