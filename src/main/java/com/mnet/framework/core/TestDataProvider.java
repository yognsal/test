package com.mnet.framework.core;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

import com.mnet.framework.utilities.ExcelParser;

/**
 * Data provider class for test classes. Reads from Excel sheet located at designated path.
 * IMPORTANT: Only accepts rows flagged with 'y' in first column (required for all test data sheets)
 * @version Spring 2023
 * @author Arya Biswas
 */
public class TestDataProvider {
		
	private static final String DATA_PATH = FrameworkProperties.getSystemProperty("user.dir") + File.separator 
			+ FrameworkProperties.getProperty("DATA_PATH") + File.separator;
		
    @DataProvider(name="TestData")
    public String[][] readTestData(ITestContext context, Method currentMethod) {
    	String relativeDataPath = (String) context.getAttribute(MITETest.DATA_CONTEXT);
    	relativeDataPath = (StringUtils.isEmpty(relativeDataPath)) ? "" : relativeDataPath + File.separator;
    	
        return parseData(context,
        			DATA_PATH + relativeDataPath + currentMethod.getDeclaringClass().getSimpleName() + ".xlsx",
        			currentMethod.getName());
    }
    
    /**
     * Local helper functions
     */
    
    private static String[][] parseData(ITestContext context, String fileName, String methodName) {
    	ExcelParser excel = (ExcelParser) context.getAttribute(MITETest.EXCEL_CONTEXT);
    	
    	List<List<String>> rawData = (context.getAllTestMethods()[0].getTestClass().getTestMethods().length > 1) ?
    			excel.readSheet(fileName, methodName, true) :
    			excel.readSheet(fileName, null, true);
    	
    	int validRows = 0;
    	
    	for (List<String> row : rawData) {
    		if (row.get(0).equalsIgnoreCase("y")) {
    			validRows++;
    		}
    	}
    	
    	if (validRows == 0) {
    		throw new RuntimeException("No usable test data found. "
        			+ "Please ensure all desired test rows are marked with 'y' in the first column of the data sheet: "
        			+ fileName);
    	}
    	
    	int columns = rawData.get(0).size();
    	String[][] data = new String[validRows][columns - 1]; // exclude data flagging column from size
    	
    	int index = 0;
    	for (List<String> row : rawData) {
    		if (row.get(0).equalsIgnoreCase("y")) {
    			data[index++] = row.subList(1, columns).toArray(new String[columns - 1]);
    		}
    	}
    	    	
    	excel.closeWorkbook(fileName);
    	
    	return data;
    }
}
