package com.mnet.framework.azure;

import java.util.HashMap;
import java.util.Map;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.utilities.FileUtilities;

/***
 * Enables access to Azure-hosted resources via service principal (i.e managed identity).
 * @author Arya Biswas
 * @version Fall 2023
 */
public class AzureManagedIdentity {

	private ClientSecretCredential clientCredentials;
	private FrameworkLog log;
	private FileUtilities fileManager;
	
	private Map<String, AzureResource> resources = new HashMap<String, AzureResource>();
	
	private static final String AZURE_CLIENT_ID = FrameworkProperties.getProperty("AZURE_CLIENT_ID");
	private static final String AZURE_CLIENT_SECRET = FrameworkProperties.getProperty("AZURE_CLIENT_SECRET");
	private static final String AZURE_TENANT_ID = FrameworkProperties.getProperty("AZURE_TENANT_ID");
	
	/**
	 * Generates a set of managed credentials to access Azure AD resources.
	 **/
	public AzureManagedIdentity(FrameworkLog frameworkLog, FileUtilities fileUtility) {
		log = frameworkLog;
		fileManager = fileUtility;
		
		clientCredentials = new ClientSecretCredentialBuilder()
				.clientId(AZURE_CLIENT_ID)
				.clientSecret(AZURE_CLIENT_SECRET)
				.tenantId(AZURE_TENANT_ID)
				.build();
	}
	
	/**
	 * Retrieves an Azure resource and establishes a connection if it doesn't already exist.
	 */
	public AzureResource getResource(String resourceName, Class<? extends AzureResource> resourceType) {
		AzureResource resource;
		
		switch(resourceType.getSimpleName()) {
		case "AzureIotHub":
			resource = new AzureIotHub(log, clientCredentials, resourceName);
			break;
		case "AzureStorageAccount":
			resource = new AzureStorageAccount(log, fileManager, clientCredentials, resourceName);
			break;
		case "AzureDeviceProvisioningService":
			resource = new AzureDeviceProvisioningService(log, clientCredentials, resourceName);
			break;
		case "AzureNotificationHubNamespace":
			resource = new AzureNotificationHubNamespace(log, resourceName);
			break;
		default:
			String err = "Resource of type: " + resourceType + " not supported";
			log.error(err);
			throw new RuntimeException(err);
		}
		
		if (!resources.containsKey(resourceName)) {
			resources.put(resourceName, resource);
		} else {
			resources.replace(resourceName, resource);
		}
		
		return resources.get(resourceName);
	}
}
