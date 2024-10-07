package com.mnet.framework.api;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;

/**
 * Utility class to handle Rest API requests.
 * @version Spring 2023 -> Q2 2024
 * @author Arya Biswas
 */
public class RestAPIManager {
	
	/**
	 * Simulates a GET request on the default host and port.
	 */
	public static APIResponse get(APIRequest request) {
		APIResponse response = null;
		
		try {
			response = new APIResponse(request, request.getSpecification().get());
		} catch (Exception e) {
			return new APIResponse(request, e);
		}
		
		return response;
	}
	
	/**
	 * Simulates a GET request on the specified proxy host and port.
	 */
	public static APIResponse get(APIRequest request, String proxyHost, int proxyPort) {
		RestAssured.proxy(proxyHost, proxyPort);
		APIResponse response = get(request);
		
		RestAssured.reset();
		
		return response;
	}
	
	/**
	 * Simulates a POST request on the default host and port.
	 */
	public static APIResponse post(APIRequest request) {
		APIResponse response = null;
		
		try {
			response = new APIResponse(request, request.getSpecification().post());
		} catch (Exception e) {
			return new APIResponse(request, e);
		}
		
		return response;
	}
	
	/**
	 * Enables verbose console logging for all API requests and responses.
	 * @apiNote For debugging purposes only.
	 */
	public static void logAllToConsole() {
		RestAssured.filters(new RequestLoggingFilter(), new ResponseLoggingFilter());
	}
}