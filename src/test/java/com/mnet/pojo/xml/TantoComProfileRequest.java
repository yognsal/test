package com.mnet.pojo.xml;

import org.apache.commons.lang3.StringUtils;

import com.mnet.framework.utilities.XMLData;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JAXB data object representing a Tanto communications profile XML request.
 * Constructor taking all possible arguments is auto-generated.
 * @author Arya Biswas
 */
@XmlRootElement(name = "GetComProfile")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @NoArgsConstructor
public class TantoComProfileRequest extends XMLData {
	
	private String IMDModelNum;
	private String IMDSerialNum;
	private String TransmitterSWLevel;
	private String ProfileVersion;
		
	/**
	 * Convenience constructor for transmitter-based ComProfile request..
	 * For IMD-based request, use TantoComProfile(String, String, String, String).
	 */
	public TantoComProfileRequest(String transmitterSWLevel, String profileVersion) {
		this(null, null, transmitterSWLevel, profileVersion);
	}
	
	/**
	 * Constructor for IMD-based ComProfile request.
	 */
	public TantoComProfileRequest(String imdModelNum, String imdSerialNum, String transmitterSWLevel, String profileVersion) {
		if (StringUtils.isEmpty(imdModelNum)) {
			imdModelNum = null;
		}
		
		if (StringUtils.isEmpty(imdSerialNum)) {
			imdSerialNum = null;
		}
		
		IMDModelNum = imdModelNum;
		IMDSerialNum = imdSerialNum;
		TransmitterSWLevel = transmitterSWLevel;
		ProfileVersion = profileVersion;
	}
	
}
