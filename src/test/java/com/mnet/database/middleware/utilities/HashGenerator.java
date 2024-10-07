package com.mnet.middleware.utilities;

import java.util.Map;
import java.util.UUID;

import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;

public class HashGenerator {

	private static final String HASH_API = FrameworkProperties.getProperty("HASH_KEY_API");
	private static final String HASH_API_HOST = FrameworkProperties.getProperty("HASH_KEY_HOST");
	private static final String HASH_AUTH_TOKEN = FrameworkProperties.getProperty("HASH_AUTH_TOKEN");

	private Map<String, Object> apiParameters = Map.of("Authorization", "Bearer " + HASH_AUTH_TOKEN, "User-Agent",
			"PostmanRuntime/7.32.2", "Accept", "*/*", "Postman-Token", UUID.randomUUID().toString(), "Host",
			HASH_API_HOST, "Accept-Encoding", "gzip, deflate, br", "Connection", "keep-alive");

	public enum HashAPIParam {
		HASH_DEVICE("/hash-device"), HASH_TRANSMITTER("/hash-transmitter"), HASH_MERLINID("/hash-merlinnetid"),
		HASH_USERID("/hash-userid");

		private String hashParam;

		private HashAPIParam(String hashParam) {
			this.hashParam = hashParam;
		}

		public String getHashParam() {
			return this.hashParam;
		}
	}

	private FrameworkLog log;

	public HashGenerator(FrameworkLog frameworkLog) {
		log = frameworkLog;
	}

	/**
	 * Get the Hash Key from the provided information. Hash key can be generated
	 * using Device Serial and Model, Transmitter Serial and Model, User ID and
	 * Merlin ID
	 * 
	 * @param serial - Device or Transmitter Serial
	 * @param model  - Device or Transmitter Model
	 * @param id     - User or merlin.net ID
	 * @param param  - For which scenario you need ot generate hash key
	 * @return
	 */
	public String getHashKey(HashAPIParam param, String serial, String model, String id) {
		APIRequest request;
		String key = null;

		switch (param) {
			case HASH_DEVICE:
				request = new APIRequest(HASH_API + param.getHashParam(),
						Map.of("device-model-num", model, "device-serial-num", serial), apiParameters);
				break;
			case HASH_TRANSMITTER:
				request = new APIRequest(HASH_API + param.getHashParam(),
						Map.of("transmitter-model-num", model, "transmitter-serial-num", serial), apiParameters);
				break;
			case HASH_MERLINID:
				request = new APIRequest(HASH_API + param.getHashParam(), Map.of("merlinNetId", id), apiParameters);
				break;
			case HASH_USERID:
				request = new APIRequest(HASH_API + param.getHashParam(), Map.of("userId", id), apiParameters);
				break;
			default:
				log.error("No hash key generation handled for Hash API parameter - " + param);
				return null;
		}
		
		log.info(request.asLoggableString());
		
		key = RestAPIManager.get(request).toString();
		log.info("Hash Key Generated:" + key);
		
		return key;
	}
}
