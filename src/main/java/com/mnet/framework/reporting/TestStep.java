package com.mnet.framework.reporting;

import java.util.Set;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

/**
 * Represents an individual step in an ExtentReport.
 * Provides functionality to customize the appearance of the test step.
 * @implNote Construct a test step: {@code TestStep.builder().message("").reportLevel(ReportLevel.INFO).build()}
 * @implNote Copy an existing test step to rebuild: {@code testStep.toBuilder().message("new message").build()}
 * @author Arya Biswas
 * @version Q1 2024
 */
@Builder(toBuilder = true)
@Getter(AccessLevel.PROTECTED)
public class TestStep {

	/**Defines reporting level for test steps (non-assertions). Defaults to {@link ReportLevel.INFO}*/
	@Builder.Default
	private ReportLevel reportLevel = ReportLevel.INFO;
	
	/**Defines assertion behavior for asserts. Defaults to {@link AssertionLevel.HARD}*/
	@Builder.Default
	@Setter(AccessLevel.PROTECTED)
	private AssertionLevel assertionLevel = AssertionLevel.HARD;
	
	/**Defines assertion behavior for asserts. Defaults to {@link ScreenshotType.NONE}*/
	@Builder.Default
	private ScreenshotType screenshotType = ScreenshotType.NONE;
	
	/**Message to be printed for test step / assertion pass.*/
	@Builder.Default
	private String message = "";
	
	/**
	 * Unique failure message for test step / assertion fail.
	 * If not provided in builder, defaults to "FAIL: " + {@link getMessage()}
	 */
	@Getter(AccessLevel.NONE)
	private String failMessage;
	
	/**Defines category tags to associate with test step.
	 * @apiNote Recommended for requirement tagging.*/
	@Singular
	private Set<String> tags;
	
	/**Defines reporting / logging behavior for a given test step in ExtentReport.*/
	public enum ReportLevel {
		INFO,
		PASS,
		ERROR,
		WARNING,
		FAIL
	}

	/**Defines assertion behavior for TestNG.*/
	public enum AssertionLevel {
		/**Fail test immediately on assertion failure.*/
		HARD,
		/**Continue test execution after assertion failure.*/
		SOFT
	}
	
	/**Defines capture method of screenshot for ExtentReport.*/
	public enum ScreenshotType {
		/**Screenshot that captures the browser viewport i.e visible portion*/
		VIEWPORT,
		/**Screenshot that scrolls to capture the entire web page*/
		SCROLLING,
		/**Screenshot that captures the entire desktop (including outside the browser viewport)*/
		DESKTOP,
		/**No screenshot is associated with the test step*/
		NONE
	}
	
	protected String getFailMessage() {
		return (failMessage == null) ? "FAIL: " + message : failMessage;
	}
	
}
