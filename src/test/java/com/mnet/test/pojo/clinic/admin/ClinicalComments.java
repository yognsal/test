package com.mnet.test.pojo.clinic.admin;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * pojo class for Clinic Administration -> Clinic Comments
 * 
 * @author NAIKKX12
 *
 */
@Data
public class ClinicalComments {
	private boolean checkFreeFormText;
	private boolean checkPresetComments;
	private List<String> presetComments = new ArrayList<>();

	public ClinicalComments() {
		checkFreeFormText = true;
		checkPresetComments = true;
	}
}