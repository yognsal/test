package com.mnet.framework.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.mnet.framework.reporting.FrameworkLog;

/**
 * Provides contextual functions to interpret a database response.
 * If the dataset remains unchanged, it is recommended that tests cache this result for reuse.
 */
public class QueryResult {
	
	private ResultSet result;
	private ResultSetMetaData metadata;
	private int columns;
	
	private FrameworkLog log;
	
	protected QueryResult(ResultSet resultSet, FrameworkLog frameworkLog) {
		result = resultSet;
		log = frameworkLog;
		
		try {
			metadata = resultSet.getMetaData();
			columns = metadata.getColumnCount();
		} catch (SQLException sqle) {
			String err = "Database access error for result set: " + result.toString();
			log.error(err);
			log.printStackTrace(sqle);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * @return Value of the first cell of the query result.
	 */
	public String getFirstCellValue() {
		try {
			boolean validResult = result.absolute(1);
			
			return (validResult) ? result.getString(1) : null;
		} catch (SQLException sqe) {
			String err = "Database access error for query result: " + result.toString();
			log.error(err);
			log.printStackTrace(sqe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * @return List of elements in first row of query result.
	 */
	public List<String> getFirstRow() {
		try {
			result.absolute(1);
			return getCurrentRow();
		} catch (SQLException sqe) {
			String err = "Database access error for query result: " + result.toString();
			log.error(err);
			log.printStackTrace(sqe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * @param columnName Name of column in SQL database.
	 * @return List of column values from the last database query made.
	 */
	public List<String> getColumn(String columnName) {
		List<String> columnValues = new ArrayList<String>();
		
		try {
			result.beforeFirst();
			
			while (result.next()) {
				columnValues.add(result.getString(columnName.toUpperCase()));
			}
		} catch (SQLException sqe) {
			String err = "Failed to find column in query result with name: " + columnName;
			log.error(err);
			log.printStackTrace(sqe);
			throw new RuntimeException(err);
		}
		
		return columnValues;
	}
	
	/**
	 * @return List of all columns names from the last database query made.
	 */
	public List<String> getColumnNames() {
		List<String> columnNames = new ArrayList<String>();
		
		for (int i = 1; i <= columns; i++) {
			try { 
				columnNames.add(metadata.getColumnName(i));
			} catch (SQLException sqe) {
				String err = "Failed to fetch column name at index: " + i;
				log.error(err);
				log.printStackTrace(sqe);
				throw new RuntimeException(err);
			}
		}
		
		return columnNames;
	}
	
	/**
	 * @return Nested list of all rows from the last database query made.
	 */
	public List<List<String>> getAllRows() {
		List<List<String>> allRows = new ArrayList<List<String>>();
		
		try {
			result.beforeFirst();
			
			while (result.next()) {				
				allRows.add(getCurrentRow());
			}
		} catch (SQLException sqe) {
			String err = "Database access error for query object: " + result.toString();
			log.error(err);
			log.printStackTrace(sqe);
			throw new RuntimeException(err);
		}
		
		return allRows;
	}
	
	/**Releases resources associated with ResultSet.*/
	protected void closeQuery() {
		try {
			result.close();
		} catch (SQLException sqe) {
			log.warn("Failed to close query object: " + result.toString());
			log.printStackTrace(sqe);
		}
	}
	
	/*
	 * Helper functions
	 */
	
	private List<String> getCurrentRow() {
		List<String> queryRow = new ArrayList<String>();
		for (int i = 1; i <= columns; i++) {
			try {
				queryRow.add(result.getString(i));
			} catch (SQLException sqe) {
				String err = "Failed to access current row in result set: " + result.toString();
				log.error(err);
				log.printStackTrace(sqe);
				throw new RuntimeException(err);
			}
		}
		
		return queryRow;
	}
}
