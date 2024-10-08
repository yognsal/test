package com.mnet.pojo.mobility;

import java.sql.Timestamp;

import com.mnet.mobility.utilities.Keyfactor.CertificateTemplate;

import lombok.Getter;

/**
 * Represents a Patient App certificate provisioned to Keyfactor.
 * @author Arya Biswas
 * @version Fall 2023
 */
@Getter
public class PatientAppCertificate {

	/**Template name associated with this certificate.*/
	private CertificateTemplate type;
	
	/**Determines whether the certificate is active or revoked.*/
	private boolean active;
	
	/**Date on which the certificate was provisioned.*/
	private Timestamp issuedDate;
	
	/**Date on which the certificate expires.*/
	private Timestamp expirationDate;
	
	public PatientAppCertificate(CertificateTemplate certificateType, boolean isActive, Timestamp issued, Timestamp expires) {
		type = certificateType;
		active = isActive;
		issuedDate = issued;
		expirationDate = expires;
	}
}
