package com.mnet.framework.api;

import java.util.List;

import org.apache.http.HttpStatus;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.Getter;

/**
 * Represents a JSON response returned by a REST API.
 * Provides utility functions to parse elements from the response.
 * @version Q1 2024 -> Q2 2024
 * @author Arya Biswas
 */
public class APIResponse {
	
	APIRequest request;
	Response response;
	JsonPath json;
	/**Details of catastropic failure during API requests (connection reset, etc)*/
	@Getter
	Exception failureException;

	protected APIResponse(APIRequest apiRequest, Response apiResponse) {
		request = apiRequest;
		response = apiResponse;
		json = response.jsonPath();
	}
	
	protected APIResponse(APIRequest apiRequest, Exception exception) {
		request = apiRequest;
		failureException = exception;
		json = JsonPath.from("");
	}
	
	@Override
	public String toString() {
		return (failureException != null) ? failureException.toString() : response.asPrettyString();
	}
	
	/**
	 * Convenience function to retrieve the APIRequest that produced this response.
	 */
	public APIRequest getRequest() {
		return request;
	}
	
	/**
	 * Returns the HTTP Status Code corresponding to the response.
	 * @see org.apache.http.httpStatus
	 */
	public int getStatusCode() {
		return (failureException != null) ? HttpStatus.SC_NOT_FOUND : response.getStatusCode();
	}
	
	/**
	 * Queries API response using JsonPath syntax.
	 * Parsed attribute is expected to be a String.
	 * @see <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath">JsonPath Syntax Guide</a>
	 */
	public String getStringFromJsonPath(String jsonPath) {
		return json.getString(jsonPath);
	}
	
	/**
	 * Queries API response using JsonPath syntax.
	 * Parsed attribute is expected to be a Boolean.
	 * @see <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath">JsonPath Syntax Guide</a>
	 */
	public boolean getBooleanFromJsonPath(String jsonPath) {
		return json.getBoolean(jsonPath);
	}
	
	/**
	 * Queries API response using JsonPath syntax.
	 * Parsed attribute is expected to be a Number (byte, double, int, short, or long).
	 * @see <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath">JsonPath Syntax Guide</a>
	 */
	public Number getNumberFromJsonPath(String jsonPath) {
		return (Number) json.get(jsonPath);
	}
	
	/**
	 * Queries API response using JsonPath syntax.
	 * Parsed attribute is expected to be a List.
	 * @see <a href="https://github.com/rest-assured/rest-assured/wiki/Usage#json-using-jsonpath">JsonPath Syntax Guide</a>
	 */
	public List<Object> getListFromJsonPath(String jsonPath) {
		return json.getList(jsonPath);
	}
	
}
