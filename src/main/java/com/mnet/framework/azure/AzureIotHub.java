package com.mnet.framework.azure;

import java.util.HashMap;
import java.util.Map;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.sdk.iot.service.twin.TwinClient;
import com.mnet.framework.reporting.FrameworkLog;

/***
 * Provides functionality to retrieve device twins from IoT hub.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class AzureIotHub extends AzureResource {

	private TwinClient twinClient;
	private Map<String, AzureDeviceTwin> deviceTwins = new HashMap<String, AzureDeviceTwin>();
	
	protected AzureIotHub(FrameworkLog frameworkLog, TokenCredential credential, String iotHubName) {
		super(iotHubName, frameworkLog);
		
		twinClient = new TwinClient(iotHubName + ".azure-devices.net", credential);
	}
	
	/**
	 * Returns formatted contents of device twin, including tags, desired properties, and reported properties.
	 * @param deviceId Unique ID associated with registered IoT device.
	 */
	public String getDeviceTwinContents(String deviceId) {
		return getDeviceTwin(deviceId).getContents();
	}
	
	/**
	 * Returns value of a twin property (can be a tag or desired property).
	 * Return value may be a string, boolean, or integer (arrays are returned as a string sequence)
	 * @param property Name of property or nested property. Nested properties should be delimited by "."
	 * @implNote e.x. tags.phoneData, properties.desired.appConfig.azureId
	 */
	public Object getDeviceTwinProperty(String deviceId, String property) {
		return getDeviceTwin(deviceId).getDeviceProperty(property);
	}
	
	/*
	 * Local helper functions
	 */
	
	private AzureDeviceTwin getDeviceTwin(String deviceId) {
		AzureDeviceTwin deviceTwin;
		
		if (!deviceTwins.containsKey(deviceId)) {
			deviceTwin = new AzureDeviceTwin(log, twinClient, deviceId);
			deviceTwins.put(deviceId, deviceTwin);
		}
		
		return deviceTwins.get(deviceId);
	}
}
