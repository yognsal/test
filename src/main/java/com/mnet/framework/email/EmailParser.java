package com.mnet.framework.email;

import com.mnet.framework.api.APIResponse;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.utilities.CommonUtils;

/**
 * Defines common email parsing utilities for two-factor authentication.
 * Supports parsing via Outlook and Gmail.
 * @version Spring 2023
 * @author Arya Biswas
 * */
public interface EmailParser {

	final String EMAIL_USER_ID = FrameworkProperties.getProperty("EMAIL_USER_ID");
	final String AUTHENTICATION_CODE_PREFIX = FrameworkProperties.getProperty("AUTHENTICATION_CODE_PREFIX");
	final long AUTHENTICATION_CODE_TIMEOUT = Long.parseLong(FrameworkProperties.getProperty("AUTHENTICATION_CODE_TIMEOUT"));
	final boolean DOMAIN_ALIAS_AVAILABLE = Boolean.parseBoolean(FrameworkProperties.getProperty("DOMAIN_ALIAS_AVAILABLE"));
	String temporaryMFA = CommonUtils.generateRandomEmail(); 
	
	/**Retrieves authentication code from email configured in application properties.*/
	//TODO: Need to move this at test utility level by calling getEmailWithText function internally
	String getAuthenticationCode();
	
	/**Retrieves text from email based on subject line passed as parameter configured in application properties.
	 * @param emailCount: It is the initial email count before checking the inbox from new emails, retrieved using getEmailCount(String)*/
	Email getEmailWithText(String text, int emailCount);
	
	/**Gets the count of emails based on the subject line passed as parameter configured in application properties.*/
	int getEmailCount(String text);
	
	/**
	 * Handles cleanup for I/O objects, etc.
	 * Override this method to provide an implementation, if applicable.
	 */
	default void close() {
		return;
	}
	
	// TODO: Database updates should not be done in framework - move to LoginUtilities
	
	/**
	 * Get unused email id in case alias is available; return mail id from properties file by replacing current entries with random email
	 * @return
	 */
	static String getMFAEmailID() {
		QueryResult queryResult;
		String email_id = EMAIL_USER_ID;
		DatabaseConnector database = new DatabaseConnector(null);
		
		String query = "select mfa_email_address from users.customer_account ca where mfa_email_address = '<email_id>'";
		String user_id = EMAIL_USER_ID.substring(0, EMAIL_USER_ID.indexOf("@"));
		user_id = user_id.indexOf("+") > 0 ? user_id.substring(0, user_id.indexOf("+")) : user_id; 
		String domain = EMAIL_USER_ID.substring(EMAIL_USER_ID.indexOf("@") + 1);
		queryResult = database.executeQuery(query.replace("<email_id>", email_id));
		
		if (DOMAIN_ALIAS_AVAILABLE) {						
			while (true) {
				if (queryResult.getAllRows().isEmpty()) {
					return email_id;
				}
				email_id = user_id + "+" + String.valueOf(CommonUtils.getRandomNumber(1, 3000)) + "@" + domain;
				queryResult = database.executeQuery(query.replace("<email_id>", email_id));
			}
		} else {
			if (!queryResult.getAllRows().isEmpty()) {
				query = "update users.customer_account set mfa_email_address = '<updatedEmail>' where mfa_email_address = '<oldEmail>'";
				query = query.replace("<updatedEmail>", temporaryMFA);
				query = query.replace("<oldEmail>", email_id);				
			}
			return email_id;
		}
	}
}
