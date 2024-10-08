package com.mnet.pojo.patient;

import lombok.Data;

// POJO defined for patient enrollment/ profile -> baseline clinical data parameters
@Data
public class BaselineClinicalData {
	
	// Options under Cardiovascular section
	// TODO: can define corresponding enum in future if needed
	private boolean anginaCheck;
}
