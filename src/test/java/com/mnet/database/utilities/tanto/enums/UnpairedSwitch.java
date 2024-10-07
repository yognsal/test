package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

/**Represents fields from Tanto Unpaired subprofile.*/
public enum UnpairedSwitch implements TantoProfileSwitch {

	MODEL_EXCL_PREF;
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.UNPAIRED.getProfileClass();
	}
	
	@Override
	public DatabaseField getDatabaseField() {
		return null;
	}
}
