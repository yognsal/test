package com.mnet.pojo.mobility.ale;

import java.util.Map;

import com.mnet.framework.utilities.CommonUtils;
import com.mnet.framework.utilities.CommonUtils.StringType;
import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.pojo.mobility.AppLifeEvent;

import lombok.Getter;
import lombok.Setter;

/**
 * ALE used to communicate status of patient app pairing with implanted device.
 */
public class IMDBondingStatus extends AppLifeEvent {
	
	private static final String IMD_BONDING_STATUS_CODE = "4003";
	
	@Getter @Setter
	private static String dimVersion = MobilityUtilities.getMobilityProperty("DIM_MAJOR_VERSION") + CommonUtils.randomString(2, StringType.NUMBER);
	
	
	/**
	 * @param aleResult Desired result code for ALE.
	 * @param patientApp Patient App instance to be bonded.
	 */
	public IMDBondingStatus(PatientApp patientApp, AppLifeEventResult aleResult) {
		super("imdBondingStatus", aleResult,
				Map.of("patient_app_ale_id", MobilityUtilities.getUUID(),
						"result_code", aleResult.getCode(),
						"patient_dim_version", dimVersion,
						"result_description", aleResult.getDescription(),
						"app_time", System.currentTimeMillis(),
						"abbreviated_model_num", patientApp.getAbbreviatedModelNum(),
						"azure_id", patientApp.getAzureId(),
						"user_record_id", patientApp.getIdentity().getUserRecordId()));
	}
		
	@Override
	public String getALETypeCode() {
		return IMD_BONDING_STATUS_CODE;
	}
}
