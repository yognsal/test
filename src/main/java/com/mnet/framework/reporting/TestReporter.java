package com.mnet.framework.reporting;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import javax.imageio.ImageIO;

import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.asserts.SoftAssert;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.framework.web.WebUtilities;

/**
 * Defines test reporting functionality via ExtentReport.
 * Report is saved as an HTML document at the path defined in environment properties.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class TestReporter {
	
	private FrameworkLog log;
	private FileUtilities fileManager;
	private ExtentReports extentReport;
	private ExtentTest currentTest;
	private ExtentTest reportTestStep;
	private ExtentHtmlReporter htmlReporter;
	private WebUtilities webDriver;
	private String testClassName;
	private String shortTestClassName;
	
	private SoftAssert softAssertions;
	
	// Environment properties defined in local properties file
	private static final String ENV_URL = FrameworkProperties.getProperty("ENV_URL");
	private static final String BROWSER = FrameworkProperties.getProperty("BROWSER");
	private static final String RELEASE = FrameworkProperties.getProperty("RELEASE");
	private static final String SAINTS_TEST_RUN = FrameworkProperties.getProperty("SAINTS_TEST_RUN");
	private static final int REPORT_MAX_PARAM_LENGTH = Integer.parseInt(FrameworkProperties.getProperty("REPORT_MAX_PARAM_LENGTH"));	
	
	private static final String TIMESTAMP = FrameworkLog.getCurrentTimestamp();
	private static final String SAINTS_RESULT = FrameworkProperties.getSystemProperty("user.dir") + File.separator + "TcStatus.txt";
	
	private static final String OS_NAME = FrameworkProperties.getSystemProperty("os.name");
	private static final String USER_NAME = FrameworkProperties.getSystemProperty("user.name");
	private static final String MACHINE_NAME = FrameworkProperties.getSystemProperty("machine.name");
	private static final String MACHINE_IP = FrameworkProperties.getSystemProperty("machine.address");
		
	/**Defines reporting / logging behavior for a given test step in ExtentReport.
	 * @see {@link TestStep.ReportLevel}*/
	@Deprecated
	public enum ReportLevel {
		INFO,
		PASS,
		ERROR,
		WARNING,
		FAIL
	}
	
	/**Defines assertion behavior for TestNG.
	 * @see {@link TestStep.AssertionLevel}*/
	@Deprecated
	public enum AssertionLevel {
		/**Fail test immediately on assertion failure.*/
		HARD,
		/**Continue test execution after assertion failure.*/
		SOFT
	}
	
	// TODO: Check behavior of desktop screenshot in parallel execution
	
	/**Defines capture method of screenshot for ExtentReport.
	 * @see {@link TestStep.ScreenshotType}*/
	@Deprecated
	public enum ScreenshotType {
		/**Screenshot that captures the browser viewport i.e visible portion*/
		VIEWPORT,
		/**Screenshot that scrolls to capture the entire web page*/
		SCROLLING,
		/**Screenshot that captures the entire desktop (including outside the browser viewport)*/
		DESKTOP
	}
	
	public TestReporter(MITETest currentTest, FrameworkLog testLogger, FileUtilities fileUtility) {
		extentReport = new ExtentReports();
		testClassName = currentTest.getClass().getSimpleName();
		shortTestClassName = currentTest.getShortTestName();
		log = testLogger;
		fileManager = fileUtility;
				
		defineReportInfo();
		htmlReporter = getReportHTML(log.getLogDirectory());
		 
		extentReport.attachReporter(htmlReporter);
	}
	
	public TestReporter(MITETest currentTest, FrameworkLog testLogger, FileUtilities fileUtility, WebUtilities driver) {
		this(currentTest, testLogger, fileUtility);
		webDriver = driver;
	}
	
	/**
	 * Starts test reporting via ExtentReport.
	 * This should be run for every test instance (@BeforeMethod).
	 * @param parameters List of parameters passed to test method.
	 */
	public void initiateTest(String[] parameters) {
		String paramList = "(";
		
		for (int i = 0; i < parameters.length; i++) {
			if (parameters[i].length() > REPORT_MAX_PARAM_LENGTH) {
				parameters[i] = parameters[i].substring(0, REPORT_MAX_PARAM_LENGTH) + "...";
			}
			
			paramList += (i == parameters.length - 1) ? parameters[i] : parameters[i] + ", ";
		}
		
		paramList += ")";
		
		currentTest = extentReport.createTest(testClassName + paramList);
		
		softAssertions = new SoftAssert();
	}
	
	/**
	 * Outputs ExtentReport to path designated in properties file.
	 * @implNote Relative path: LOG_PATH/TestName_timestamp
	 */
	public void generateReport() {
		extentReport.flush();
	}
	
	/**
	 * Logs step in ExtentReport depending on whether the provided condition matches the expected value.
	 * Assertions may be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 */
	public void assertCondition(boolean condition, boolean expected, TestStep testStep) {
		addStepToReport(testStep.toBuilder()
				.reportLevel((condition == expected) ? TestStep.ReportLevel.PASS : TestStep.ReportLevel.FAIL)
				.build());
	}
	
	/**
	 * Logs step in ExtentReport depending on whether the provided object matches the expected value.
	 * Assertions may be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 */
	public void assertValue(Object actual, Object expected, TestStep testStep) {
		assertCondition(equalOrNull(actual, expected), true, testStep);
	}
	
	/**
	 * Logs message in header of ExtentReport (before any test steps).
	 */
	public void logHeader(TestStep testStep) {
		String message = getMessageByReportLevel(testStep);
		
		switch (testStep.getReportLevel()) {
			default:
			case INFO:
				currentTest.info(message);
				break;
			case PASS:
				currentTest.pass(message);
				break;
			case ERROR:
				currentTest.error(message);
				break;
			case WARNING:
				currentTest.warning(message);
				break;
			case FAIL:
				currentTest.fail(message);
				break;
		}
	}
	
	/**
	 * Logs step in ExtentReport based on the ReportLevel provided.
	 * Additionally outputs logs to the standard console and log path defined in environment properties.
	 */
	public void logStep(TestStep testStep) {
		addStepToReport(testStep.toBuilder().assertionLevel(TestStep.AssertionLevel.HARD).build());
	}
	
	/**
	 * Marks ExtentReport test result as PASS, FAIL, or SKIP.
	 */
	public void setTestResult(ITestResult result, boolean usesWebDriver) {		
		switch (result.getStatus()) {
			default:
			case ITestResult.FAILURE:
				handleTestFailure(result, usesWebDriver);
				break;
			case ITestResult.SUCCESS:
				currentTest.pass(testClassName + " PASS");
				break;
			case ITestResult.SKIP:
				currentTest.skip(testClassName + " SKIP");
				break;
		}
	}
	
	/**
	 * Outputs test result to text file to be logged by SAINTS.
	 * @implNote TcStatus.txt
	 */
	public void generateSAINTSTestResult(boolean passed) {
		String status = testClassName + "," + (passed ? "Success" : "Failure");
		fileManager.writeToFile(SAINTS_RESULT, status);
	}
	
	/**
	 * Asserts all soft assertions at end of test.
	 */
	public void assertAll() {
		softAssertions.assertAll();
	}
	
	// TODO: Remove references to deprecated functions in test classes
	
	/* --------------------
	 * Deprecated functions
	 * --------------------
	 */
	
	/**
	 * Logs step in ExtentReport depending on whether the provided condition matches the expected value.
	 * In the case of assertion failure, terminates the test immediately with a status of Fail.
	 * For soft assertions (test does not terminate on failure),
	 * call {@link #assertCondition(AssertionLevel, boolean, boolean, String, String)} instead.
	 * @see {@link assertCondition(boolean, boolean, TestStep)}
	 */
	@Deprecated
	public void assertCondition(boolean condition, boolean expected, String passMessage, String failMessage) {
		assertCondition(AssertionLevel.HARD, condition, expected, passMessage, failMessage);
	}
	
	/**
	 * Logs step in ExtentReport depending on whether the provided condition matches the expected value.
	 * Assertions can be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 * @see {@link assertCondition(boolean, boolean, TestStep)}
	 */
	@Deprecated
	public void assertCondition(AssertionLevel level, boolean condition, boolean expected, String passMessage, String failMessage) {
		addStepToReport(TestStep.builder()
				.assertionLevel(TestStep.AssertionLevel.valueOf(level.toString()))
				.reportLevel((condition == expected) ? TestStep.ReportLevel.PASS : TestStep.ReportLevel.FAIL)
				.message(passMessage)
				.failMessage(failMessage)
				.screenshotType(TestStep.ScreenshotType.NONE)
				.build());
	}
	
	/**
	 * Logs step with screenshot in ExtentReport depending on whether the provided condition matches the expected value.
	 * In the case of assertion failure, terminates the test immediately with a status of Fail.
	 * @apiNote Provides functionality to capture the entire page / desktop when taking a screenshot, if desired.
	 * @implNote For soft assertions (test does not terminate on failure),
	 * call {@link #assertWithScreenshot(AssertionLevel, boolean, boolean, String, String)} instead.
	 * @see {@link #assertCondition(boolean, boolean, TestStep)}
	 */
	@Deprecated
	public void assertWithScreenshot(boolean condition, boolean expected, String passMessage, String failMessage, ScreenshotType screenshotType) {
		assertWithScreenshot(AssertionLevel.HARD, condition, expected, passMessage, failMessage, screenshotType);
	}
	
	/**
	 * Logs step with viewport screenshot in ExtentReport depending on whether the provided condition matches the expected value.
	 * Assertions can be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 * @see {@link #assertCondition(boolean, boolean, TestStep)}
	 */
	@Deprecated
	public void assertWithScreenshot(AssertionLevel level, boolean condition, boolean expected, String passMessage, String failMessage) {
		assertWithScreenshot(level, condition, expected, passMessage, failMessage, ScreenshotType.VIEWPORT);
	}
	
	/**
	 * Logs step with screenshot in ExtentReport depending on whether the provided condition matches the expected value.
	 * Assertions can be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 * Provides functionality to capture the entire page / desktop when taking a screenshot, if desired.
	 * @see {@link #assertCondition(boolean, boolean, TestStep)}
	 */
	@Deprecated
	public void assertWithScreenshot(AssertionLevel level, boolean condition, boolean expected, 
			String passMessage, String failMessage, ScreenshotType screenshotType) {
		addStepToReport(TestStep.builder()
				.assertionLevel(TestStep.AssertionLevel.valueOf(level.toString()))
				.reportLevel((condition == expected) ? TestStep.ReportLevel.PASS : TestStep.ReportLevel.FAIL)
				.message(passMessage)
				.failMessage(failMessage)
				.screenshotType(TestStep.ScreenshotType.valueOf(screenshotType.toString()))
				.build());
	}
	
	/**
	 * Logs step in ExtentReport depending on whether the provided object matches the expected value.
	 * In the case of assertion failure, terminates the test immediately with a status of Fail.
	 * For soft assertions (test does not terminate on failure),
	 * call {@link #assertValue(AssertionLevel, boolean, boolean, String, String)} instead.
	 * @see {@link #assertValue(Object, Object, TestStep)}
	 */
	@Deprecated
	public void assertValue(Object actual, Object expected, String passMessage, String failMessage) {
		assertValue(AssertionLevel.HARD, actual, expected, passMessage, failMessage);
	}
	
	/**
	 * Logs step in ExtentReport depending on whether the provided object matches the expected value.
	 * Assertions can be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 * @see {@link #assertValue(Object, Object, TestStep)}
	 */
	@Deprecated
	public void assertValue(AssertionLevel level, Object actual, Object expected, String passMessage, String failMessage) {
		addStepToReport(TestStep.builder()
				.assertionLevel(TestStep.AssertionLevel.valueOf(level.toString()))
				.reportLevel(equalOrNull(actual, expected) ? TestStep.ReportLevel.PASS : TestStep.ReportLevel.FAIL)
				.message(passMessage)
				.failMessage(failMessage)
				.build());
	}
	
	/**
	 * Logs step with screenshot in ExtentReport depending on whether the provided condition matches the expected value.
	 * In the case of assertion failure, terminates the test immediately with a status of Fail.
	 * @implNote For soft assertions (test does not terminate on failure),
	 * call {@link #assertWithScreenshot(AssertionLevel, boolean, boolean, String, String)} instead.
	 * @see {@link #assertCondition(boolean, boolean, TestStep)}
	 */
	@Deprecated
	public void assertWithScreenshot(boolean condition, boolean expected, String passMessage, String failMessage) {
		assertWithScreenshot(AssertionLevel.HARD, condition, expected, passMessage, failMessage);
	}
	
	/**
	 * Logs step with viewport screenshot in ExtentReport depending on whether the provided object matches the expected value.
	 * In the case of assertion failure, terminates the test immediately with a status of Fail.
	 * For soft assertions (test does not terminate on failure),
	 * call assertWithScreenshot(AssertionLevel.SOFT, Object, Object, String, String) instead.
	 * @see {@link #assertValue(Object, Object, TestStep)}
	 */
	@Deprecated
	public void assertWithScreenshot(Object actual, Object expected, String passMessage, String failMessage) {
		assertWithScreenshot(AssertionLevel.HARD, actual, expected, passMessage, failMessage);
	}
	
	/**
	 * Logs step with viewport screenshot in ExtentReport depending on whether the provided condition matches the expected value.
	 * Assertions can be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 * @see {@link #assertValue(Object, Object, TestStep)}
	 */
	@Deprecated
	public void assertWithScreenshot(AssertionLevel level, Object actual, Object expected, String passMessage, String failMessage) {
		assertWithScreenshot(level, actual, expected, passMessage, failMessage, ScreenshotType.VIEWPORT);
	}
	
	/**
	 * Logs step with screenshot in ExtentReport depending on whether the provided condition matches the expected value.
	 * Assertions can be HARD (terminates the test immediately with a status of Fail) or SOFT (test does not terminate on failure).
	 * Provides functionality to capture the entire page when taking a screenshot, if desired.
	 * @see {@link #assertValue(Object, Object, TestStep)}
	 */
	@Deprecated
	public void assertWithScreenshot(AssertionLevel level, Object actual, Object expected, 
			String passMessage, String failMessage, ScreenshotType screenshotType) {
		addStepToReport(TestStep.builder()
				.assertionLevel(TestStep.AssertionLevel.valueOf(level.toString()))
				.reportLevel(equalOrNull(actual, expected) ? TestStep.ReportLevel.PASS : TestStep.ReportLevel.FAIL)
				.message(passMessage)
				.failMessage(failMessage)
				.screenshotType(TestStep.ScreenshotType.valueOf(screenshotType.toString()))
				.build());
	}
	
	/**
	 * Logs message in header of ExtentReport (before any test steps).
	 * @see {@link #logHeader(TestStep)}
	 */
	@Deprecated
	public void logHeader(ReportLevel level, String message) {
		switch (level) {
			default:
			case INFO:
				currentTest.info(message);
				break;
			case PASS:
				currentTest.pass(message);
				break;
			case ERROR:
				currentTest.error(message);
				break;
			case WARNING:
				currentTest.warning(message);
				break;
			case FAIL:
				currentTest.fail(message);
				break;
		}
	}
	
	/**
	 * Logs step in ExtentReport based on the ReportLevel provided.
	 * Additionally outputs logs to the standard console and log path defined in environment properties.
	 * @param level ReportLevel.INFO, PASS, FAIL
	 * @param message Message to be logged.
	 * @see {@link #logStep(TestStep)}
	 */
	@Deprecated
	public void logStep(ReportLevel level, String message) {
		addStepToReport(TestStep.builder()
				.assertionLevel(TestStep.AssertionLevel.HARD)
				.reportLevel(TestStep.ReportLevel.valueOf(level.toString()))
				.message(message)
				.build());
	}
	
	/**
	 * Logs step with screenshot of webbrowser in ExtentReport. Step is logged based on the ReportLevel provided.
	 * Additionally outputs logs to the standard console and log path defined in environment properties.
	 * Behavior is only defined when TestAttribute.WEBAPP is set for the current test.
	 * @param level ReportLevel.INFO, PASS, FAIL
	 * @param message Message to be logged.
	 * @see {@link #logStep(TestStep)}
	 */
	@Deprecated
	public void logStepWithScreenshot(ReportLevel level, String message) {
		logStepWithScreenshot(level, message, ScreenshotType.VIEWPORT);
	}
	
	/**
	 * Logs step with screenshot of webbrowser in ExtentReport. Step is logged based on the ReportLevel provided.
	 * Additionally outputs logs to the standard console and log path defined in environment properties.
	 * Provides functionality to capture the entire page when taking a screenshot, if desired.
	 * Behavior is only defined when TestAttribute.WEBAPP is set for the current test.
	 * @param level ReportLevel.INFO, PASS, FAIL
	 * @param message Message to be logged.
	 * @see {@link #logStep(TestStep)}
	 */
	@Deprecated
	public void logStepWithScreenshot(ReportLevel level, String message, ScreenshotType screenshotType) {
		addStepToReport(TestStep.builder()
				.assertionLevel(TestStep.AssertionLevel.HARD)
				.reportLevel(TestStep.ReportLevel.valueOf(level.toString()))
				.message(message)
				.screenshotType(TestStep.ScreenshotType.valueOf(screenshotType.toString()))
				.build());
	}
	
	/* --------------------
	 * Helper functions
	 * --------------------
	 */
	
	private void defineReportInfo() {
		extentReport.setSystemInfo("OS : ", OS_NAME);
		extentReport.setSystemInfo("Release : ", RELEASE);
		extentReport.setSystemInfo("Test Run", SAINTS_TEST_RUN);
		extentReport.setSystemInfo("User Name : ", USER_NAME);
		extentReport.setSystemInfo("Machine : ", MACHINE_NAME);
		extentReport.setSystemInfo("IP Address : ", MACHINE_IP);
		extentReport.setSystemInfo("Browser : ", BROWSER);
		// extent.setSystemInfo("Build : ", buildInfo); TODO: define per microservice
		extentReport.setSystemInfo("Environment : ", ENV_URL);
	}
	
	private ExtentHtmlReporter getReportHTML(String filePath) {
		ExtentHtmlReporter extentHTML = new ExtentHtmlReporter(
				filePath + File.separator + shortTestClassName + "_" + TIMESTAMP + ".html");
		
		extentHTML.config().setDocumentTitle("Merlin Automation Report");
		extentHTML.config().setReportName("Automation Report");
		extentHTML.config().setTheme(Theme.STANDARD);
		extentHTML.config().setTimeStampFormat("MMM dd, yyyy HH:mm:ss");
		extentHTML.config().setEncoding("utf-8");
		extentHTML.config().setCSS(".r-img {width: 100px;}");
		
		return extentHTML;
	}
	
	private void logStepByLevel(TestStep testStep) {
		String message = getMessageByReportLevel(testStep);
		
		reportTestStep = currentTest.createNode(message);
		
		switch (testStep.getReportLevel()) {
			default:
			case INFO:
				reportTestStep.info(message);
				log.info(message);
				break;
			case PASS:
				reportTestStep.pass(message);
				log.info(message);
				break;
			case ERROR:
				reportTestStep.error(message);
				log.error(message);
				break;
			case WARNING:
				reportTestStep.warning(message);
				log.warn(message);
				break;
			case FAIL:
				reportTestStep.fail(message);
				log.error(message);
				break;
		}
		
		Set<String> tags = testStep.getTags();
		
		if (tags.size() > 0) {
			reportTestStep.assignCategory(tags.toArray(String[]::new));
		}
	}
	
	private String getMessageByReportLevel(TestStep testStep) {
		String message = testStep.getMessage();
		
		switch (testStep.getReportLevel()) {
			default:
			case INFO:
				return message;
			case ERROR:
			case WARNING:
				return (message.isEmpty() ? testStep.getFailMessage() : message);
			case FAIL:
				return testStep.getFailMessage();
		}
	}
	
	private void checkStepFailure(TestStep.AssertionLevel assertLevel, TestStep.ReportLevel reportLevel, String message) {
		if (reportLevel == TestStep.ReportLevel.FAIL) {
			if (assertLevel == TestStep.AssertionLevel.SOFT) {
				softAssertions.assertTrue(false, "Test failure: " + message);
			} else {
				Assert.fail("Test failure: " + message);
			}
		}
	}
	
	private void addScreenshotToReport(TestStep.ScreenshotType screenshotType) {
		String screenshotFile = FrameworkLog.getCurrentTimestamp() + ".jpg";
		String screenshotPath = log.getLogDirectory() + File.separator + screenshotFile;
		
		switch (screenshotType) {
			case VIEWPORT:
				webDriver.takeScreenshot(screenshotPath);
				break;
			case SCROLLING:
				webDriver.takeScrollingScreenshot(screenshotPath);
				break;
			case DESKTOP:
				takeDesktopScreenshot(screenshotPath);
				break;
			case NONE:
				return;
		}
		
		try {
			reportTestStep.addScreenCaptureFromPath(screenshotFile);
		} catch (IOException ioe) {
			log.error("Failed to locate screenshot: " + screenshotFile);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	private void addStepToReport(TestStep testStep) {
		String message = getMessageByReportLevel(testStep);
		
		logStepByLevel(testStep);
		
		addScreenshotToReport(testStep.getScreenshotType());
		
		checkStepFailure(testStep.getAssertionLevel(), testStep.getReportLevel(), message);
	}
	
	private void handleTestFailure(ITestResult result, boolean takeScreenshot) {
		Throwable thrownException = result.getThrowable();
		
		if (thrownException == null || thrownException instanceof AssertionError) {
			currentTest.fail(testClassName + " FAIL");
		} else {
			String errorMessageHeader = "Runtime error - see attached logs for stack trace";
			String errorMessageNode = "Runtime error: " + thrownException.getMessage();
			
			String htmlError = "<a href='../" + log.getRelativeLogFilePath() + "'>"; 
			
			currentTest.error(htmlError + errorMessageHeader + "</a>");
			
			logStep(TestStep.builder()
					.reportLevel(TestStep.ReportLevel.ERROR)
					.message(errorMessageNode)
					.screenshotType(takeScreenshot ? TestStep.ScreenshotType.VIEWPORT : TestStep.ScreenshotType.NONE)
					.build());
			
			reportTestStep.error(htmlError + "Link to logs" + "</a>");
			
			log.printStackTrace(thrownException);
		}
	}
	
	private boolean equalOrNull(Object actual, Object expected) {
		if (Objects.isNull(actual)) {
			return (Objects.isNull(expected) || expected.equals("")) ? true : false;
		} else if (actual.equals(expected)) {
			return true;
		}
		
		return false;
	}
	
	private void takeDesktopScreenshot(String path) {
		Robot robot;
		
		try {
			robot= new Robot();
		} catch (AWTException awte) {
			String err = "Platform configuration does not allow low-level input control";
			log.error(err);
			log.printStackTrace(awte);
			throw new RuntimeException(err);
		}
		
        Rectangle capture = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        BufferedImage image = robot.createScreenCapture(capture);
      
        try {
        	ImageIO.write(image, "jpg", new File(path));
        } catch (IOException ioe) {
        	String err = "Failed to write image to path: " + path + ".jpg";
        	log.error(err);
        	log.printStackTrace(ioe);
        	throw new RuntimeException(err);
        }
	}
	
}
