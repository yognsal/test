package com.mnet.framework.utilities;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract representation of XML data objects.
 * Any XML data representations using JAXB should inherit from this class.
 * XmlAccessorType should be overridden in the child class to the appropriate access type.
 * @version Spring 2023
 * @author Arya Biswas
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class XMLData {
	
	/**Set to true for XML data representing a failure.*/
	@Getter @Setter 
	protected boolean failure;
	
	/**Local file where raw (unparsed) XML data is stored.*/
	@Getter @Setter
	protected String rawContentFileName;
	
	/**Local file where parsed XML data is stored.*/
	@Getter @Setter
	protected String parsedContentFileName;
}
