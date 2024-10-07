package com.mnet.middleware.utilities.tanto;

import com.mnet.middleware.utilities.tanto.enums.DatabaseField;

/**
 * Interface representing a Tanto profile switch enum.
 */
public interface TantoProfileSwitch {

	/**Returns the Tanto subprofile which this enum represents.*/
	public abstract Class<? extends TantoSubProfile> getSubprofile();
	
	/**Returns database flag associated with the designated switch.*/
	public abstract DatabaseField getDatabaseField();
	
	/**
	 * Returns true only if the field represents an XML attribute (false by default).
	 * Override for subprofile enums that support XML attributes.
	 */
	public default boolean isXmlAttribute() {
		return false;
	}

}
