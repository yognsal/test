package com.mnet.mobility.utilities.validation;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.json.simple.JSONObject;

import com.mnet.database.utilities.PatientAppDBUtilities;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.mnet.mobility.utilities.PatientApp;
import com.mnet.mobility.utilities.profile.AppProfile;
import com.mnet.mobility.utilities.profile.DevMedProfile;
import com.mnet.mobility.utilities.profile.DualPatientNotifierProfile;
import com.mnet.mobility.utilities.profile.ParameterSynchProfile;
import com.mnet.mobility.utilities.profile.PatientInitiatedFollowupProfile;
import com.mnet.mobility.utilities.profile.ProfileAttribute;
import com.mnet.mobility.utilities.profile.DirectAlertsProgrammingProfile;
import com.mnet.mobility.utilities.profile.ScheduledDeviceCheckProfile;
import com.mnet.mobility.utilities.profile.ScheduledFollowupProfile;
import com.mnet.mobility.utilities.profile.SystemProfile;

import io.restassured.path.json.JsonPath;
import lombok.Getter;

/**
 * Provides functionality to perform operations (get/ verify) with profile data
 * 
 * @author NAIKKX12
 */
public interface ProfileValidation {
	public static String contentVersion = "contentVersion";

	public enum EventType {
		NPATENROLL(100, 200, 210, 230, 260, 270, 280, 840), NPATBOND(100, 200, 210, 230, 260, 270, 280, 840),
		NPATSCHED(200), NPATDACHK(210), NPATINIT(260), NCLRBOND(100), NCLNPROF(100), NCLNSCHED(200), NCLNDACHK(210),
		NCLNINIT(260), NPATDIRALRT(230, 840), NCLNDIRALRT(230, 840), NDASUCC(230), NDASETBVVI(230),
		NDACLRBVVI(230, 840), NDARETRY(230, 840), NPATEOS(200, 210, 230, 260, 270, 280);

		private List<AppProfileType> profileTypes = new ArrayList<>();
		@Getter
		private List<Integer> profileCodes;

		private EventType(Integer... codes) {
			this.profileCodes = Arrays.asList(codes);
		}

		public List<AppProfileType> getProfileTypes() {
			for (AppProfileType profileType : AppProfileType.values()) {
				if (profileCodes.contains(profileType.getProfileCode())) {
					profileTypes.add(profileType);
				}
			}
			return profileTypes;
		}
	}

	/** Enum for NGQ/ICM profile classes */
	public enum AppProfileType {
		/** 100 Workflow Profile */
		SYSTEM(SystemProfile.class, 100),
		/** 200 Workflow Profile */
		SCHEDULED_FOLLOWUP(ScheduledFollowupProfile.class, 200),
		/** 210 Workflow Profile */
		SCHEDULED_DEVICE_CHECK(ScheduledDeviceCheckProfile.class, 210),
		/** 230 Workflow Profile */
		DIRECT_ALERT_PROGRAMMING(DirectAlertsProgrammingProfile.class, 230),
		/** 260 Workflow Profile */
		PATIENT_INITIATED_FOLLOWUP(PatientInitiatedFollowupProfile.class, 260),
		/** 270 Workflow Profile */
		DEVMED(DevMedProfile.class, 270),
		/** 280 Workflow Profile */
		DUAL_PATIENT_NOTIFIER(DualPatientNotifierProfile.class, 280),
		/** 840 Instruction Profile */
		PARAMETER_SYNC(ParameterSynchProfile.class, 840);

		@Getter
		private Class<? extends AppProfile> profileClass;
		@Getter
		private int profileCode;

		private AppProfileType(Class<? extends AppProfile> profile, int code) {
			profileClass = profile;
			profileCode = code;
		}
	}

	/** Validate profile based on requested profile type */
	public default boolean validateProfile(PatientApp patientApp, AppProfileType appProfileType) {
		return getProfile(patientApp, appProfileType).validate();
	}

