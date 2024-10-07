package com.mnet.framework.email;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import com.mnet.framework.api.APIResponse;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.sun.mail.util.MailSSLSocketFactory;

import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.NoSuchProviderException;
import jakarta.mail.Session;
import jakarta.mail.Store;

/**
 * Functions to perform email operations - connect, disconnect, read inbox &
 * extract authentication code
 * 
 * @author NAIKKX12
 *
 */
public class GmailParser implements EmailParser {

	private String PROPERTY_IMAP_HOST = "mail.imap.host";
	private String PROPERTY_IMAP_PORT = "mail.imap.port";
	private String PROPERTY_IMAP_STARTTLS_ENABLE = "mail.imap.starttls.enable";
	private String PROPERTY_IMAP_SSL_TRUST = "mail.imap.ssl.trust";
	private String PROPERTY_IMAP_SSL_SOCKETFACTORY = "mail.imap.ssl.socketFactory";
	private String AUTHENTICATION_MAIL_SUBJECT = FrameworkProperties.getProperty("TWO_FACTOR_IDENTIFIER");
	private String PROPERTY_EMAIL_PROTOCOL = "mail.store.protocol";

	private static final String EMAIL_HOST = FrameworkProperties.getProperty("EMAIL_HOST");
	private static final String EMAIL_PORT = FrameworkProperties.getProperty("EMAIL_PORT");
	private static final String EMAIL_PASSWORD = FrameworkProperties.getProperty("EMAIL_PASSWORD");

	private FrameworkLog log;
	private Store store;
	private Message[] inboxMsgs;
	private Folder inbox;
	private String host;
	private String user;
	private String password;
	private String port;

	public GmailParser(FrameworkLog frameworkLog) {
		log = frameworkLog;
		host = EMAIL_HOST;
		user = EMAIL_USER_ID;
		password = EMAIL_PASSWORD;
		port = EMAIL_PORT;
		
		connectMailServer();
	}

	/**
	 * Close mailbox and store connection
	 */
	public void close() {
		try {
			store.close();
			if (inbox != null && inbox.isOpen()) {
				inbox.close(false);
			}
		} catch (MessagingException msge) {
			String err = "Failed to close mailbox";
			log.error(err);
			log.printStackTrace(msge);
			throw new RuntimeException(err);
		}
	}

