package com.mnet.framework.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;

/**
 * Handles database connections and query execution.
 * @version Spring 2023
 * @author Arya Biswas
 **/
public class DatabaseConnector {

	/**
	 * Database URL defined in JDBC connection format.
	 * @implNote Postgres: jdbc:postgresql://host:port/database
	 * @implNote Oracle: jdbc:oracle:thin:@//host:port/database
	 **/
	private static final String DATABASE_URL = FrameworkProperties.getProperty("DATABASE_URL");
	private static final String DATABASE_USERNAME = FrameworkProperties.getProperty("DATABASE_USERNAME");
	private static final String DATABASE_PASSWORD = FrameworkProperties.getProperty("DATABASE_PASSWORD");
	
	/**Unique string to identify default connection.*/
	private static final String DEFAULT_ID = "default";	
	private FrameworkLog log;
	private HashMap<String, Connection> connections = new HashMap<String, Connection>();
	private List<QueryResult> cachedQueries = new ArrayList<QueryResult>();
	
	/**
	 * Initializes default database connection defined in environment properties file.
	 * For additional database connections, please invoke openConnection().
	 */
	public DatabaseConnector(FrameworkLog frameworkLog) {
		log = frameworkLog;
		openConnection(DEFAULT_ID, DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD);
		
	}
	
	/**
	 * Opens new database connection with specified connection details.
	 * @param id Unique string to identify connection. Cannot be "default".
	 * @param url JDBC connection string.
	 * @param username
	 * @param password
	 * @implNote Postgres JDBC connection string: jdbc:postgresql://host:port/database
	 * @implNote Oracle JDBC connection string: jdbc:oracle:thin:@//host:port/database
	 */
	public void openConnection(String id, String url, String username, String password) {	
		try {
			connections.put(id, DriverManager.getConnection(url, username, password));
		} catch (SQLException sqe) {
			String err = "Database access error for: " + 
					url + " | " + username + " / " + password;
			log.error(err);
			log.printStackTrace(sqe);
			throw new RuntimeException(err);
		}
	}
	
	public void closeConnections() {
		for (QueryResult result : cachedQueries) {
			result.closeQuery();
		}
		
		for (String key : connections.keySet()) {
			try {
				connections.get(key).close();
			} catch (SQLException sqe) {
				log.warn("Failed to close database connection: " + DATABASE_URL);
				log.printStackTrace(sqe);
			}
		}
	}
	
	/**
	 * Executes SQL SELECT query on default connection defined in environment properties.
	 * @param query Valid SQL statement to query database.
	 */
	public QueryResult executeQuery(String query) {
		return executeQuery(DEFAULT_ID, query);
	}

	/***
	 * Executes SQL SELECT query on database connection explicitly initialized with openConnection().
	 * If using the default connection defined in environment properties, use executeQuery(String) instead.
	 * @param id Unique identifier for database used in openConnection(String, String, String, String).
	 * @param query Valid SQL statement to query database.
	 */
	public QueryResult executeQuery(String id, String query) {
		Statement statement;
		
		try {
			statement = connections.get(id).createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			QueryResult result = new QueryResult(statement.executeQuery(query), log);
			cachedQueries.add(result);
			return result;
		} catch (SQLException sqe) {
			String err = "Failed to execute query: " + query;
			log.error(err);
			log.printStackTrace(sqe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * Executes SQL UPDATE/INSERT statement on default connection defined in environment properties.
	 * @param query Valid SQL statement to update database.
	 */
	public void executeUpdate(String updateStatement) {
		executeUpdate(DEFAULT_ID, updateStatement);
	}
	
	/***
	 * Executes SQL UPDATE/INSERT statement on database connection explicitly initialized with openConnection().
	 * If using the default connection defined in environment properties, use executeUpdate(String) instead.
	 * @param id Unique identifier for database used in openConnection(String, String, String, String).
	 * @param updateStatement Valid SQL statement to update database.
	 */
	public void executeUpdate(String id, String updateStatement) {
		Statement statement;
		
		try {
			statement = connections.get(id).createStatement();
			statement.executeUpdate(updateStatement);
		} catch (SQLException sqe) {
			String err = "Failed to execute statement: " + updateStatement;
			log.error(err);
			log.printStackTrace(sqe);
			throw new RuntimeException(err);
		}
	}
	
}
