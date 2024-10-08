package com.mnet.test.sanity.web;

import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.pojo.POJOSample;

public class POJOTest extends MITETest {
	
	@Test
	public void test() {
		POJOSample pojoBase = new POJOSample("testing", "retesting", null, true);
		POJOSample pojoIsSame = new POJOSample("testing", "retesting", null, true);
		POJOSample pojoNotSame = new POJOSample("testing", "wrong", 1, false);
		POJOSample pojoCopy = pojoNotSame;
		
		report.logStep(ReportLevel.INFO, "POJO base values: " 
				+ pojoBase.getStringA() + " | " + pojoBase.getStringB() + " | " 
				+ pojoBase.getNumber() + " | " + pojoBase.isFlag());
		
		report.assertCondition(pojoBase.equals(pojoIsSame), true, "POJO equality validated", "POJO value mismatch");
		
		report.assertCondition(pojoBase.equals(pojoNotSame), false, "POJO inequality validated", "Invalid equality for different objects");
		
		report.logStep(ReportLevel.INFO, "Setting values for copy...");
		pojoCopy.setStringB("retesting");
		pojoCopy.setNumber(null);
		pojoCopy.setFlag(true);
		
		report.assertCondition(pojoBase.equals(pojoCopy), true, "POJO equality validated for copy", "POJO value mismatch for copy");
	}
}
