package com.mnet.mobility.utilities.profile;

/**
 * Interface representing a Mobility profile enum.
 */
public interface ProfileAttribute {

	/**Returns the NGQ/ICM subprofile which this enum represents.*/
	public abstract Class<? extends AppProfile> getSubprofile();
	
	/**Returns JSONPath locator for this attribute.*/
	public default String getJsonPath() {
		return this.toString().replaceAll("_", "."); 
	}
}
