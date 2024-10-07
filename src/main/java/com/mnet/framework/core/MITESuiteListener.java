package com.mnet.framework.core;

import org.testng.ISuite;
import org.testng.ISuiteListener;

/**
 * Handles suite level setup and cleanup.
 * Define this class as a property in TestNG XML file.
 * @author Arya Biswas
 * */
public class MITESuiteListener implements ISuiteListener {
	
	/**Suite attribute identifying number of tests in suite.*/
	public static final String TEST_COUNT = "TEST_COUNT";
	
	@Override
	public void onStart(ISuite suite) {
		suite.setAttribute(TEST_COUNT, suite.getAllMethods().size());
	}
	
}
