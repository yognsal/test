package com.mnet.mobility.utilities.profile.enums;

import com.mnet.mobility.utilities.profile.AppProfile;
import com.mnet.mobility.utilities.profile.ProfileAttribute;
import com.mnet.mobility.utilities.validation.ProfileValidation.AppProfileType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
/**Represents fields from NGQ/ICM DevMed profile */
public enum DevMedAttribute implements ProfileAttribute {

	bleSessionTimeout,
	retryOnFailure,
	payloadsToRescheduleWorkflow,
	workflowState,
	scriptToExecute_scriptsAvailable,
	scriptToExecute_sequenceName,
	workflowTransactionID,
	uploadPayloadLabel,
	eventTime_eventType,
	eventTime_eventSchedule_periodic_eventTime,
	eventTime_eventSchedule_periodic_eventInterval,
	contentVersion;
	
	@Override
	public Class<? extends AppProfile> getSubprofile() {
		return AppProfileType.DEVMED.getProfileClass();
	}
}
