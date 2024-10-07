package com.mnet.database.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;

/**
 * Utilities for all database calls for user
 * @author NAIKKX12
 *
 */
public class UserDBUtilities {
	private TestReporter report;
	private DatabaseConnector database;
	
	public UserDBUtilities(TestReporter testReporter, DatabaseConnector databaseConnector) {
		report = testReporter;
		database = databaseConnector;
	}
	
	/**
	 * verify user exists or deleted
	 * @param userId (logon_user_name)
	 * @return
	 */
	public boolean userExists(String userId) {
		List<String> user = getUser(userId);
		// NOTE: Logon_user_name column is 2nd and deleteFlag is 7th column
		int logonUserNameColumn = 1, deleteFlag = 6;

		if (user.size() > 0) {
			if (user.get(logonUserNameColumn).equals(userId) && user.get(deleteFlag).equals("0")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Verify that user is created with the details which are passed as parameters
	 */

	public boolean verifyUserCreation(String userid, String firstName, String lastName) {
		List<String> userInfo = getUser(userid);
		report.logStep(ReportLevel.INFO, userInfo.toString());
		//NOTE: First name column is 4th and Last name column is 6th
		int firstNameColumn = 3, lastNameColumn = 5;
		if (!userInfo.get(firstNameColumn).equals(firstName) || !userInfo.get(lastNameColumn).equals(lastName)) {
			return false;
		}

		return true;
	}
	
	/**
	 * Get all users associated with any clinic
	 * @param columnName - column name to apply filter e.g. customer_id
	 */
	public List<Map<String, String>> getUsersFromClinic(String columnName, String filterValue) {
		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		Map<String, String> row = new HashMap<String, String>();
		String dbQuery = "select * from users.customer_account where " + columnName + "= '" + filterValue + "'";
		
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
	 * Get users based on the filter requested
	 */
	public List<Map<String, String>> getUsers(String columnName, String filterValue) {
		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		Map<String, String> row = new HashMap<String, String>();
		String dbQuery = "select * from users.user_record where " + columnName + "= '" + filterValue + "'";
		
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
	 * Read user details from database based on logon_user_name (userId) provided
	 */
	private List<String> getUser(String logonUserName) {

		String dbQuery = "select * from users.user_record ur where logon_user_name = '<logon_user_name>'";
		dbQuery = dbQuery.replace("<logon_user_name>", logonUserName);
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		
		return database.executeQuery(dbQuery).getFirstRow();
	}
}
