package com.mnet.pojo.mobility.attributes;

import lombok.Data;

/**
 * POJO to maintain attributes for Azure Data API response
 * @author kotwarx2
 *
 */

@Data
public class AzureDataAPIAttributes {
	
	private Number connectRetryTimeout;
	private Number supervisionTimeoutDelay;
	private Number interConnectRetryTimeout;
	private Number lowBattSupervisionTimeout;
	private Number lowBattLatency;
	private Number lowBattMaxInterval;
	private Number lowBattMinInterval;
	private Number normalSupervisionTimeout;
	private Number normalLatency;
	private Number normalMaxInterval;
	private Number normalMinInterval;
	private Number osBondConfirmTimeout;
	private Number linkEncryptionYieldTimeout;
	private Number findDevicesRetryTimeout;
	private Number attMTU;
	private Number complianceFreq;
	private String serverRegion;

	/**This constructor sets the specified atributes to its default value*/
	public AzureDataAPIAttributes() {
		connectRetryTimeout = -1;
		supervisionTimeoutDelay = -1;
		interConnectRetryTimeout = -1;
		lowBattSupervisionTimeout = -1;
		lowBattLatency = -1;
		lowBattMaxInterval = -1;
		lowBattMinInterval = -1;
		normalSupervisionTimeout = -1;
		normalLatency = -1;
		normalMaxInterval = -1;
		normalMinInterval = -1;
		osBondConfirmTimeout = -1;
		linkEncryptionYieldTimeout = -1;
		findDevicesRetryTimeout = -1;
		attMTU = 230;
		complianceFreq = 3;
		serverRegion = "EU";
		
	}

}
