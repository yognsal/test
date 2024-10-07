package com.mnet.database.utilities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;

public abstract class DBUtilities {
	
	// TODO: Accept MITETest instance instead of report, database as arguments; make implementations static
	
	/**
	 * Function to execute DB query with filter if provided 
	 * @param dbQuery An SQL select statement with no qualifiers.
	 * @param filterColumn Filter column for "where" qualifier.
	 * @param filterValue Filter value for "where" qualifier.
	 */
	protected static List<Map<String, String>> getDBContents(TestReporter report, DatabaseConnector database, String dbQuery, String filterColumn, String filterValue) {
		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		Map<String, String> row = new HashMap<String, String>();
		
		if (filterColumn != null && filterValue != null) {
			dbQuery = dbQuery + " where " + filterColumn + "='" + filterValue + "'";
		}
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = database.executeQuery(dbQuery);

		List<List<String>> data = queryResult.getAllRows();
		List<String> columnNames = queryResult.getColumnNames();
		
		for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
			row = new HashMap<String, String>();
			for (int colIndex= 0; colIndex < columnNames.size(); colIndex++) {
				row.put(columnNames.get(colIndex), data.get(rowIndex).get(colIndex));
			}
			content.add(row);
		}
		
		return content;
	}
	
	/**
	 * Convenience function to support complex queries.
	 * @param dbQuery An SQL select statement with any qualifiers allowed.
	 */
	protected static List<Map<String, String>> getDBContents(TestReporter report, DatabaseConnector database, String dbQuery) {
		return getDBContents(report, database, dbQuery, null, null);
	}
	
	/**
	 * Converts an SQL timestamp to a Java Instant in the format 2011-12-03T10:15:30Z.
	 * Omits fractional seconds in the conversion.
	 * @implNote Example: 2023-05-02 11:59:31 to 2023-05-02T11:59:31Z*/
	public static Instant sqlTimestampToInstant(String sqlTimestamp) {
		// Convert to ISO format
		String sanitizedTimestamp = sqlTimestamp.replace(" ", "T") + "Z";
		
		return Instant.parse(sanitizedTimestamp);
	}
	
	/**
	 * Convenience function to interpret database flags (0 or 1) as boolean values.
	 * If the value does not represent a Boolean, returns null.*/
	public static Boolean interpretDatabaseFlag(String value) {
		if (value == null) {
			return false;
		} else if (value.equals("0")) {
			return false;
		} else if (value.equals("1")) {
			return true;
		}
		
		return null;
	}
}
