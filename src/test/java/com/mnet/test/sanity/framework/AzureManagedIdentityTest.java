package com.mnet.test.sanity.framework;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.azure.AzureDeviceProvisioningService;
import com.mnet.framework.azure.AzureIotHub;
import com.mnet.framework.azure.AzureStorageAccount;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.reporting.utilities.GraylogReporting;

/***
 * Tool for NGQ2.0 registration and bonding workflows.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class AzureManagedIdentityTest extends MITETest implements GraylogReporting {

	private AzureStorageAccount storageAccount;
	private AzureIotHub iotHub;
	private AzureDeviceProvisioningService deviceProvisioningService;
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		shortTestName = "NGQManagedIdentityTest";
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.AZURE);
		relativeDataDirectory = "sanity/framework";
		super.initialize(context);
		
		storageAccount = (AzureStorageAccount) azure.getResource("d4mrnmkspvcstor521", AzureStorageAccount.class);
		iotHub = (AzureIotHub) azure.getResource("d4-mr-ngqapp-iot521", AzureIotHub.class);
		deviceProvisioningService = (AzureDeviceProvisioningService) azure.getResource("d4-mr-orbit-dps521-dps", AzureDeviceProvisioningService.class);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void blobTest(String azureId, String localFilePath) {
		storageAccount.uploadBlob("global", "test", localFilePath);
		report.logStep(TestStep.builder().reportLevel(ReportLevel.PASS).message("Uploaded blob to storage account").build());
		
		storageAccount.downloadBlobAtPath("global", "test");
		report.logStep(TestStep.builder().reportLevel(ReportLevel.PASS).message("Downloaded blobs from global container").build());
		
		String fileName = localFilePath.substring(localFilePath.lastIndexOf("\\") + 1);
		
		report.assertCondition(fileManager.fileExists(log.getLogDirectory() + "d3mrnmkspvcstor521\\global\\test\\" + fileName), true,
				TestStep.builder().message(fileName + " downloaded from blob storage").build());
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void dpsTest(String azureId) {
		report.logStep(TestStep.builder().reportLevel(ReportLevel.PASS)
				.message("Retrieved device twin contents: <textarea>" + iotHub.getDeviceTwinContents(azureId) + "</textarea>").build());

		report.logStep(TestStep.builder().message("App config: <textarea>" + (String) iotHub.getDeviceTwinProperty(azureId, "properties.desired.appConfig") + "</textarea>").build());
		
		String appModel = (String) iotHub.getDeviceTwinProperty(azureId, "tags.phoneData.appModel");
		report.assertValue(appModel, "APP1004", 
				TestStep.builder()
				.message("Device twin property: tags.phoneData.appModel = " + appModel)
				.failMessage("Device twin property mismatch: Expected = APP1004 | Actual = " + appModel).build());
		
		Integer appUpgradeStatus = (Integer) iotHub.getDeviceTwinProperty(azureId, "tags.phoneData.appUpgradeStatus");
		report.assertValue(appUpgradeStatus, 18, 
				TestStep.builder().message("Device twin property appUpgradeStatus: Expected = 18 | Actual = " + appUpgradeStatus).build());

		report.assertCondition(deviceProvisioningService.isDeviceProvisioned(azureId), true,
				TestStep.builder().message("Device is provisioned to IoT hub: " + azureId)
				.failMessage("Device registration could not be found in IoT hub for deviceId = " + azureId).build());
		
		report.assertCondition(deviceProvisioningService.isDeviceInIndividualEnrollment(azureId), false, 
				TestStep.builder().message("Device is not in individual enrollment: " + azureId)
				.failMessage("Device is erroneously in individual enrollment: " + azureId).build());
	}
	
}
