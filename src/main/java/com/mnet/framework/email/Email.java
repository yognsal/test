package com.mnet.framework.email;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO to store email content values (e.g. Sender, Email body etc)
 * @author kotwarx2
 *
 */

@Getter @Setter(AccessLevel.PROTECTED) 
public class Email {
	
	private String mailBody;
	private String subject;
	private String hasAttachments;
	private String senderEmail;
	private String mailBodyPreview;

}
