
package com.mnet.database.utilities;

import java.util.HashMap;
import java.util.List;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;

public class GlobalDBUtilities {

	private TestReporter report;
	private DatabaseConnector globalDatabase;
	private String GLOBAL_DATABASE_URL = FrameworkProperties.getProperty("GLOBAL_DATABASE_URL");
	private String GLOBAL_DATABASE_USERNAME = FrameworkProperties.getProperty("GLOBAL_DATABASE_USERNAME");
	private String GLOBAL_DATABASE_PASSWORD = FrameworkProperties.getProperty("GLOBAL_DATABASE_PASSWORD");
	private String GLOBAL_ID = "global_id";

	public GlobalDBUtilities(TestReporter testReport, DatabaseConnector globalDatabaseConnector) {
		report = testReport;
		globalDatabase = globalDatabaseConnector; 
		globalDatabaseConnector.openConnection(GLOBAL_ID, GLOBAL_DATABASE_URL, GLOBAL_DATABASE_USERNAME, GLOBAL_DATABASE_PASSWORD);
	}

	/**
	 * Get Device details for the created patient in Global DB using the hashkey
	 * generated from postman.
	 */
	@Deprecated
	public HashMap<String, String> getDeviceDetails(String hashkey) {
		HashMap<String, String> details = new HashMap<>();
		String dbQuery = "select * from globalindex.global_device gd where key1 = '<key>';";
		dbQuery = dbQuery.replace("<key>", hashkey);
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = globalDatabase.executeQuery(GLOBAL_ID, dbQuery);
		
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(ReportLevel.ERROR, "No output from executed database query");
			return null;
		}
		
		List<String> values = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			details.put(columnNames.get(index), values.get(index));
		}
		return details;
	}
	
	/**
	 * Get Device details for the created patient in Global DB using the hashkey
	 * generated from postman.
	 */
	public HashMap<String, String> getDeviceDetails(String hashkey, boolean isKey1) {
		HashMap<String, String> details = new HashMap<>();
		String dbQuery;
		if(isKey1) {
			dbQuery = "select * from globalindex.global_device gd where key1 = '<key>';";
		}else {
			dbQuery = "select * from globalindex.global_device gd where key2 = '<key>';";
		}
		dbQuery = dbQuery.replace("<key>", hashkey);
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = globalDatabase.executeQuery(GLOBAL_ID, dbQuery);
		
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(ReportLevel.ERROR, "No output from executed database query");
			return null;
		}
		
		List<String> values = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			details.put(columnNames.get(index), values.get(index));
		}
		return details;
	}

	/**
	 * Get transmitter details from global DB. There are two keys from which you can
	 * get the required output. If output is required from key1 then set it true
	 * else false
	 */

	public HashMap<String, String> getTransmitterDetails(String hashkey, boolean isKey1) {
		HashMap<String, String> details = new HashMap<>();
		String dbQuery;
		if (isKey1) {
			dbQuery = "select * from globalindex.global_transmitter gt where key1 = '<key>';";
		} else {
			dbQuery = "select * from globalindex.global_transmitter gt where key2 = '<key>';";
		}
		dbQuery = dbQuery.replace("<key>", hashkey);
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = globalDatabase.executeQuery(GLOBAL_ID, dbQuery);
		
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(ReportLevel.INFO, "No output from executed database query");
			return null;
		}
		List<String> values = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			details.put(columnNames.get(index), values.get(index));
		}
		return details;
	}
	
	/**
	 * Get Clinic details or Clinic User Details from global database
	 */
	
	public HashMap<String, String> getClinicUserDetails(String hash) {
		HashMap<String, String> details = new HashMap<>();
		String dbQuery = "select * from globalindex.global_user gu where key1 = '<key>'";
		dbQuery = dbQuery.replace("<key>", hash);
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = globalDatabase.executeQuery(GLOBAL_ID, dbQuery);
		
		if (queryResult.getAllRows().isEmpty()) {
			report.logStep(ReportLevel.ERROR, "The Database query returned empty dataset");
			return null;
		}
		List<String> values = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			details.put(columnNames.get(index), values.get(index));
		}
		return details;
	}

}