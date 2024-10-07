package com.mnet.framework.azure;

import com.mnet.framework.reporting.FrameworkLog;

/**
 * Represents an Azure-managed resource in the cloud.
 */
public abstract class AzureResource {

	protected String name;
	protected FrameworkLog log;
	
	protected AzureResource(String resourceName, FrameworkLog frameworkLog) {
		name = resourceName;
		log = frameworkLog;
	}
	
}