	/** Validate profiles based on event type if mentioned */
	public default boolean validateProfiles(PatientApp patientApp, EventType event) {
		TestReporter report = patientApp.getCurrentTest().getReport();

		boolean success = true, individualProfileSuccess = true;

		List<AppProfileType> profileTypes = event.getProfileTypes();

		for (AppProfileType profileType : profileTypes) {
			report.logStep(TestStep.builder().reportLevel(ReportLevel.INFO)
					.message("Validating " + profileType.name() + " profile attributes...").build());

			individualProfileSuccess = validateProfile(patientApp, profileType);
			success = individualProfileSuccess ? success : false;

			report.logStep(TestStep.builder().reportLevel(ReportLevel.INFO)
					.message(profileType.name() + " profile attribute validation: " + individualProfileSuccess)
					.build());
		}

		return success;
	}

	/**
	 * Validate content_clob from patient profile container table against IOT
	 * Message clob
	 */
	public default boolean validateIOTMessage(PatientApp patientApp, EventType profileTrigger) {
		TestReporter report = patientApp.getCurrentTest().getReport();
		JsonPath iotMessageJson;

		Map<String, Object> iotMessageContent = new LinkedHashMap<>();
		boolean isValid = true, success = true;

		List<AppProfileType> triggeredProfileTypes = profileTrigger.getProfileTypes();
		Map<AppProfileType, AppProfile> containerProfiles = getProfiles(patientApp);

		for (AppProfileType profileType : containerProfiles.keySet()) {
			iotMessageJson = new JsonPath(
					getIOTMessageClob(patientApp, containerProfiles.get(profileType).getProfileCode()));

			List<ProfileAttribute> attributes = containerProfiles.get(profileType).getAllAttributes();

			iotMessageContent = new LinkedHashMap<>();
			for (ProfileAttribute attribute : attributes) {
				iotMessageContent.put(attribute.getJsonPath(), iotMessageJson.get(attribute.getJsonPath()));
			}

			AppProfile profile = containerProfiles.get(profileType);

			JsonPath containerClob = profile.getContentClob();

			report.logStep(TestStep.builder()
					.message("Profile type: " + profileType + "| Content version value | profile container table: "
							+ profile.getContentVersion() + " | IOT message table: "
							+ iotMessageJson.getInt(contentVersion))
					.build());
			success = containerProfiles.get(profileType).getContentVersion() == iotMessageJson.getInt(contentVersion);
			isValid = success ? isValid : false;

			if (triggeredProfileTypes.contains(profileType)) {
				report.logStep(
						TestStep.builder()
						.message("Profile type: " + profileType + " | IOT message table content: "
								+ iotMessageJson.get() + " | Container table content: " + containerClob.get())
						.build());
				for (ProfileAttribute attribute : attributes) {
					if (!Objects.equals(containerClob.get(attribute.getJsonPath()),
							iotMessageJson.get(attribute.getJsonPath()))) {
						success = false;
					}
				}
				report.logStep(
						TestStep.builder()
						.message("Profile type: " + profileType
								+ " | IOT message content matches with profile container table: " + success)
						.build());
				isValid = success ? isValid : false;
			}
		}
		return isValid;
	}

	/** Get all profiles populated based on device serial */
	default Map<AppProfileType, AppProfile> getProfiles(PatientApp patientApp) {
		Map<AppProfileType, AppProfile> profiles = new HashMap<>();
		AppProfile profile;

		for (AppProfileType profileType : AppProfileType.values()) {
			profile = getProfile(patientApp, profileType);
			if (profile.getContentVersion() != 0) {
				profiles.put(profileType, profile);
			}
		}

		return profiles;
	}

