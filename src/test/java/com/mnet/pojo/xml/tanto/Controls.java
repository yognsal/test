package com.mnet.pojo.xml.tanto;

import java.util.ArrayList;
import java.util.List;

import com.mnet.framework.utilities.XMLData;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JAXB data object representing a Tanto patient profile XML request.
 * Constructor taking all possible arguments is auto-generated.
 * @author Arya Biswas
 */
@XmlRootElement(name = "SystemData")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @AllArgsConstructor @NoArgsConstructor
public class Controls extends XMLData {
	
	@XmlElement(name = "Switch")
	private List<Switch> switches;
	
	@XmlElement(name = "iSwitch")
	private List<Switch> iSwitches;
	
	@XmlElement(name = "rSwitch")
	private List<Switch> rSwitches;
	
	@XmlElement(name = "tSwitch")
	private List<Switch> tSwitches;
	
	public List<Switch> getAllSwitches() {
		List<Switch> allSwitches = new ArrayList<Switch>();
		
		if (switches != null) {
			allSwitches.addAll(switches);
		}
		
		if (iSwitches != null) {
			allSwitches.addAll(iSwitches);
		}
		
		if (rSwitches != null) {
			allSwitches.addAll(rSwitches);
		}
		
		if (tSwitches != null) {
			allSwitches.addAll(tSwitches);
		}
		
		return allSwitches;
	}
}
