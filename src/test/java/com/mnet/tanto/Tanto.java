package com.mnet.pojo.tanto;

import lombok.Data;

/**
 * POJO containing parameters that are required to perform profiling
 * @author kotwarx2
 *
 */

@Data
public class Tanto {
	
	private String driverType;
	private String profileType;
	private String profileVersion;
	private String transmitterSWVersion;
	private String transmissionType;
	private String timeOfFollowup;
	private String transmitterModelExtension;
	private String transmitterScriptSWVersion;
	private String transmitterScriptContentVersion;
	
	/**
	 * Enum to get different tanto profile versions
	 */
	public enum TantoProfileVersion{
		PROFILE_VERSION_7("7"), PROFILE_VERSION_8("8");
		
		private String version;

		private TantoProfileVersion(String version) {
			this.version = version;
		}

		public String getVersion() {
			return this.version;
		}
	}
	
	/**
	 * Enum to get different tanto profile types
	 */
	public enum TantoProfileType{
		PROFILE_TYPE_P("PProfile"), PROFILE_TYPE_I("IProfile"), PROFILE_TYPE_T("TProfile");
		
		private String type;

		private TantoProfileType(String type) {
			this.type = type;
		}

		public String getTantoProfileType() {
			return this.type;
		}
	}
	
	/**
	 * Enum to get different tanto profile types
	 * TODO: Need to add for other profile types
	 */
	public enum TantoProfileTypeCode{
		PProfile_Code("1057");
		
		private String code;

		private TantoProfileTypeCode(String code) {
			this.code = code;
		}

		public String getTantoProfileCode() {
			return this.code;
		}
	}
	
	/**
	 * Default constructor with default values required to perform profiling
	 */
	public Tanto() {
		this.driverType = "8.x";
		this.profileVersion = "7";
		this.transmissionType = "";
		this.transmitterModelExtension = "";
		this.transmitterSWVersion = "EX2000 v8.8 PR_8.85";
		this.timeOfFollowup = "";
	}
}
