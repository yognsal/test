package com.mnet.test.sanity.framework;

import java.util.Arrays;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.reporting.TestReporter.AssertionLevel;
import com.mnet.middleware.utilities.TantoDriver;
import com.mnet.middleware.utilities.TantoProfileResponseValidator;

public class AssertionTest extends MITETest {
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		relativeDataDirectory = "sanity/framework";
		super.initialize(context);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void softAssertionTest(String iteration) {
		boolean result = (iteration.equals("1")) ? false : true;
		
		report.assertCondition(AssertionLevel.SOFT, result, true, "Test step A passed", "Test step A failed");
		report.assertCondition(AssertionLevel.SOFT, true, true, "Test step B passed", "Test step B failed");
		report.assertCondition(AssertionLevel.SOFT, true, true, "Test step C passed", "Test step C failed");
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void hardAssertionTest(String iteration, String value) {
		report.assertValue(iteration, value, "Iteration matches value: " + value, "Mismatch - iteration: " + iteration + " | value: " + value);
	}
}
