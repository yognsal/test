package com.mnet.framework.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

/**
 * Fetches property values from local properties file.
 * Property file to be used is defined in environment.properties or equivalent.
 * Defined path: /src/test/resources/properties/
 * @version Spring 2023
 * @author Arya Biswas
 */
public class FrameworkProperties {
	
	private static final String PROPERTY_DIRECTORY = System.getProperty("user.dir") + File.separator
			+ "src" + File.separator + "test" + File.separator
			+ "resources" + File.separator + "properties" + File.separator;
	
	private static Properties defaultProperties;
	private static Map<String, Properties> allProperties;
	protected String applicationVersion;
	
	/**Intializes propeties collection with default properties.*/
	protected FrameworkProperties() {
		Properties envProperties = loadPropertyFile("environment.properties", PROPERTY_DIRECTORY);
		String propertiesFile = interpretAsProperty(envProperties.getProperty("Env"));
		applicationVersion = getApplicationVersion();
		
		boolean saintsExecution = Boolean.parseBoolean(envProperties.getProperty("SaintsExecution"));
		if (saintsExecution) {
			try {
				FileUtils.copyFileToDirectory(new File(envProperties.getProperty("PropertiesSource")), new File(envProperties.getProperty("PropertiesDestFolder")));
			} catch (IOException ioe) {
				ioe.printStackTrace();
				throw new RuntimeException("Failed to copy file from properties file to " + envProperties.getProperty("PropertiesDestFolder"));
			}	
		}
		
		defaultProperties = loadPropertyFile(propertiesFile, PROPERTY_DIRECTORY);
		allProperties = new HashMap<String, Properties>();
		allProperties.put(propertiesFile, defaultProperties);
	}
	
	/**
	 * Returns the application version in use during automation execution (e.g. d4 or d3)
	 */
	public static String getApplicationVersion() {
		Properties envProperties = loadPropertyFile("environment.properties", PROPERTY_DIRECTORY);
		String propertiesFile = interpretAsProperty(envProperties.getProperty("Env"));
		String applicationVersion = null;
		if(propertiesFile.contains("d4_settings.properties")) {
			applicationVersion = "d4" ;
		}else if(propertiesFile.contains("d3_settings.properties")) {
			applicationVersion = "d3" ;
		}
		
		return applicationVersion;
	}
	
	/**
	 * Returns value of specified property in default properties file at /src/test/resources/properties.
	 * @param property Property to be searched in default properties file
	 * @return String value of property in properties file
	 */
	public static String getProperty(String property) {
		return defaultProperties.getProperty(property);
	}
	
	/**
	 * Returns value of specified property in properties file at /src/test/resources/properties/fileName.
	 * @param property Property to be searched in properties file
	 * @param fileName Properties file name or nested path under /src/test/resources/properties/fileName
	 * @return String value of property in properties file
	 */
	public static String getProperty(String property, String fileName) {
		return getProperty(property, fileName, PROPERTY_DIRECTORY);
	}
		
	/**
	 * Returns value of specified property in properties file at filePath/fileName.
	 * @param property Property to be searched in properties file
	 * @param fileName Properties file name
	 * @param filePath Directory where properties file is located
	 * @return String value of property in properties file
	 */
	public static String getProperty(String property, String fileName, String filePath) {
		fileName = interpretAsProperty(fileName);
		
		Properties properties = allProperties.get(fileName);
		
		if (properties == null) {
			properties = loadPropertyFile(fileName, filePath);
			allProperties.put(fileName, properties);
		}
		
		return properties.getProperty(property);
	}
	
	/**
	 * Returns value of specified system property.
	 * Full list of properties can be found via System.getProperties()
	 * @param property Property to be searched in system properties
	 */
	public static String getSystemProperty(String property) {
		return System.getProperty(property);
	}
	
	/*
	 * Local helper functions
	 */
	
	private static Properties loadPropertyFile(String fileName, String directory) {
		FileInputStream input;
		
		directory += (directory.endsWith(File.separator)) ? "" : File.separator;
		
		try {
			input = new FileInputStream(directory + fileName);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			throw new RuntimeException(fileName + " not found at path: " + directory, fnfe);
		}
		
		Properties properties = new Properties();
		
		try {
			properties.load(input);
		} catch (IOException | IllegalArgumentException | NullPointerException ioe){
			ioe.printStackTrace();
			throw new RuntimeException("Error loading " + fileName + " from input stream", ioe);
		}
		
		return properties;
	}
	
	private static String interpretAsProperty(String fileName) {
		if (!fileName.contains(".properties")) {
			fileName += ".properties";
		}
		
		return fileName;
	}
	
}
