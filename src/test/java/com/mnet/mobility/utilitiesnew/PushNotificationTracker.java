package com.mnet.mobility.utilities;

import com.mnet.framework.azure.AzureManagedIdentity;
import com.mnet.framework.azure.AzureNotificationHubNamespace;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.reporting.utilities.GraylogArchive;
import com.mnet.reporting.utilities.GraylogReporting;
import com.windowsazure.messaging.NotificationStatus;

/**
 * Provides convenience function to parse push notification hub ID from microservices
 * and search for the push notification in the applicable notificatin hub.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class PushNotificationTracker implements GraylogReporting {

	private FrameworkLog log;
	private TestReporter report;
	private AzureNotificationHubNamespace namespace;
	
	private static final String NOTIFICATION_HUB_FORMAT = FrameworkProperties.getProperty("NOTIFICATION_HUB_FORMAT");
	private static final String NOTIFICATION_DELIMITER = "notificationId = ";
	
	public PushNotificationTracker(FrameworkLog frameworkLog, TestReporter testReporter, AzureManagedIdentity managedIdentity) {
		log = frameworkLog;
		report = testReporter;
		
		namespace = (AzureNotificationHubNamespace) managedIdentity.getResource(
				FrameworkProperties.getProperty("NOTIFICATION_HUB_NAMESPACE"), 
				AzureNotificationHubNamespace.class);
	}
	
	/**
	 * Identifies push notification sent from the relevant microservice
	 * and determines the status of this notification, if it exists.
	 * @param azureId Unique UUID associated with the patient app registration.
	 * @param packageIdRegion Region code associated with the phone packageId e.x. If packageId = com.abbott.crm.ngq.patient.tuv, region = tuv
	 * @param microservice Microservice where push notification ID is expected to be logged.
	 * @param startTime Start time of microservice log query, in epoch milliseconds.
	 * @param endTime End time of microservice log query, in epoch milliseconds.
	 * @return NotificationStatus corresponding to the sent push notification, or null if it does not exist.
	 */
	public NotificationStatus getPushNotificationStatus(String azureId, String packageIdRegion, Microservice microservice, long startTime) {
		GraylogArchive graylog = getMicroserviceLogsInInterval(startTime, log, report, microservice);		
		String logText = graylog.getReportText();
		
		graylog.writeAll();
		
		int azureIndex = logText.lastIndexOf("[AzureId=" + azureId);
		
		String notificationId; 
		
		try {
			logText = logText.substring(0, azureIndex);
			int notificationIndex = logText.lastIndexOf(NOTIFICATION_DELIMITER) + NOTIFICATION_DELIMITER.length();
			
			notificationId = logText.substring(notificationIndex, logText.lastIndexOf(","));
		} catch (IndexOutOfBoundsException iobe) {
			throw new RuntimeException("Notification ID could not be located in " + microservice.toString() + " for azureId = " + azureId);
		}
		
		return namespace.getNotificationStatus(
				NOTIFICATION_HUB_FORMAT.replace("<REGION>", packageIdRegion.toLowerCase()), 
				notificationId);
	}
	
}
