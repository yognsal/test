package com.mnet.pojo.xml;

import com.mnet.framework.utilities.XMLData;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * JAXB data object representing a Tanto communications profile XML request.
 * Constructor taking all possible arguments is auto-generated.
 * @author Arya Biswas
 */
@XmlRootElement(name = "ComProfile")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter @AllArgsConstructor @NoArgsConstructor
public class TantoComProfileResponse extends XMLData {
	
	private String BrokerDomain;
	private String BrokerPort;
	private String ConnectRetryInterval;
	private String MessageRetryInterval;
	private String RetryCount;
	private String KeepAlive;
	private String ProfileTimeout;
	private String AckTimeout;
	private String SegmentSize;
	private String PatientProfileVersion;
	private String SWUpgradeHost;
	private String SWUpgradePort;
	private String SubscribeTimeout;
	private String PublishTimeout;
	private String UploadTimeout;
	
}
