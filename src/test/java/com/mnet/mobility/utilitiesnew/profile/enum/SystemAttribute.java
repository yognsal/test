package com.mnet.mobility.utilities.profile.enums;

import com.mnet.mobility.utilities.profile.AppProfile;
import com.mnet.mobility.utilities.profile.ProfileAttribute;
import com.mnet.mobility.utilities.validation.ProfileValidation.AppProfileType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
/**Represents fields from NGQ/ICM System profile */
public enum SystemAttribute implements ProfileAttribute {

	clinicInformation_clinicName,
	clinicInformation_address_address1,
	clinicInformation_address_address2,
	clinicInformation_address_address3,
	clinicInformation_address_city,
	clinicInformation_address_state,
	clinicInformation_address_zip,
	clinicInformation_address_country,
	clinicInformation_contactPhone_country,
	clinicInformation_contactPhone_area,
	clinicInformation_contactPhone_number,
	userNotificationWindow_notifyWindowStart,
	userNotificationWindow_notifyWindowEnd,
	debugProxyLogging_verbose,
	workflowPriority,
	appLogging_verbose,
	complianceNotificationDelay,
	deviceInformation_unityApplicationID,
	deviceInformation_marketingName,
	deviceInformation_modelNumber,
	backgroundConnectionAttemptIntervalControl_retryCount,
	backgroundConnectionAttemptIntervalControl_retryDelay,
	clearDataAndReset,
	foregroundDeviceScanTimeout,
	workflowSequences,
	contentVersion;
	
	@Override
	public Class<? extends AppProfile> getSubprofile() {
		return AppProfileType.SYSTEM.getProfileClass();
	}
}
