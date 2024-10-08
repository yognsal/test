package com.mnet.pojo.mobility;

import java.util.Map;

import com.mnet.mobility.utilities.MobilityUtilities;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityDeviceType;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Represents app phone data used for fetching global parameters / provisioning.
 * @author Arya Biswas
 * @version Fall 2023
 */
@Data @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class PhoneData {
	
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private MobilityDeviceType deviceType;
	
	@NonNull
	private String appVer, appModel, os, osVer, make, model, locale, mtx, imeiNumber, packageId, serverRegion;
	
	private PhoneData(MobilityDeviceType mobilityDeviceType, MobilityOS mobilityOS) {
		deviceType = mobilityDeviceType;
		appVer = MobilityUtilities.getMobilityProperty("APP_VERSION");
		appModel = mobilityOS.getAppModel(mobilityDeviceType);
		os = mobilityOS.toString();
		osVer = MobilityUtilities.getMobilityProperty("OS_VERSION");
		locale = MobilityUtilities.getMobilityProperty("LOCALE");
		mtx = MobilityUtilities.getMobilityProperty("MTX");
		imeiNumber = MobilityUtilities.getMobilityProperty("IMEI_NUMBER");
		serverRegion = MobilityUtilities.getMobilityProperty("SERVER_REGION");
		
		switch (mobilityDeviceType) {
			case NGQ:
				make = MobilityUtilities.getMobilityProperty("MAKE");
				model = MobilityUtilities.getMobilityProperty("MODEL");
				packageId = MobilityUtilities.getMobilityProperty("PACKAGE_ID");
				break;
			case ICM: // Define new properties for ICM-specific values
			default:
				throw new RuntimeException("Unsupported device type for phone data: " + mobilityDeviceType);
		}
	}
	
	/**
	 * Creates an instance of PhoneData with default values from mobility properties file.
	 */
	public static PhoneData of(MobilityDeviceType deviceType, MobilityOS os) {
		return new PhoneData(deviceType, os);
	}
	
	/**
	 * Creates an instance of PhoneData with specified argument values.
	 */
	public static PhoneData of(MobilityDeviceType deviceType, MobilityOS os, 
			String appVer, String osVer, String make, String model, String locale,
			String mtx, String imeiNumber, String packageId, String serverRegion) {
		return new PhoneData(appVer, os.getAppModel(deviceType), os.toString(), osVer, make, model, locale, mtx, imeiNumber, packageId, serverRegion);
	}
	
	/**
	 * Interprets PhoneData as a JSON-parsable string.
	 */
	public String asJSON() {
		return MobilityUtilities.updateJSON("PhoneData", 
					Map.ofEntries(
							Map.entry("app_version", appVer),
							Map.entry("app_model", appModel),
							Map.entry("os", os),
							Map.entry("os_version", osVer),
							Map.entry("make", make),
							Map.entry("model", model),
							Map.entry("locale", locale),
							Map.entry("mtx", mtx),
							Map.entry("imei_number", imeiNumber),
							Map.entry("package_id", packageId),
							Map.entry("server_region", serverRegion)));
	}
	
	/**
	 * Retrieves device manufacturer in the format "Make|Model".
	 */
	public String getManufacturer() {
		return make;
	}
}
