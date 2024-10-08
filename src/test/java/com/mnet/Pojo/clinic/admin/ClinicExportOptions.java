package com.mnet.pojo.clinic.admin;

import lombok.Data;

/**
 * Pojo class for clinic export options (Clinic Administration -> Export
 * Options)
 * 
 * @author NAIKKX12
 *
 */
@Data
public class ClinicExportOptions {
	private boolean setupPCData;
	private boolean exportSession;
}
