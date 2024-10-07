package com.mnet.framework.email;

import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.jsoup.Jsoup;

import com.google.common.base.CharMatcher;
import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.APIResponse;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.Timeout;

/**
 * Parses Outlook emails for authentication code via Microsoft Graph API.
 * @implNote Bearer token in application properties can be obtained by logging into https://developer.microsoft.com/graph/graph-explorer
 * @version Spring 2023
 * @author Arya Biswas
 */
public class OutlookParser implements EmailParser {

	/**@apiNote https://graph.microsoft.com/v1.0/me/messages*/
	private static final String OUTLOOK_SEARCH_API = FrameworkProperties.getProperty("OUTLOOK_SEARCH_API");
	
	/**@apiNote graph.microsoft.com*/
	private static final String OUTLOOK_API_HOST = 
			StringUtils.substringBefore(StringUtils.substringAfter(OUTLOOK_SEARCH_API, "https://"), "/");
	
	/**Unique string in email body used to identify 2FA emails.
	 *@implNote "d2mrb2cus" for Merlin.net*/
	private static final String TWO_FACTOR_IDENTIFIER = FrameworkProperties.getProperty("TWO_FACTOR_IDENTIFIER");
	
	/** Maximum mail count to receive during search
	 */
	private static final int OUTLOOK_MAX_AUTH_MAILS = Integer.parseInt(FrameworkProperties.getProperty("OUTLOOK_MAX_AUTH_MAILS")); 
	/**
	 * Bearer token for Microsoft Graph API. 
	 * @implNote Can be obtained by logging into https://developer.microsoft.com/graph/graph-explorer and viewing 'Access Token' under any API.
	 */
	private static final String OUTLOOK_BEARER_TOKEN = FrameworkProperties.getProperty("OUTLOOK_BEARER_TOKEN");
	
	/**
	 * Uses proxy host to mimic Postman invocation behavior. @implNote Usually localhost or http://127.0.0.1
	 */
	private static final String OUTLOOK_PROXY_HOST = FrameworkProperties.getProperty("OUTLOOK_PROXY_HOST");
	
	/**
	 * Uses proxy port to mimic Postman invocation behavior. @implNote 9000
	 */
	private static final int OUTLOOK_PROXY_PORT = Integer.parseInt(FrameworkProperties.getProperty("OUTLOOK_PROXY_PORT"));
	
	private FrameworkLog log;
	private String authenticationToken;
	
	public OutlookParser(FrameworkLog frameworkLog) {
		log = frameworkLog;
		authenticationToken = OUTLOOK_BEARER_TOKEN;
	}
	
	public OutlookParser(String bearerToken, FrameworkLog frameworkLog) {
		log = frameworkLog;
		authenticationToken = bearerToken;
	}
	
	public String getAuthenticationCode() {
		Integer initialEmailMessages = getTwoFactorEmailCount(TWO_FACTOR_IDENTIFIER);
		Integer currentEmailMessages = 0;
		long startTime = System.nanoTime();
		
		while (CommonUtils.millisFromTime(startTime) < AUTHENTICATION_CODE_TIMEOUT) {
			currentEmailMessages = getTwoFactorEmailCount(TWO_FACTOR_IDENTIFIER);	
			
			if (currentEmailMessages > initialEmailMessages) {
				break;
			}
			
			Timeout.waitForTimeout(log, 5000L);
		}
		
		
		
		if(currentEmailMessages > initialEmailMessages) {
			String emailBody = getSearchAPIResponse(TWO_FACTOR_IDENTIFIER).getStringFromJsonPath("value[0].bodyPreview");
			String truncatedEmailBody = StringUtils.substringBefore(StringUtils.substringAfter(emailBody, AUTHENTICATION_CODE_PREFIX), "\n");
			
			return CharMatcher.inRange('0','9').retainFrom(truncatedEmailBody);
		}
		
		String emailBody = getSearchAPIResponse(TWO_FACTOR_IDENTIFIER).getStringFromJsonPath("value[0].bodyPreview");
		String truncatedEmailBody = StringUtils.substringBefore(StringUtils.substringAfter(emailBody, AUTHENTICATION_CODE_PREFIX), "\n");
		return CharMatcher.inRange('0','9').retainFrom(truncatedEmailBody);
	}
	
	public Email getEmailWithText(String text, int emailCount) {
		Integer initialEmailMessages = emailCount;
		Integer currentEmailMessages = 0;
		long startTime = System.nanoTime();
		
		while (CommonUtils.millisFromTime(startTime) < AUTHENTICATION_CODE_TIMEOUT) {
			currentEmailMessages = getTwoFactorEmailCount(text);
			if (currentEmailMessages > initialEmailMessages) {
					break;
			}
					
			Timeout.waitForTimeout(log, 5000L);
		}
		
		APIResponse emailBody = null;
		if(currentEmailMessages > initialEmailMessages) {
			emailBody = getSearchAPIResponse(text);
		}
		
		Email emailData = new Email();
		emailData.setMailBody(Jsoup.parse(emailBody.getStringFromJsonPath("value[0].body.content")).text());
		emailData.setSubject(emailBody.getStringFromJsonPath("value[0].subject"));
		emailData.setHasAttachments(emailBody.getStringFromJsonPath("value[0].hasAttachments"));
		emailData.setSenderEmail(emailBody.getStringFromJsonPath("value[0].sender.emailAddress.address"));
		emailData.setMailBodyPreview(emailBody.getStringFromJsonPath("value[0].bodyPreview"));
		
		return emailData;

	}
	
	public int getEmailCount(String text) {
		return getTwoFactorEmailCount(text);
	}
	
	/*
	 * Helper functions
	 */
	
	private Integer getTwoFactorEmailCount(String text) {
		APIResponse apiResponse = getSearchAPIResponse(text);
		
		if (apiResponse.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
			String err = "Outlook bearer token is invalid or expired - please obtain a new bearer token: https://developer.microsoft.com/en-us/graph/graph-explorer";
			log.error(apiResponse.toString());
			log.error(err);
			throw new RuntimeException(err);
		}
		
		return (Integer) apiResponse.getNumberFromJsonPath("value.size()");
	}
	
	/*
	 * Additional headers are to match Postman invocation behavior to avoid 2FA authentication failure.
	 */
	private APIResponse getSearchAPIResponse(String identifier) {
		APIRequest request = new APIRequest(OUTLOOK_SEARCH_API,
				Map.of("$search", identifier, "$count", true, "$top", OUTLOOK_MAX_AUTH_MAILS), 
				Map.of("Authorization", "Bearer " + authenticationToken,
						"User-Agent", "PostmanRuntime/7.32.2",
						"Accept", "*/*",
						"Postman-Token", UUID.randomUUID().toString(),
						"Host", OUTLOOK_API_HOST,
						"Accept-Encoding", "gzip, deflate, br",
						"Connection", "keep-alive"));
		
		log.info(request.asLoggableString());
		
		return RestAPIManager.get(request, OUTLOOK_PROXY_HOST, OUTLOOK_PROXY_PORT);
	}
}
