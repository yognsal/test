package com.mnet.framework.azure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.microsoft.azure.sdk.iot.service.twin.Twin;
import com.microsoft.azure.sdk.iot.service.twin.TwinClient;
import com.mnet.framework.reporting.FrameworkLog;


/***
 * Represents a device twin associated with an IoT hub.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class AzureDeviceTwin {
	
	private Twin device;
	private JsonObject tags;
	private JsonObject desiredProperties;
	private JsonObject reportedProperties;
	
	private FrameworkLog log;
	
	private List<String> reservedWords = List.of("tags", "properties", "desired", "reported");
	
	protected AzureDeviceTwin(FrameworkLog frameworkLog, TwinClient twinClient, String deviceId) {
		log = frameworkLog;

		try {
			device = twinClient.get(deviceId);
		} catch (IOException | IotHubException ie) {
			String err = "Failed to retrieve device twin from IoT hub for deviceId: " + deviceId + " | Reason: " + ie.getMessage();
			log.error(err);
			log.printStackTrace(ie);
			throw new RuntimeException(err);
		}
		
		tags = device.getTags().toJsonObject();
		desiredProperties = device.getDesiredProperties().toJsonObject();
		reportedProperties = device.getReportedProperties().toJsonObject();
	}
	
	protected String getContents() {
		return device.toString();
	}
	
	/**
	 * Returns value of a twin property (can be a tag, desired property, or reported property)
	 * Nested properties should be delimited by "."
	 * Return value may be a string, boolean, or integer (arrays are returned as a string sequence)
	 * @implNote e.x. properties.desired.appConfig.azureId, tags.phoneData.screenWidth
	 */
	protected Object getDeviceProperty(String property) {
		JsonObject collection;
		
		if (property.contains("tags")) {
			collection = tags;
		} else if (property.contains("properties.desired")) {
			collection = desiredProperties;
		} else if (property.contains("properties.reported")) {
			collection = reportedProperties;
		} else {
			throw new RuntimeException("Element must either be a tag or desired property: " + property);
		}
		
		return parseJsonObject(property, collection);
	}
	
	/*
	 * Local helper functions
	 */
	
	/**Recursively traverses JSON to obtain property.*/
	private Object parseJsonObject(String property, JsonObject json) {
		List<String> elements = new ArrayList<>(Arrays.asList(property.split("\\.")));
		elements.removeAll(reservedWords);
		
		int index = 0, depth = elements.size();
		JsonElement element = json.get(elements.get(index++));
		
		while ((index < depth) && (element != null)) {
			if (!element.isJsonObject()) {
				return element.toString();
			}
			
			element = element.getAsJsonObject().get(elements.get(index++));
		}
		
		if (element == null) {
			throw new RuntimeException("No JSON element found in device twin corresponding to: " + property);
		} else if (element.isJsonNull()) {
			return null;
		} else if (element.isJsonArray() || !element.isJsonPrimitive()) {
			return element.toString();
		} 
		
		JsonPrimitive primitiveValue = element.getAsJsonPrimitive();
		
		if (primitiveValue.isBoolean()) {
			return primitiveValue.getAsBoolean();
		} else if (primitiveValue.isNumber()) {
			return primitiveValue.getAsInt();
		} else {
			return primitiveValue.getAsString();
		}
	}
}
