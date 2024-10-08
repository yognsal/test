package com.mnet.mobility.utilities.validation;

import com.mnet.framework.azure.AzureDeviceProvisioningService;
import com.mnet.framework.azure.AzureIotHub;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.mobility.utilities.Keyfactor;
import com.mnet.mobility.utilities.PatientApp;

/**
 * Provides functionality to verify patient app provisioning status in an IoT hub.
 * @implNote Requires {@code TestAttribute.AZURE} to be enabled in test class.
 * @author Arya Biswas
 * @version Q1 2024
 */
public interface DeviceProvisioningValidation {
	
	/**Returns true only if patient app instance is actively provisioned to a DPS enrollment group.*/
	public default boolean isDeviceProvisioned(PatientApp patientApp) {
		return getDPS(patientApp.getCurrentTest()).isDeviceProvisioned(patientApp.getAzureId());
	}
	
	/**Returns true only if patient app instance has been moved to individual enrollment and certs have been revoked.*/
	public default boolean isDeviceDeprovisioned(PatientApp patientApp) {
		return isDeviceInIndividualEnrollment(patientApp) && areAppCertificatesRevoked(patientApp);
	}
	
	/**Returns true only if patient app instance has been moved to individual enrollment.*/
	public default boolean isDeviceInIndividualEnrollment(PatientApp patientApp) {
		return getDPS(patientApp.getCurrentTest()).isDeviceInIndividualEnrollment(patientApp.getAzureId());
	}
	
	/**Returns true only if patient app identity and credential certificates have been revoked.*/
	public default boolean areAppCertificatesRevoked(PatientApp patientApp) {
		Keyfactor pki = new Keyfactor(patientApp.getCurrentTest().getLog());
		
		return !(pki.hasActivePatientAppCertificates(patientApp.getAzureId()));
	}
	
	public default String getAzureIotHubPropertyValue() {
		return FrameworkProperties.getProperty("AZURE_IOT_HUB");
	}
	
	/**Returns JSON contents of device twin for a device associated with an IoT hub.*/
	public default String getDeviceTwinContents(PatientApp patientApp) {
		return getIotHub(patientApp.getCurrentTest()).getDeviceTwinContents(patientApp.getAzureId());
	}
	
	static AzureDeviceProvisioningService getDPS(MITETest currentTest) {
		String AZURE_DPS = FrameworkProperties.getProperty("AZURE_DPS");
		return (AzureDeviceProvisioningService) currentTest.getAzure().getResource(AZURE_DPS, AzureDeviceProvisioningService.class);
	}
	
	static AzureIotHub getIotHub(MITETest currentTest) {
		String AZURE_IOT_HUB = FrameworkProperties.getProperty("AZURE_IOT_HUB");
		return (AzureIotHub) currentTest.getAzure().getResource(AZURE_IOT_HUB, AzureIotHub.class);
	}
}
