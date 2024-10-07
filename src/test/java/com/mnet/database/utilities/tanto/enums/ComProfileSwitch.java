package com.mnet.middleware.utilities.tanto.enums;

import com.mnet.middleware.utilities.TantoProfileResponseValidator.TantoSubProfileType;
import com.mnet.middleware.utilities.tanto.TantoProfileSwitch;
import com.mnet.middleware.utilities.tanto.TantoSubProfile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter @NoArgsConstructor @AllArgsConstructor
public enum ComProfileSwitch implements TantoProfileSwitch {

	BrokerDomain,
	BrokerPort,
	ConnectRetryInterval,
	MessageRetryInterval,
	RetryCount,
	KeepAlive,
	ProfileTimeout,
	AckTimeout,
	SegmentSize,
	PatientProfileVersion,
	SWUpgradeHost,
	SWUpgradePort,
	SubscribeTimeout(true),
	PublishTimeout(true),
	UploadTimeout(true);
	
	private boolean nonLegacyOnly;
	
	@Override
	public Class<? extends TantoSubProfile> getSubprofile() {
		return TantoSubProfileType.COMPROFILE.getProfileClass();
	}
	
	@Override
	public DatabaseField getDatabaseField() {
		return null;
	}
}
