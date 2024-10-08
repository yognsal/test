package com.mnet.mobility.utilities;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.APIRequest.APICharset;
import com.mnet.framework.api.APIResponse;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.pojo.mobility.PatientAppCertificate;

import io.restassured.http.ContentType;

/**
 * Convenience utility to fetch certificates from PKI Keyfactor (CMS) console.
 * @author Arya Biswas
 * @version Fall 2023
 */
public class Keyfactor {
	
	private FrameworkLog log;
	
	private String imdIdentityQuery = "page=1&rp=50&sortname=IssuedDN&sortorder=asc&query=IMDSerialNumber+-eq+%22<DEVICE_SERIAL>%22+AND+TemplateShortName+-contains+%22AbbottIMDIdentity%22&qtype=&qid=0&qsecurity=%5Bobject+Object%5D";
	private String patientAppIdentityQuery = "page=1&rp=50&sortname=IssuedDN&sortorder=asc&query=AzureUUID+-eq+%22<AZURE_ID>%22+AND+TemplateShortName+-contains+%22PatientAppIdentity%22&qtype=&qid=0&qsecurity=%5Bobject+Object%5D";
	private String patientAppCredentialsQuery = "page=1&rp=50&sortname=IssuedDN&sortorder=asc&query=AzureUUID+-eq+%22<AZURE_ID>%22+AND+TemplateShortName+-contains+%22PatientAppCredential%22&qtype=&qid=0&qsecurity=%5Bobject+Object%5D";

	private static final String API_KEYFACTOR_URI = FrameworkProperties.getProperty("API_KEYFACTOR_URI");
	private static final String API_KEYFACTOR_AUTH = FrameworkProperties.getProperty("API_KEYFACTOR_AUTH");
	
	/**Represents a type of certificate provisioned to Keyfactor.*/
	public enum CertificateTemplate {
		AbbottIMDIdentity,
		AbbottPatientAppIdentity,
		AbbottPatientAppCredential
	}
	
	public Keyfactor(FrameworkLog frameworkLog) {
		log = frameworkLog;
	}
	
	public boolean hasActiveIMDIdentity(int deviceSerial) {
		Set<PatientAppCertificate> imdIdentityCerts = getCertificates(deviceSerial, null, CertificateTemplate.AbbottIMDIdentity);
		
		for (PatientAppCertificate certificate : imdIdentityCerts) {
			if (certificate.isActive()) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasActivePatientAppCertificates(String azureId) {
		Set<PatientAppCertificate> patientAppCerts = getCertificates(0, azureId, CertificateTemplate.AbbottPatientAppCredential, CertificateTemplate.AbbottPatientAppIdentity);
		
		boolean activePatientAppIdentity = false;
		boolean activePatientAppCredential = false;
		
		for (PatientAppCertificate certificate : patientAppCerts) {
			if (!activePatientAppIdentity && (certificate.getType() == CertificateTemplate.AbbottPatientAppIdentity)) {
				activePatientAppIdentity = (certificate.isActive()) ? true : false;
			}
			
			if (!activePatientAppCredential && (certificate.getType() == CertificateTemplate.AbbottPatientAppCredential)) {
				activePatientAppCredential = (certificate.isActive()) ? true : false;
			}
		}
		
		return (activePatientAppIdentity && activePatientAppCredential);
	}
	
	public Set<PatientAppCertificate> getCertificates(int deviceSerial, String azureId, CertificateTemplate... certificateTypes) {
		Set<PatientAppCertificate> certificates = new HashSet<>();
		Set<CertificateTemplate> certTypes = new HashSet<>();
		Collections.addAll(certTypes, certificateTypes); // remove duplicates from varargs
		
		String query;
		
		for (CertificateTemplate certType : certTypes) {
			switch (certType) {
				case AbbottIMDIdentity:
					query = imdIdentityQuery.replace("<DEVICE_SERIAL>", Integer.toString(deviceSerial));
					break;
				case AbbottPatientAppIdentity:
					query = patientAppIdentityQuery.replace("<AZURE_ID>", azureId);
					break;
				case AbbottPatientAppCredential:
					query = patientAppCredentialsQuery.replace("<AZURE_ID>", azureId);
					break;
				default:
					throw new RuntimeException("Invalid patient app certificate type requested");
			}
			
			certificates.addAll(getCertificatesFromResponse(certType, queryCertificateStore(query)));
		}
		
		return certificates;
	}
	
	/*
	 * Helper functions
	 */
	
	private APIResponse queryCertificateStore(String query) {
		Map<String, Object> headers = Map.ofEntries(
				Map.entry("Authorization", API_KEYFACTOR_AUTH),
				Map.entry("X-Requested-With", "XMLHttpRequest")
		);
		
		APIRequest request = new APIRequest(API_KEYFACTOR_URI, headers, query, ContentType.URLENC, APICharset.UTF_8);
		log.info(request.asLoggableString());
		
		return RestAPIManager.post(request);
	}
	
	private Set<PatientAppCertificate> getCertificatesFromResponse(CertificateTemplate certificateType, APIResponse response) {
		Set<PatientAppCertificate> certificates = new HashSet<>();
		
		int totalEntries = (int) response.getNumberFromJsonPath("total");
		
		for (int i = 0; i < totalEntries; i++) {
			String row = "rows[" + i + "]";
			Timestamp provisionedDate = Timestamp.from(Instant.parse(response.getStringFromJsonPath(row + ".cell[1]") + "Z"));
			Timestamp expiryDate = Timestamp.from(Instant.parse(response.getStringFromJsonPath(row + ".cell[2]") + "Z"));
			boolean isActive = (response.getStringFromJsonPath(row + ".cell[11]").contains("Active")) ? true : false;
			
			certificates.add(new PatientAppCertificate(certificateType, isActive, provisionedDate, expiryDate));
		}
		
		return certificates;
	}
	
	
}
