package com.mnet.pojo.mobility;

import lombok.Data;

@Data
public class ActivationCodeData {
	
	private String failureCount;
	private String retryCount;
	private String code;
	private String codeValue;

}