	/**
	 * Check for recent mail received in AUTHENTICATION_CODE_TIMEOUT (max time to
	 * check)
	 * 
	 * @return authentication code extracted from the latest mail
	 */
	public String getAuthenticationCode() {
		String authenticationCode = null;
		Message expectedMsg = null;
		String regex = "<div";
		String startsWith = AUTHENTICATION_CODE_PREFIX;
		String endsWith = "</span>";
		// Max attempts which will be tried to get updated inbox with latest mail
		// received in last AUTHENTICATION_CODE_TIMEOUT
		int totalAttempts = (int)(AUTHENTICATION_CODE_TIMEOUT/2000l);

		try {
			this.readInbox();
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
			Date mostRecentMailDate = dateFormat
					.parse(dateFormat.format(inboxMsgs[inboxMsgs.length - 1].getReceivedDate()));
			Date currentDateTime = dateFormat.parse(dateFormat.format(Calendar.getInstance().getTime()));
			log.info("Current Time noted to check mails: " + currentDateTime);
			int attempt = 0;
			long timeDiff;

			while (attempt < totalAttempts) {
				timeDiff = currentDateTime.getTime() - mostRecentMailDate.getTime();
				if (timeDiff <= AUTHENTICATION_CODE_TIMEOUT) {
					break;
				}
				
				// Wait for one second in between 2 attempts
				Thread.sleep(1000l);
				
				this.readInbox();
				mostRecentMailDate = dateFormat
						.parse(dateFormat.format(inboxMsgs[inboxMsgs.length - 1].getReceivedDate()));
				
				attempt++;
			}

			// Check most recent mail with expected subject line
			for (int index = inboxMsgs.length - 1; index >= 0; index--) {
				if (inboxMsgs[index].getSubject().equals(AUTHENTICATION_MAIL_SUBJECT)) {
					expectedMsg = inboxMsgs[index];
					break;
				}
			}

			if (expectedMsg != null) {
				String code;
				String[] ls = expectedMsg.getContent().toString().split(regex);

				for (int index = 0; index < ls.length; index++) {
					if (ls[index].contains(startsWith)) {
						code = ls[index].substring(ls[index].indexOf(startsWith) + startsWith.length(),
								ls[index].indexOf(endsWith)).trim();
						authenticationCode = code;
						break;
					}
				}
			}
		} catch (MessagingException e) {
			log.error("Issue occured to connect email server and extract the E-mail");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		} catch (ParseException e) {
			log.error("Issue occured while parsing date.");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			log.error("The message content is Null");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return authenticationCode;
	}
	
	//TODO: Need to update function in Gmail Parser
	public Email getEmailWithText(String text, int emailCount) {
		Message expectedMsg = null;
		String emailText = null;
		Email email = new Email();
		// Max attempts which will be tried to get updated inbox with latest mail
		// received in last AUTHENTICATION_CODE_TIMEOUT
		int totalAttempts = (int)(AUTHENTICATION_CODE_TIMEOUT/2000l);

		try {
			SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
			Date mostRecentMailDate = dateFormat
					.parse(dateFormat.format(inboxMsgs[inboxMsgs.length - 1].getReceivedDate()));
			Date currentDateTime = dateFormat.parse(dateFormat.format(Calendar.getInstance().getTime()));
			log.info("Current Time noted to check mails: " + currentDateTime);
			int attempt = 0;
			long timeDiff;

			while (attempt < totalAttempts) {
				timeDiff = currentDateTime.getTime() - mostRecentMailDate.getTime();
				log.info("timeDiff: " + timeDiff);
				if (timeDiff <= AUTHENTICATION_CODE_TIMEOUT && inboxMsgs[inboxMsgs.length - 1].getSubject().contains(text)) {
					log.info("currentDateTime: " + currentDateTime);
					log.info("mostRecentMailDate: " + mostRecentMailDate);
					expectedMsg = inboxMsgs[inboxMsgs.length - 1];
					break;
				}
				// Wait for one second in between 2 attempts
				Thread.sleep(2000l);
				this.readInbox();
				mostRecentMailDate = dateFormat
						.parse(dateFormat.format(inboxMsgs[inboxMsgs.length - 1].getReceivedDate()));
				attempt++;
			}

			if (expectedMsg != null) {
				emailText = expectedMsg.getContent().toString();
				email.setMailBody(emailText);
			}
		} catch (MessagingException e) {
			log.error("Issue occured to connect email server and extract the E-mail");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			log.error("Issue occured to connect email server and extract the Transmission E-mail");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
	
		return email;
	}
	
	public int getEmailCount(String text) {
		return getEmailCount(text);
	}
	
	/*
	 * Helper functions
	 */
	
	/**
	 * Read all message from the inbox
	 * 
	 * @return Message Array with all messages in inbox
	 */
	private Message[] readInbox() {

		try {
			if (!store.isConnected()) {
				store.connect(host, user, password);
			}
			// create the inbox object and open it
			inbox = store.getFolder("Inbox");
			inbox.open(Folder.READ_ONLY);

			inboxMsgs = inbox.getMessages();
			log.info("Total messages in the inbox are: " + inboxMsgs.length);
		} catch (NoSuchProviderException e) {
			String err = "Failed to read messages from the email inbox - provider does not exist";
			log.error(err);
			log.printStackTrace(e);
			throw new RuntimeException(err);
		} catch (MessagingException e) {
			String err = "Issue occured to read messages from the email inbox";
			log.error(err);
			log.printStackTrace(e);
			throw new RuntimeException(err);
		}
		return inboxMsgs;
	}
	
	private void connectMailServer() {
		Properties properties = new Properties();
		MailSSLSocketFactory socketFactory = null;
		try {
			socketFactory = new MailSSLSocketFactory();
		} catch (GeneralSecurityException gse) {
			String err = "Failed to create socket factory object";
			log.error(err);
			log.printStackTrace(gse);
			throw new RuntimeException(err);
		}

		socketFactory.setTrustAllHosts(true); 
		properties.put(PROPERTY_IMAP_SSL_SOCKETFACTORY, socketFactory);
		properties.put(PROPERTY_IMAP_HOST, host);
		properties.put(PROPERTY_IMAP_PORT, port);
		properties.put(PROPERTY_IMAP_STARTTLS_ENABLE, "true");
		properties.put(PROPERTY_IMAP_SSL_TRUST, "*");
		properties.put(PROPERTY_IMAP_SSL_TRUST, host);
		properties.put(PROPERTY_EMAIL_PROTOCOL, "imaps");
		try {
			

			Session emailSession = Session.getDefaultInstance(properties);
			// Set below option for debug logs
			emailSession.setDebug(true);
			store = emailSession.getStore("imaps");
			store.connect(host, user, password);
		} catch (MessagingException msge) {
			String err = "Failed to connect to email server: '" + host + "' with user: " + user + "|" + password;
			log.error(err);
			log.printStackTrace(msge);
			throw new RuntimeException(err);
		}
	}
}