package com.mnet.framework.azure;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.utilities.FileUtilities;

/**
 * Represents an Azure storage container and its associated blobs.
 * @implNote Not intended to be accessed outside framework scope.
 * @author Arya Biswas
 * @version Fall 2023
 */
class AzureStorageContainer {

	private BlobContainerClient containerClient;
	private Map<String, BlobClient> blobs = new HashMap<String, BlobClient>();
	
	private FrameworkLog log;
	private FileUtilities fileManager;
	
	protected AzureStorageContainer(FrameworkLog frameworkLog, FileUtilities fileUtility,
			BlobServiceClient blobServiceClient, String containerName) {
		log = frameworkLog;
		fileManager = fileUtility;
		
		containerClient = blobServiceClient.getBlobContainerClient(containerName);
		
		PagedIterable<BlobItem> azureBlobs = containerClient.listBlobs();
		
		for (BlobItem azureBlob : azureBlobs) {	
			blobs.put(azureBlob.getName(), null);
		}
	}
	
	/**
	 * Lists all blobs which partially or completely match the directory path provided. 
	 **/
	protected Set<String> listBlobs(String directory) {
		Set<String> allBlobs = blobs.keySet();
		Set<String> directoryBlobs = new HashSet<String>();
		
		for (String blobName : allBlobs) {
			if (blobName.contains(directory) && blobName.contains(".")) { // represents a file
				directoryBlobs.add(blobName);
			}
		}
		
		return directoryBlobs;
	}
	
	/**
	 * Downloads all blobs located in the directory or subdirectory to the local logging directory.
	 * If the file already exists locally, overwrites it.
	 * @param directory Directory, subdirectory, or blob file path.
	 * @return Local file path where blobs were downloaded.
	 */
	protected String downloadBlob(String directory) {
		Set<String> directoryBlobs = listBlobs(directory);
		String logDirectory = log.getLogDirectory();
		
		String baseDirectoryPath = logDirectory 
				+ File.separator + containerClient.getAccountName()
				+ File.separator + containerClient.getBlobContainerName() + File.separator;
		
		for (String blobName : directoryBlobs) {
			String directoryPath = baseDirectoryPath;
			
			directoryPath += (blobName.contains("/")) 
					? blobName.substring(0, blobName.lastIndexOf("/") + 1) 
					: blobName;
			fileManager.createDirectory(directoryPath);
			
			getBlobClient(blobName, true).downloadToFile(baseDirectoryPath + blobName, true);
		}
		
		return baseDirectoryPath + directory;
	}
	
	/**
	 * Uploads a blob to the directory path defined for the storage container.
	 * If the blob already exists, overwrites the existing blob.
	 * @param directory Directory, subdirectory, or blob file path in storage container.
	 * @param localFilePath Local file (with fully qualified path) to upload to blob storage.
	 */
	protected void uploadBlob(String directory, String localFilePath) {
		String fileName = (localFilePath.contains(File.separator)) 
				? localFilePath.substring(localFilePath.lastIndexOf(File.separator) + 1)
				: localFilePath;
		
		if (!directory.contains(".")) {
			directory += "/" + fileName;
		}
		
		getBlobClient(directory, false).uploadFromFile(localFilePath, true);
	}
	
	/**
	 * Local helper functions
	 */
	
	/**
	 * @param useExistingBlob Determines whether blob client should be instantiated if it doesn't already exist. 
	 **/
	private BlobClient getBlobClient(String blobPath, boolean useExistingBlob) {
		if (useExistingBlob && !blobs.containsKey(blobPath)) {
			throw new RuntimeException("Blob does not exist - "
					+ "storage account: " + containerClient.getAccountName() + " | "
					+ "container: " + containerClient.getBlobContainerName() + " | "
					+ "directory path: " + blobPath);
		}
		
		BlobClient blobClient = blobs.get(blobPath);
		
		if (blobClient == null) {
			blobClient = containerClient.getBlobClient(blobPath);
			blobs.put(blobPath, blobClient);
		}
		
		return blobClient;
	}
	
	
}
