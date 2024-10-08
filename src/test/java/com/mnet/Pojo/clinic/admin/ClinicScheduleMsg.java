package com.mnet.pojo.clinic.admin;

import lombok.Data;

/**
 * pojo class for Clinic Administration -> Scheduling & Messaging
 * 
 * @author NAIKKX12
 *
 */
@Data
public class ClinicScheduleMsg {
	private SchedulingMethod schedulingMethod;
	private boolean collectDiagnostic;
	private boolean dailyDirectAlert;

	public enum SchedulingMethod {
		SMART("SmartSchedule™ calendar"), MANUAL("Manual entry calendar"), NONE("None");

		private String schedulingMethod;

		private SchedulingMethod(String schedulingMethod) {
			this.schedulingMethod = schedulingMethod;
		}

		public String getSchedulingMethod() {
			return this.schedulingMethod;
		}
	}
}
