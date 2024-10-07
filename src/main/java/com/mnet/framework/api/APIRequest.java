package com.mnet.framework.api;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represents a request made to a REST API.
 * Provides functionality to construct an API request 
 * and retrieve the request as a loggable string.
 * @author Arya Biswas
 * @version Fall 2023
 */
@Getter
public class APIRequest {
	
	@Getter(AccessLevel.PROTECTED)
	private RequestSpecification specification;
	
	private String uri;
	private String contentType;
	private Map<String, Object> queryParameters;
	private Map<String, Object> headers;
	private String rawBody;
	private Map<String, Object> formBody;
	
	@Getter(AccessLevel.NONE)
	private String loggableString;
	
	/*
	 * Represents the charset encoding of the API request's content type.
	 * Default (unspecified) charset is ISO-8859-1 for content encoding and UTF-8 for query parameter encoding.
	 */
	public enum APICharset {
		NONE(""),
		ISO_8859_1("ISO-8859-1"),
		UTF_8("UTF-8");
		
		private String rawText;
		
		private APICharset(String text) {
			rawText = text;
		}
		
		@Override
		public String toString() {
			return rawText;
		}
	}
	
	/**
	 * Represents an API request with no body.
	 */
	public APIRequest(String uri, Map<String, Object> queryParameters, Map<String, Object> headers) {
		this.uri = uri;
		this.queryParameters = queryParameters;
		this.headers = headers;
		
		specification = RestAssured.given().relaxedHTTPSValidation()
				.baseUri(uri).queryParams(queryParameters).headers(headers);
	}
	
	/**
	 * Represents an API request with no query parameters as well body.
	 */
	public APIRequest(String uri, Map<String, Object> headers) {
		this.uri = uri;
		this.headers = headers;
		
		specification = RestAssured.given().relaxedHTTPSValidation()
				.baseUri(uri).headers(headers);
	}
	
	/**
	 * Represents an API request with no query parameters and a specified request content type.
	 * @param contentCharset Used to define the 'charset' attribute of the Content-Type header.
	 * If not applicable, use APICharset.NONE.
	 */
	public APIRequest(String uri, Map<String, Object> headers, ContentType contentType, APICharset contentCharset) {
		this.uri = uri;
		this.headers = headers;
		this.contentType = getContentTypeHeader(contentType, contentCharset);
		
		specification = RestAssured.given().relaxedHTTPSValidation()
				.baseUri(uri).headers(headers).contentType(contentType);
	}
	
	/**
	 * Represents an API request with no query parameters, a raw body, and a specified request content type.
	 * @param contentCharset Used to define the 'charset' attribute of the Content-Type header.
	 * If not applicable, use APICharset.NONE.
	 */
	public APIRequest(String uri, Map<String, Object> headers, String body, ContentType contentType, APICharset contentCharset) {
		this(uri, headers, contentType, contentCharset);
			
		this.rawBody = body;
		specification = specification.body(body);
	}
	
	/**
	 * Represents an API request with no body and a specified request content type.
	 * Allows specification of complex content types e.x. application/xml;type=entry;charset=utf-8
	 */
	public APIRequest(String uri, Map<String, Object> queryParameters, Map<String, Object> headers,
			String contentType) {
		this(uri, queryParameters, headers);
		
		this.contentType = contentType;
		specification = specification.contentType(contentType);
	}
	
	/**
	 * Represents an API request with a body and a specified request content type.
	 * Allows specification of complex content types e.x. application/xml;type=entry;charset=utf-8
	 */
	public APIRequest(String uri, Map<String, Object> queryParameters, Map<String, Object> headers,
			String body, String contentType) {
		this(uri, queryParameters, headers, contentType);
		
		this.rawBody = body;
		specification = specification.body(body);
	}
	
	/**
	 * Represents an API request with a raw body and a specified request content type.
	 * @param contentCharset Used to define the 'charset' attribute of the Content-Type header.
	 * If not applicable, use APICharset.NONE.
	 */
	public APIRequest(String uri, Map<String, Object> queryParameters, Map<String, Object> headers,
			String body, ContentType contentType, APICharset contentCharset) {
		this(uri, queryParameters, headers, body, getContentTypeHeader(contentType, contentCharset));
	}
	
	/**
	 * Represents an API request with a body as multipart form data / URL encoded form data.
	 * @param contentCharset Used to define the 'charset' attribute of the Content-Type header.
	 * If not applicable, use APICharset.NONE.
	 */
	public APIRequest(String uri, Map<String, Object> queryParameters, Map<String, Object> headers,
			Map<String, Object> body, ContentType contentType, APICharset contentCharset) {
		this(uri, queryParameters, headers, getContentTypeHeader(contentType, contentCharset));
		
		this.formBody = body;
		
		switch (contentType) {
			case URLENC:
				specification = specification.formParams(body);
				break;
			case MULTIPART:
				addMultipart(body);
				break;
			default:
				throw new RuntimeException("Content type not recognized as form data: " + contentType.toString());
		}
	}
	
	/**
	 * Represents an API request with a File passed as multipart form data.
	 * @param fileControlName Control name (key) of the File body parameter (value).
	 */
	public APIRequest(String uri, Map<String, Object> queryParameters, Map<String, Object> headers, String fileControlName, File body) {
		this(uri, queryParameters, headers);
		
		this.formBody = Map.of(fileControlName, body);
		specification = specification.multiPart(fileControlName, body);
	}
	
	/**
	 * Represents an API request with a File passed as multipart form data & no query parameters
	 * @param fileControlName Control name (key) of the File body parameter (value).
	 */
	public APIRequest(String uri, Map<String, Object> headers, String fileControlName, File body) {
		this(uri, headers);
		
		this.formBody = Map.of(fileControlName, body);
		specification = specification.multiPart(fileControlName, body);
	}
	
	/*
	 * Instance functions
	 */
	
	/**
	 * Returns the content of the API request as a log-friendly string.
	 */
	public String asLoggableString() {
		if (loggableString == null) {
			loggableString = buildLoggableString();
		}
		
		return loggableString;
	}
	
	/*
	 * Local helper functions
	 */
	
	private static String getContentTypeHeader(ContentType contentType, APICharset contentCharset) {
		return (contentCharset == APICharset.NONE) ? 
				contentType.toString() : 
				contentType.withCharset(contentCharset.toString());
	}
	
	private void addMultipart(Map<String, Object> body) {
		Set<Entry<String, Object>> multipartBody = body.entrySet();
		
		for (Entry<String, Object> element : multipartBody) {
			specification = specification.multiPart(element.getKey(), element.getValue());
		}
	}
	
	private String buildLoggableString() {
		String logQueryParams = "", logHeaders = "", logBody = rawBody;
		
		logQueryParams = getMapAsLoggableString(queryParameters);
		logHeaders = getMapAsLoggableString(headers);
		
		if ((rawBody == null)) {
			logBody = getMapAsLoggableString(formBody);
		}
		
		return "API Request: \n"
				+ "URI: " + uri + "\n"
				+ "Query Parameters: \n " + logQueryParams + "\n"
				+ "Headers: \n" + logHeaders + "\n"
				+ "Content-Type: " + contentType + "\n"
				+ "Body: \n" + logBody;
	}
	
	private String getMapAsLoggableString(Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		
		Set<String> keys = map.keySet();
		String loggableString = "";
		
		for (String key : keys) {
			loggableString += "\t" + key + " : " + map.get(key).toString() + "\n";
		}
		
		return StringUtils.removeEnd(loggableString, "\n");
	}
}
