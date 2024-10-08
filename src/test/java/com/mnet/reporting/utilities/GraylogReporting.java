package com.mnet.reporting.utilities;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.testng.ITestResult;

import com.mnet.framework.api.APIRequest;
import com.mnet.framework.api.APIRequest.APICharset;
import com.mnet.framework.api.RestAPIManager;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.framework.utilities.Timeout;

import io.restassured.http.ContentType;

/**
 * Used to add Graylog reporting functionality to reports.
 * Override @AfterMethod cleanup() on any instance of MITETest which implements this interface.
 **/
public interface GraylogReporting {
	
	/**Represents microservices with Graylog integration on the AKS cluster.*/
	public enum Microservice {
		ALL_MICROSERVICES,
		
		TANTO_ROUTING_SERVICE,
		TANTO_PATIENT_PROFILE_SERVICE,
		TANTO_COMM_PROFILE_SERVICE,
		TRANSMISSION_ROUTING_SERVICE,
		ETL_APP_SERVICE,
		EVENT_EVALUATION_SERVICE,
		SNEAKERNET_SERVICE,
		TRANS_DECRYPT_SERVICE,
		
		ICM_APP_SERVICE,
		ICM_APP_LIFE_EVENT_SERVICE,
		NGQ_APP_CONFIG_SERVICE,
		NGQ_APP_LIFE_SERVICE,
		NGQ_APP_LIFE_EVENT_SERVICE,
		NGQ_TRANSMISSION_SERVICE,
		NGQ_APP_REGISTRATION_SERVICE,
		MOBILITY_SYNC_SERVICE,
		CMS_SERVICE,
		
		PATIENT_MGMT_INTG_SERVICE,
		PATIENT_PROFILE_SERVICE,
		CLINIC_PROFILE_SERVICE,
		
		NDS_APP_SERVICE,
		PATIENT_BIGLOOP_TIMER_SERVICE,
		PATIENT_NOTIFICATION_TIMER_SERVICE,
		PATIENT_NOTIFICATION_STATUS_SERVICE,
		ALERT_BIGLOOP_TIMER_SERVICE;
		
		/**Returns microservice name as a string searchable by the Graylog API.*/
		@Override
		public String toString() {
			return super.toString().replace("_", "-").toLowerCase();
		}
	}
	
	/***
	 * Adds Graylog logs to Extent Report and saves them to the test logging path.
	 * @param result ITestResult from current test. Obtain from overridden @AfterMethod cleanup() on instance of MITETest.
	 * @param report TestReporter from current test.
	 * @param api RestAPIManager from current test.
	 * @param microservices Name of Graylog microservice(s) to fetch logs from.
	 * @implNote Invoke from overridden @AfterMethod cleanup() in any instance of MITETest.
	 */
	default void fetchGraylogReports(ITestResult result, TestReporter report, Microservice... microservices) {
		getMicroserviceLogsInInterval(
				result.getStartMillis(), 
				result.getEndMillis(),
				(FrameworkLog) result.getTestContext().getAttribute("log"), 
				report, 
				microservices
			).writeAll();
	}
	
	/**
	 * Get the Graylog content corresponding to the applicable microservices.
	 * Current system time is used as reference for the end of the logging interval.
	 * @param startTime - Starting timestamp of retrieved logs in epoch milliseconds
	 */
	default GraylogArchive getMicroserviceLogsInInterval(long startTime, FrameworkLog logger, TestReporter report,
			Microservice... microservices) {
		return getMicroserviceLogsInInterval(startTime, System.currentTimeMillis(), logger, report, microservices);
	}
	
	// TODO: Update to use Graylog absolute timerange with ISO Instant
	
	/**
	 * Get the Graylog content corresponding to the applicable microservices.
	 * @param startTime - Starting timestamp of retrieved logs in epoch milliseconds
	 * @param endTime - Ending timestamp of retrieved logs in epoch milliseconds
	 */
	default GraylogArchive getMicroserviceLogsInInterval(long startTime, long endTime, FrameworkLog log, TestReporter report,
			Microservice... microservices) {
		final String VPN2_CREDENTIALS = FrameworkProperties.getProperty("VPN2_USERNAME") + ":"
				+ FrameworkProperties.getProperty("VPN2_PASSWORD");
		final String API_GRAYLOG_URI = FrameworkProperties.getProperty("API_GRAYLOG_URI");
		final String JSON_PATH = 
				FrameworkProperties.getSystemProperty("user.dir") + File.separator + FrameworkProperties.getProperty("JSON_PATH");
		final String JSON_GRAYLOG_LOOKUP = JSON_PATH + FrameworkProperties.getProperty("JSON_GRAYLOG_LOOKUP");
		final long API_GRAYLOG_TIMEOUT = Long.parseLong(FrameworkProperties.getProperty("API_GRAYLOG_TIMEOUT"));

		FileUtilities fileManager = new FileUtilities(log);
		Map<Microservice, String> microserviceLogs = new HashMap<>();
		
		double durationMillis = (double) (endTime - startTime + API_GRAYLOG_TIMEOUT);
		int durationMinutes = 1 + (int) Math.ceil(durationMillis / (60.0 * 1000.0));

		String lookupQuery = fileManager.getFileContent(JSON_GRAYLOG_LOOKUP);
		lookupQuery = lookupQuery.replace("<duration_minutes>", Integer.toString(durationMinutes));

		String encodedCredentials = "Basic " + Base64.getEncoder().encodeToString(VPN2_CREDENTIALS.getBytes());
		
		Map<String, Object> headers = Map.of(
				"Authorization", encodedCredentials,
				"X-Requested-By", "");

		log.info("Waiting for Graylog microservice logs to populate...");
		Timeout.waitForTimeout(log, API_GRAYLOG_TIMEOUT);
		
		for (Microservice service : microservices) {
			String serviceName = service.toString();
			log.info("Fetching microservice logs for past " + durationMinutes + " minute(s) from " + serviceName + ":");

			String serviceLookupQuery;

			if (service == Microservice.ALL_MICROSERVICES) {
				serviceLookupQuery = lookupQuery.replace("filebeat_kubernetes_container_name: <microservice>", "");
			} else {
				serviceLookupQuery = lookupQuery.replace("<microservice>", serviceName);
			}

			APIRequest request = new APIRequest(API_GRAYLOG_URI, headers, serviceLookupQuery, ContentType.JSON, APICharset.NONE);
			log.info(request.asLoggableString());
			
			String apiResponse = RestAPIManager.post(request).toString();

			microserviceLogs.put(service, apiResponse);
		}

		return new GraylogArchive(log, report, microserviceLogs);
	}
}
