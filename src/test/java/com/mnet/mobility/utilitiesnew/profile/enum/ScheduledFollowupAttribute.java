package com.mnet.mobility.utilities.profile.enums;

import com.mnet.mobility.utilities.profile.AppProfile;
import com.mnet.mobility.utilities.profile.ProfileAttribute;
import com.mnet.mobility.utilities.validation.ProfileValidation.AppProfileType;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor
/**Represents fields from NGQ/ICM Follow-up profile */
public enum ScheduledFollowupAttribute implements ProfileAttribute {

	bleSessionTimeout,
	retryOnFailure,
	payloadsToRescheduleWorkflow,
	workflowState,
	scriptToExecute_scriptsAvailable,
	scriptToExecute_sequenceName,
	workflowTransactionID,
	uploadPayloadLabel,
	eventTime_eventType,
	eventTime_eventSchedule_scheduled,
	contentVersion;
	
	@Override
	public Class<? extends AppProfile> getSubprofile() {
		return AppProfileType.SCHEDULED_FOLLOWUP.getProfileClass();
	}
}