	/**
	 * Validate content version is increased for triggered profile; no change in
	 * content version of other profiles Validate entire profile object if event
	 * triggered
	 */
	default boolean validateProfileTrigger(PatientApp patientApp, Map<AppProfileType, AppProfile> preTriggerProfiles,
			EventType profileTrigger) {

		boolean isValid = true, success = true;

		Map<AppProfileType, AppProfile> allProfiles = getProfiles(patientApp);
		List<AppProfileType> triggeredProfileTypes = profileTrigger.getProfileTypes();

		for (AppProfileType profileType : allProfiles.keySet()) {
			if (triggeredProfileTypes.contains(profileType)) {
				patientApp.getCurrentTest().getReport()
				.logStep(TestStep.builder()
						.message("Triggered profile type: " + profileType
								+ "; Content version of triggered profile is "
								+ allProfiles.get(profileType).getContentVersion())
						.build());
				success = preTriggerProfiles.get(profileType).getContentVersion() < allProfiles.get(profileType)
						.getContentVersion();
				patientApp.getCurrentTest().getReport().logStep(
						TestStep.builder().message("Content version of triggered profile matches: " + success).build());
				isValid = success ? isValid : false;
				success = allProfiles.get(profileType).validate();
				patientApp.getCurrentTest().getReport().logStep(TestStep.builder()
						.message("Triggered profile changes in profile container are successful: " + success).build());
				isValid = success ? isValid : false;
			} else {
				patientApp.getCurrentTest().getReport().logStep(TestStep.builder()
						.message("Profile type: " + profileType + "; content version | before event trigger: "
								+ preTriggerProfiles.get(profileType).getContentVersion() + "| after event trigger: "
								+ allProfiles.get(profileType).getContentVersion())
						.build());
				isValid = preTriggerProfiles.get(profileType).getContentVersion() == allProfiles.get(profileType)
						.getContentVersion() ? isValid : false;
			}
		}
		return isValid;
	}

	/** Local function to get profile object based on type */
	static AppProfile getProfile(PatientApp patientApp, AppProfileType appProfileType) {
		try {
			return (AppProfile) appProfileType.getProfileClass().getConstructors()[0].newInstance(patientApp);
		} catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
			String err = "Failed to construct new instance of subprofile: "
					+ appProfileType.getProfileClass().toString() + " \n Cause: " + e.getMessage();
			patientApp.getCurrentTest().getLog().error(err);
			throw new RuntimeException(err);
		}
	}

	/**
	 * Function to read content clob from iot_message table for provided profile
	 * code
	 */
	default String getIOTMessageClob(PatientApp patientApp, int profileCode) {

		List<Map<String, String>> dbContent = PatientAppDBUtilities.getIOTMessage(patientApp.getCurrentTest(),
				patientApp.getAzureId(), null);
		
		if(dbContent.isEmpty()) {
			return null;
		}
 
		List<Map<Object, Object>> data = new ArrayList<>();
 
		for (int index = 0; index < dbContent.size(); index++) {
			data = new JsonPath(dbContent.get(index).get("iot_message_clob")).getList("profiles");
			for (Map<Object, Object> str : data) {
				if ((int) str.get("id") == profileCode) {
					return str.get("content").toString();
				}
			}
		}
		return null;
	}

	/** local function to convert jsonPath to Json Object **/
	static JSONObject jsonPathToObject(AppProfile appProfile, JsonPath jsonPath) {
		List<ProfileAttribute> attributes = appProfile.getAllAttributes();
		Map<String, Object> attributeMap = new LinkedHashMap<>();

		for (ProfileAttribute attribute : attributes) {
			String key = attribute.getJsonPath();
			attributeMap.put(key, jsonPath.getString(key));
		}

		return new JSONObject(attributeMap);
	}
	
	/**Returns data for specified profile from cloud server (IOT Message table) in form of JSON**/
	default JsonPath getProfileFromCloudServer(PatientApp patientApp, AppProfileType profileType) {
		JsonPath iotMessageJson = null;
		String getIOTMessageValue = getIOTMessageClob(patientApp, profileType.getProfileCode());
		if(getIOTMessageValue == null) {
			return null;
		}
		iotMessageJson = new JsonPath(getIOTMessageValue);
		return iotMessageJson;
	}
}
