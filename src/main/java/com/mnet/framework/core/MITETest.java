package com.mnet.framework.core;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ISuite;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import com.mnet.framework.azure.AzureManagedIdentity;
import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.email.EmailParser;
import com.mnet.framework.email.GmailParser;
import com.mnet.framework.email.OutlookParser;
import com.mnet.framework.middleware.UnixConnector;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.utilities.ExcelParser;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.framework.web.WebUtilities;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Extend this class and override the annotated functions for all derived test classes.
 * Sample test in com.mnet.sanity.web.LoginTest.
 * @version Spring 2023
 * @author Arya Biswas
 */
@Getter
public abstract class MITETest implements IHookable {
	
	/**Attribute identifier for FrameworkLog from ITestContext*/
	public static String LOG_CONTEXT = "log";
	/**Attribute identifier for ExcelParser from ITestContext*/
	public static String EXCEL_CONTEXT = "excel";
	/**Attribute identifier for relative data path string from ITestContext*/
	public static String DATA_CONTEXT = "data";
	
	protected FrameworkProperties properties;
	protected TestReporter report;
	protected FrameworkLog log; // TODO: upgrade to log4j2
	protected WebUtilities webDriver;
	protected EmailParser email;
	protected FileUtilities fileManager;
	protected ExcelParser excel;
	protected DatabaseConnector database;
	protected UnixConnector remoteMachine;
	protected AzureManagedIdentity azure;
	
	// TODO: Move out of MITETest as this is business logic. Property can be accessed directly
	@Getter(AccessLevel.NONE)
	protected boolean manualCustomerSetup;
	
	/**
	 * Defines what type of test is being executed so the corresponding utility classes can be initialized.
	 * Set these attributes before calling super.initialize() in subclass.
	 */
	@Getter(AccessLevel.NONE)
	protected Set<TestAttribute> attributes = EnumSet.noneOf(TestAttribute.class);
	
	/**
	 * Shorthand name for test - used to define directory structure due to system path length limitations.
	 * Will be truncated if greater than SHORT_TEST_NAME_MAX_LENGTH in application properties.
	 * @apiNote Set this value before calling super.initialize() in subclass.*/
	@Getter(AccessLevel.NONE)
	protected String shortTestName;
	
	/**
	 * Directory relative to DATA_PATH where test data Excel is located.
	 * Naming convention is the use case or other relevant identifier (e.x. if set to "UC051", data sheet is found at DATA_PATH/UC051)
	 * @apiNote Set this value before calling super.initialize() in subclass. If not set, uses DATA_PATH as the source directory for TestDataProvider.
	 */
	@Getter(AccessLevel.NONE)
	protected String relativeDataDirectory;
	
	protected enum TestAttribute {
		/**Test uses com.mnet.framework.web.WebUtilities*/
		WEBAPP,
		/**Test uses com.mnet.framework.email*/
		EMAIL,
		/**Test uses com.mnet.framework.database.DataBaseConnector*/
		DATABASE,
		/**Test uses com.mnet.framework.middleware.UnixConnection*/
		REMOTE_MACHINE,
		/**No longer used - use APIRequest and RestAPIManager static methods instead*/
		@Deprecated
		API,
		/**Test uses com.mnet.framework.azure.AzureManagedIdentity*/
		AZURE
	}
	
	private Integer shortTestNameMaxLength;
	
	// TODO: Define UncaughtExceptionHandler to log on unchecked exceptions
	
