package com.mnet.framework.azure;

import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.sdk.iot.provisioning.service.ProvisioningServiceClient;
import com.microsoft.azure.sdk.iot.provisioning.service.configs.EnrollmentStatus;
import com.microsoft.azure.sdk.iot.provisioning.service.exceptions.ProvisioningServiceClientException;
import com.mnet.framework.reporting.FrameworkLog;

/**
 * Represents an Azure Device Provisioning Service (DPS) connection.
 * Supports checking the provisioning status of an IoT device.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class AzureDeviceProvisioningService extends AzureResource {

	private ProvisioningServiceClient dpsClient;
	
	protected AzureDeviceProvisioningService(FrameworkLog frameworkLog, TokenCredential credential, String dpsName) {
		super(dpsName, frameworkLog);
		
		dpsClient = new ProvisioningServiceClient(dpsName + ".azure-devices-provisioning.net", credential);
	}
	
	/**
	 * @return true: if and only if the deviceId is actively provisioned and assigned to an IoT hub.
	 * @param deviceId Unique ID associated with the device registration.
	 */
	public boolean isDeviceProvisioned(String deviceId) {
		try {
			return dpsClient.getDeviceRegistrationState(deviceId).getStatus().equals(EnrollmentStatus.ASSIGNED) ? true : false;
		} catch (ProvisioningServiceClientException psce) {
			log.info("Device registration does not exist in enrollment group for deviceId: " + deviceId);
			return false;
		}
	}
	
	/**
	 * @return true: if and only if the deviceId is associated with an individual enrollment.
	 * @param registrationId Unique ID associated with the device registration.
	 */
	public boolean isDeviceInIndividualEnrollment(String registrationId) {
		try {
			dpsClient.getIndividualEnrollment(registrationId);
		} catch (ProvisioningServiceClientException psce) {
			log.info("Device registration does not exist in individual enrollment for registrationId: " + registrationId);
			return false;
		}
		
		return true;
	}
	
}
