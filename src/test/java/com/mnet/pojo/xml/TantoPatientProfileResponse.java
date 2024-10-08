package com.mnet.pojo.xml;

import java.util.List;

import com.mnet.framework.utilities.XMLData;
import com.mnet.pojo.xml.tanto.PayloadProfile;
import com.mnet.pojo.xml.tanto.SystemData;

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
@XmlRootElement(name = "ProfileList", namespace = "http://www.merlin.net/PayloadProfile.xsd")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @AllArgsConstructor @NoArgsConstructor
public class TantoPatientProfileResponse extends XMLData {
	
	private SystemData SystemData;
	
	@XmlElement(name = "PayloadProfile")
	private List<PayloadProfile> subprofiles;

	//**Returns Tanto subprofile with the designated "Type" attribute. */
	public PayloadProfile getSubprofileOfType(String subprofileType) {
		for (PayloadProfile profile : subprofiles) {
			if (profile.getType().equalsIgnoreCase(subprofileType)) {
				return profile;
			}
		}
		
		return null;
	}
	
}
