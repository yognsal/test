package com.mnet.framework.azure;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.windowsazure.messaging.NotificationStatus;

/**
 * Represents an Azure notification hub namespace API client for tracking push notifications.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class AzureNotificationHubNamespace extends AzureResource {

	private static final String NOTIFICATION_HUB_SAS_KEY_NAME = FrameworkProperties.getProperty("NOTIFICATION_HUB_SAS_KEY_NAME");
	private static final String NOTIFICATION_HUB_SAS_KEY = FrameworkProperties.getProperty("NOTIFICATION_HUB_SAS_KEY");
	
	private static final String BASE_URI = "https://{{NHnamespace}}.servicebus.windows.net";
	private static final String NOTIFICATION_MESSAGE_PATH = "/{{NHub}}/messages/";
	private static final String CONTENT_TYPE = "application/xml;type=entry;charset=utf-8";
	private static final String API_VERSION = "2016-07";
	private static final String X_MS_VERSION = "2015-01";
	
	private static final Long API_NOTIFICATION_MESSAGE_TOKEN_TTL = Long.parseLong(
			FrameworkProperties.getProperty("API_NOTIFICATION_MESSAGE_TOKEN_TTL"));
	
	private static final Encoder BASE64 = Base64.getEncoder();
	
	private Set<NotificationStatus> allStatus = EnumSet.allOf(NotificationStatus.class);
	
	protected AzureNotificationHubNamespace(FrameworkLog frameworkLog, String resourceName) {
		super(BASE_URI.replace("{{NHnamespace}}", resourceName), frameworkLog);
	}
	
	/**
	 * Determines status of push notification was associated with the given notification hub, if it exists.
	 * @return NotificationStatus corresponding to the push notification, or null if the push notification does not exist.
	 */
	public NotificationStatus getNotificationStatus(String notificationHubName, String notificationId) {
		String notificationMessageURI = name + NOTIFICATION_MESSAGE_PATH.replace("{{NHub}}", notificationHubName) + notificationId;
		
		try {
			Thread.sleep(Long.parseLong(FrameworkProperties.getProperty("BROWSER_MIN_TIMEOUT")) * 3);
		}catch (Exception e){
			log.warn("Failed during sleep");
	        log.printStackTrace(e);
		}
		APIRequest request = new APIRequest(notificationMessageURI, 
						Map.of("api-version", API_VERSION), 
						Map.of("Authorization", getSASToken(), 
								"Content-Type", CONTENT_TYPE, 
								"x-ms-version", X_MS_VERSION), 
						CONTENT_TYPE);

		String response = RestAPIManager.get(request).toString();
		
		for (NotificationStatus status : allStatus) {
			if (response.contains(status.toString())) {
				return status;
			}
		}
		
		return null;
	}
	
	/*
	 * Local helper functions
	 */
	
	/**@see {@link https://learn.microsoft.com/en-us/rest/api/eventhub/generate-sas-token}*/
	private String getSASToken() {
		String timeToLive = Long.toString((System.currentTimeMillis() / 1000L) + API_NOTIFICATION_MESSAGE_TOKEN_TTL);
		String content = urlEncode(name) + "\n" + timeToLive;
		
		return "SharedAccessSignature sr=" + urlEncode(name)
				+ "&sig=" + urlEncode(getSignature(content))
				+ "&se=" + timeToLive
				+ "&skn=" + NOTIFICATION_HUB_SAS_KEY_NAME;
	}
	
	private String urlEncode(String content) {
		try {
			return URLEncoder.encode(content, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			String err = "Failed to URL encode string to UTF-8";
			log.error(err);
			log.printStackTrace(uee);
			throw new RuntimeException(err);
		}
	}
	
	/**Retrieves signature of content hashed using SHA-256 and the SAS Key.*/
	private String getSignature(String content) {
		Mac sha256;
		
		try {
			sha256 = Mac.getInstance("HmacSHA256");
			sha256.init(new SecretKeySpec(NOTIFICATION_HUB_SAS_KEY.getBytes(), "HmacSHA256"));
			return new String(BASE64.encode(sha256.doFinal(content.getBytes("UTF-8"))));
		} catch (NoSuchAlgorithmException | InvalidKeyException | UnsupportedEncodingException e) {
			String err = "Failed to hash signature using SHA256";
			log.error(err);
			log.printStackTrace(e);
			throw new RuntimeException(err);
		}
	}
	

}
