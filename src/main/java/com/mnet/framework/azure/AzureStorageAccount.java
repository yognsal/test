package com.mnet.framework.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobContainerItem;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.utilities.FileUtilities;

/***
 * Represents an Azure storage account connection authenticated using managed identity credentials.
 * Range of acceptable operations correspond to the RBAC provided
 * to the service principal for the storage account resource.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class AzureStorageAccount extends AzureResource {

	private BlobServiceClient blobServiceClient;
	private Map<String, AzureStorageContainer> blobContainers = new HashMap<String, AzureStorageContainer>();
	
	private FileUtilities fileManager;
	
	protected AzureStorageAccount(FrameworkLog frameworkLog, FileUtilities fileUtility,
			TokenCredential credential, String storageAccountName) {
		super(storageAccountName, frameworkLog);
		
		fileManager = fileUtility;
				
		blobServiceClient = new BlobServiceClientBuilder()
				.endpoint("https://" + storageAccountName +".blob.core.windows.net/")
				.credential(credential)
				.buildClient();
				
		PagedIterable<BlobContainerItem> azureBlobContainers = blobServiceClient.listBlobContainers();
		
		for (BlobContainerItem azureBlobContainer : azureBlobContainers) {
			String containerName = azureBlobContainer.getName();
			
			blobContainers.put(containerName, null);
		}	
	}
	
	/**
	 * Returns the set of blob container names associated with the storage account.
	 */
	public Set<String> getBlobContainerNames() {
		return blobContainers.keySet();
	}
	
	/**
	 * Returns the set of blob paths associated with the specified directory in the blob container.
	 * @param containerName Name of the top-level storage container as found under Data Storage -> Containers for the given storage account.
	 * @param directory Name of the directory or subdirectory (e.x NGQ/2.0) under which to search blobs
	 */
	public Set<String> listBlobs(String containerName, String directory) {
		return getAzureStorageContainer(containerName).listBlobs(directory);
	}
	
	/**
	 * Returns all blob paths associated with the specified blob container.
	 * @param containerName Name of the top-level storage container as found under Data Storage -> Containers for the given storage account.
	 */
	public Set<String> listBlobs(String containerName) {
		return listBlobs(containerName, "");
	}
	
	/**
	 * Downloads all blob directory contents to logging file path (replicating storage container directory structure).
	 * If the file already exists locally, overwrites it.
	 * @param containerName Name of the top-level storage container as found under Data Storage -> Containers for the given storage account.
	 * @param directory Directory, subdirectory, or blob file path to download blobs from.
	 * @return Local file path where blobs were downloaded.
	 * @implNote Blob hierarchy: storageAccount/containerName/directory e.x. d3mrnmkspvcstor521/tanto-patient-profile/EX1150
	 */
	public String downloadBlobAtPath(String containerName, String directory) {
		return getAzureStorageContainer(containerName).downloadBlob(directory);
	}
	
	/**
	 * Uploads a local file to storage container as a blob at the destination directory path.
	 * If the blob already exists, overwrites the existing blob.
	 * @param containerName Name of the top-level storage container as found under Data Storage -> Containers for the given storage account.
	 * @param directory Directory, subdirectory, or blob file path in storage container.
	 * @param localFilePath Local file (with fully qualified path) to upload to blob storage.
	 */
	public void uploadBlob(String containerName, String directory, String localFilePath) {
		getAzureStorageContainer(containerName).uploadBlob(directory, localFilePath);
	}
	
	/*
	 * Local helper functions
	 */
	
	private AzureStorageContainer getAzureStorageContainer(String containerName) {
		if (!blobContainers.containsKey(containerName)) {
			throw new RuntimeException("No storage container exists with name: " + containerName);
		}
		
		AzureStorageContainer storageContainer = blobContainers.get(containerName);
		
		if (storageContainer == null) {
			storageContainer = new AzureStorageContainer(log, fileManager, blobServiceClient, containerName);
			
			blobContainers.put(containerName, storageContainer);
		}
		
		return storageContainer;
	}
}
