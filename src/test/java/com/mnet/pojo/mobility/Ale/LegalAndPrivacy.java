package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * ALE used to communicate status of Patient App legal / privacy notice acceptance by patient.
 */
public class LegalAndPrivacy extends AppLifeEvent {
	
	private static final String LEGAL_AND_PRIVACY_CODE = "4012";
	
	/**Indicates the type of consent provided by Legal And Privacy ALE.*/
	public enum LegalAndPrivacyConsent {
		TERMS_ACCEPTED("{\n\t\"type\": \"termsAccepted\"\n}"),
		NOTICE_ACKNOWLEDGED("{\n\t\"type\": \"noticeAcknowledged\"\n}"),
		ALL(TERMS_ACCEPTED.getContent() + ",\n" + NOTICE_ACKNOWLEDGED.getContent());
		
		@Getter(AccessLevel.PRIVATE)
		private String content;
		
		private LegalAndPrivacyConsent(String contentValue) {
			content = contentValue;
		}
	}
	
	/**
	 * @param patientApp Patient App instance to be bonded.
	 * @param consent Type of consent to be provided by Legal And Privacy ALE.
	 */
	public LegalAndPrivacy(PatientApp patientApp, LegalAndPrivacyConsent consent) {
		super("legalAndPrivacy", AppLifeEventResult.UNKNOWN,
				Map.of("consent_provided", consent.getContent(),
						"locale", patientApp.getPhoneData().getLocale(),
						"app_time", System.currentTimeMillis(),
						"azure_id", patientApp.getAzureId(),
						"patient_app_ale_id", MobilityUtilities.getUUID(),
						"user_record_id", (patientApp.getIdentity() != null) ? patientApp.getIdentity().getUserRecordId() : 0));
	}
	
	@Override
	public String getALETypeCode() {
		return LEGAL_AND_PRIVACY_CODE;
	}
}
