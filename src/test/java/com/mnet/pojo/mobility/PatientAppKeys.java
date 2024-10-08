package com.mnet.pojo.mobility;

import lombok.Getter;

/**
 * Represents the public-private key pair associated with a patient app instance.
 * 
 * @author Arya Biswas
 * @version Fall 2023
 */
@Getter
public class PatientAppKeys {

	private String publicKeyEncoded, privateKeyURLEncoded, privateKeyEncoded;
	
	public PatientAppKeys(String publicKeyEncoded, String privateKeyURLEncoded) {
		this.publicKeyEncoded = publicKeyEncoded;
		this.privateKeyURLEncoded = privateKeyURLEncoded;
		// modify URL encoded characters and pad
		this.privateKeyEncoded = privateKeyURLEncoded.replace("-", "+").replace("_", "/") + "==";
	}
	
}