	/**
	 * Performs initialization for test classes. Runs before any tests in a given test class is run.
	 * Override in subclass to define applicable attributes: attributes.add(TestAttribute)
	 * @param context Pass ITestContext from subclass.
	 */
	@BeforeClass
	public void initialize(ITestContext context) {
		properties = new FrameworkProperties();
		log = new FrameworkLog(this);
		fileManager = new FileUtilities(log);
		excel = new ExcelParser(log);
		
		// Define context for TestNG components
		context.setAttribute(LOG_CONTEXT, log);
		context.setAttribute(EXCEL_CONTEXT, excel);
		context.setAttribute(DATA_CONTEXT, relativeDataDirectory);
		
		if (usesAttribute(TestAttribute.WEBAPP)) {
			webDriver = new WebUtilities(log);
			report = new TestReporter(this, log, fileManager, webDriver);
		} else {
			report = new TestReporter(this, log, fileManager);
		}
		
		if (usesAttribute(TestAttribute.DATABASE)) {
			database = new DatabaseConnector(log);
		}
		
		if (usesAttribute(TestAttribute.REMOTE_MACHINE)) {
			remoteMachine = new UnixConnector(log);
		}
		
		if (usesAttribute(TestAttribute.EMAIL)) {
			String emailId = EmailParser.getMFAEmailID();
			
			if (emailId.contains("@gmail")) {
				email = new GmailParser(log);
			} else {
				email = new OutlookParser(log);
			}
		}
		
		if (usesAttribute(TestAttribute.AZURE)) {
			azure = new AzureManagedIdentity(log, fileManager);
		}
		
		manualCustomerSetup = Boolean
				.parseBoolean(FrameworkProperties.getProperty("MANUAL_CUSTOMER_SETUP"));
	}
	
	/**
	 * Performs setup on a per-test iteration basis.
	 */
	@BeforeMethod
	public void setup(Object[] parameters) {
		// Pass array of parameters as String[]
		report.initiateTest(Arrays.stream(parameters).map(Object::toString).toArray(String[]::new));
	}
	
	/**
	 * Invokes test and performs any necessary operations before test result is reported back to TestNG.
	 */
	@Override
	public void run(IHookCallBack callback, ITestResult result) {
		callback.runTestMethod(result);
		report.assertAll();
	}
	
	/**
	 * Performs cleanup on a per-test iteration basis.
	 */
	@AfterMethod
	public void cleanup(ITestResult result) {
		log.info("----------------");
		report.setTestResult(result, usesAttribute(TestAttribute.WEBAPP));
	}
	
	/**
	 * Finalizes test results and reporting after all tests belonging to a given test class have run.
	 */
	@AfterClass
	public void reportAndTeardown(ITestContext context) {
		
		if (usesAttribute(TestAttribute.WEBAPP)) {
			webDriver.quitDriver();
		}
		
		if (usesAttribute(TestAttribute.EMAIL)) {
			email.close();
		}
		
		if (usesAttribute(TestAttribute.DATABASE)) {
			database.closeConnections();
		}
		
		if (usesAttribute(TestAttribute.REMOTE_MACHINE)) {
			remoteMachine.closeConnection();
		}
		
		report.generateReport();
		report.generateSAINTSTestResult((context.getFailedTests().size() == 0));
		
		log.close();
		copyReportsToSuiteFolder(context);
	}
	
	/**
	 * Returns shorthand name for test - used to define directory structure due to system path length limitations.
	 * Will be truncated if greater than SHORT_TEST_NAME_MAX_LENGTH in application properties.
	 */
	public String getShortTestName() {
		if (shortTestNameMaxLength == null) {
			shortTestNameMaxLength = Integer.parseInt(FrameworkProperties.getProperty("SHORT_TEST_NAME_MAX_LENGTH"));
		}
		
		if (shortTestName == null) {
			shortTestName = this.getClass().getSimpleName();
		}
			
		if (shortTestName.length() > shortTestNameMaxLength) {
			shortTestName = shortTestName.substring(0, shortTestNameMaxLength + 1);
		}
		
		try {
			Paths.get(shortTestName);
		} catch (InvalidPathException ipe) {
			throw new RuntimeException("Provided short test name cannot be part of a Windows path: " + shortTestName);
		}
		
		return shortTestName;
	}
	
	/*
	 * Helper functions
	 */
	
	private boolean usesAttribute(TestAttribute attribute) {
		return attributes.contains(attribute);
	}
	
	private void copyReportsToSuiteFolder(ITestContext context) {
		/**Directory where all suite reports are saved.*/
		String SUITE_DIR = FrameworkProperties.getProperty("LOG_DIR") + File.separator + "MITE_Suite";
		
		ISuite testSuite = context.getSuite();
		Integer testCount = (Integer) testSuite.getAttribute(MITESuiteListener.TEST_COUNT);
		
		if (testCount == null || testCount <= 1) {
			return;
		}
		
		fileManager.copyDirectoryToPath(log.getLogDirectory(), SUITE_DIR, true);
	}
		
}
