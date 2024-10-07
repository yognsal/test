package com.mnet.framework.reporting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;

/**
 * Framework-level logging functionality.
 * Logs reflect in console output and are stored at the log path defined in environment.properties.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class FrameworkLog
{
	private Logger logger;
	private ConsoleAppender consoleAppender;
	private RollingFileAppender rollingFileAppender;
	private String timestamp = getCurrentTimestamp();
	
	private static PatternLayout layout = new PatternLayout("%d{MM-dd-yyyy HH:mm:ss} %-5p %m%n");
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	
	private String logDirectory;
	private String logFilePath;
	
	public static final String LOG_DIR = FrameworkProperties.getProperty("LOG_DIR");
	
	public FrameworkLog(MITETest currentTest) {
		logger = Logger.getLogger(currentTest.getClass());
	
		consoleAppender = new ConsoleAppender(layout, "System.out");
		logDirectory = LOG_DIR + File.separator + currentTest.getShortTestName() + "_" + timestamp + File.separator;
		logFilePath = logDirectory + timestamp + "_log.log";
		
		try {
			rollingFileAppender = new RollingFileAppender(layout, logFilePath, true);
		} catch (IOException ioe) {
			ioe.printStackTrace();
			throw new RuntimeException("Could not configure log4j appender", ioe);
		}
				
		rollingFileAppender.setMaxFileSize("10mb");
		rollingFileAppender.setMaxBackupIndex(10);
		consoleAppender.setName("Console");
		logger.setAdditivity(false);
		logger.addAppender(consoleAppender);
		logger.addAppender(rollingFileAppender);
	}
	
	/**
	 * @return Current timestamp in the format: yyyy-MM-dd_HH-mm-ss
	 */
	public static String getCurrentTimestamp() {
		return dateFormat.format(new Date());
	}
	
	/**
	 * Outputs message to console log with level INFO.
	 */
	public void info(String message) {
		log(message, Level.INFO, getCaller());
	}
	
	/**
	 * Outputs message to console log with level INFO. Explicitly defines calling class.
	 */
	public void info (String message, Class<?> caller) {
		log(message, Level.INFO, caller.getName());
	}
	
	/**
	 * Outputs message to console log with level WARN.
	 */
	public void warn(String message) {
		log(message, Level.WARN, getCaller());
	}
	
	/**
	 * Outputs message to console log with level WARN. Explicitly defines calling class.
	 */
	public void warn(String message, Class<?> caller) {
		log(message, Level.WARN, caller.getName());
	}

	/**
	 * Outputs message to console log with level ERROR.
	 */
	public void error(String message) {
		log(message, Level.ERROR, getCaller());
	}
	
	/**
	 * Outputs message to console log with level ERROR. Explicitly defines calling class.
	 */
	public void error(String message, Class<?> caller) {
		log(message, Level.ERROR, caller.getName());
	}
	
	/**
	 * Returns local file path of directory where logs and reports are stored for the current test instance.
	 * Includes trailing file separator.
	 */
	public String getLogDirectory() {
		return logDirectory;
	}
	
	/**
	 * Returns relative file path of directory where logs and reports are stored for the current test instance, excluding LOG_DIR.
	 * Includes trailing file separator.
	 */
	public String getRelativeLogDirectory() {
		return logDirectory.replace(LOG_DIR, "");
	}
	
	/**
	 * Returns fully qualified local file path of log file.
	 */
	public String getLogFilePath() {
		return logFilePath;
	}
	
	/**
	 * Returns file path of log file, excluding LOG_DIR.
	 */
	public String getRelativeLogFilePath() {
		return logFilePath.replace(LOG_DIR, "");
	}
	
	/**
	 * Prints stack trace to console log for designated exception.
	 */
	public void printStackTrace(Throwable exception) 
	{
		StringWriter stringWriter = new StringWriter();
		
		exception.printStackTrace(new PrintWriter(stringWriter));
		error(stringWriter.toString());
	}
	
	/**
	 * Closes logger and stops writing to file / console.
	 **/
	public void close() {
		rollingFileAppender.close();
		consoleAppender.close();
	}
	
	/**
	 * Helper functions
	 */
	
	private void log(String message, Level logLevel, String callingClass) {
		logger.setLevel(logLevel);
		String logMessage = callingClass + " - " + message;

		if (logLevel.equals(Level.ERROR)) {
			logger.error(logMessage);
		} else if (logLevel.equals(Level.WARN)) {
			logger.warn(logMessage);
		} else {
			logger.info(logMessage);
		}
	}
	
	private String getCaller() {
		return Thread.currentThread().getStackTrace()[3].getClassName();
	}
	
}