package com.mnet.test.sanity.framework;

import java.util.List;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter.ReportLevel;

public class DatabaseConnectionTest extends MITETest {

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.DATABASE);
		relativeDataDirectory = "sanity/framework";
		super.initialize(context);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void databaseConnectionTest(String query, String expectedValue, String expectedRows, String checkColumn) {
		report.logStep(ReportLevel.INFO, "Executing database query: " + query);
		
		QueryResult queryResult = database.executeQuery(query);
		
		report.logStep(ReportLevel.PASS, "Query executed successfully");
		
		String result = queryResult.getFirstCellValue();
		
		report.assertValue(result, expectedValue,
				"Database value matched expected result: " + expectedValue,
				"Value mismatch - Database: " + result + " | Test data: " + expectedValue);
		
		List<String> firstRow = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();
		
		report.assertCondition(firstRow.size() == columnNames.size(), true, 
				"Query result has expected number of columns: " + firstRow.size(), 
				"Query result mismatch - columns in first row: " + firstRow.size() + " | columns in result: " + columnNames.size());
		
		String mappedValues = "Query first row output: \n";
		
		for (int i = 0; i < firstRow.size(); i++) {
			mappedValues += columnNames.get(i) + ": " + firstRow.get(i) + "\n";
		}
		
		report.logStep(ReportLevel.INFO, mappedValues);
		
		List<String> columnValues = queryResult.getColumn(checkColumn);
		List<List<String>> allRows = queryResult.getAllRows();
		
		report.assertValue(columnValues.get(0), expectedValue, 
				"Column contains expected result - " + checkColumn + ": " + expectedValue,
				"Column value mismatch - expected: " + expectedValue + " | column result: " + columnValues.get(0));
		
		report.assertCondition(columnValues.size() == allRows.size(), true, 
				"Number of rows matches number of values in column: " + columnValues.size(), 
				"Query result mismatch - number of rows: " + allRows.size() + " | " + "number of values in column: " + columnValues.size());
	}
}
