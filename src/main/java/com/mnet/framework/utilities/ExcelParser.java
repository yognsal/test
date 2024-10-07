package com.mnet.framework.utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mnet.framework.reporting.FrameworkLog;

/***
 * Utility to read from Excel workbooks.
 * 
 * @author Arya Biswas
 * @version Fall 2023
 */
public class ExcelParser {

	private Map<String, XSSFWorkbook> workbooks = new HashMap<>();
	
	private FrameworkLog log;
	
	public ExcelParser(FrameworkLog frameworkLog) {
		log = frameworkLog;
	}
	
	/**
	 * Retrieves row-wise data associated with the first sheet of the workbook.
	 * @param excludeHeaderRow If true, excludes the first row of the sheet from the data set.
	 */
	public List<List<String>> readSheet(String fileName, boolean excludeHeaderRow) {
		return readSheet(fileName, null, excludeHeaderRow);
	}
	
	/**
	 * Retrieves row-wise data associated with the named sheet of the workbook.
	 * @param excludeHeaderRow If true, excludes the first row of the sheet from the data set.
	 */
	public List<List<String>> readSheet(String fileName, String sheetName, boolean excludeHeaderRow) {
		XSSFWorkbook workbook = getWorkbook(fileName);
		XSSFSheet sheet;
		
		if (sheetName == null) {
			sheet = workbook.getSheetAt(0);
		} else {
			sheet = workbook.getSheet(sheetName);
		}
		
		int rowCount = sheet.getLastRowNum();
        int columnCount = sheet.getRow(0).getLastCellNum();
        
        List<List<String>> rawData = new ArrayList<>(rowCount);
        
        for (int rowIndex = excludeHeaderRow ? 1 : 0; rowIndex <= rowCount; rowIndex++) {
            XSSFRow row = sheet.getRow(rowIndex);
            List<String> dataRow = new ArrayList<>(columnCount - 1);
            
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                XSSFCell cell = row.getCell(columnIndex);
                DataFormatter formatter = new DataFormatter();
                
                dataRow.add(formatter.formatCellValue(cell));
            }
            
            rawData.add(dataRow);
        }
		
		return rawData;
	}
	
	/**
	 * Closes the named workbook.
	 */
	public void closeWorkbook(String fileName) {
		XSSFWorkbook workbook = workbooks.get(fileName);
		
		if (workbook == null) {
			return;
		}
		
		try {
			workbook.close();
		} catch (IOException ioe) {
			String err = "Failed to close workbook with name: " + fileName;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
		
		workbooks.remove(fileName);
	}
	
	/**
	 * Closes all open workbooks.
	 */
	public void closeAllWorkbooks() {
		Set<String> fileNames = workbooks.keySet();
		
		for (String fileName : fileNames) {
			closeWorkbook(fileName);
		}
	}
	
	/*
	 * Local helper functions
	 */
	
	private XSSFWorkbook getWorkbook(String fileName) {
		XSSFWorkbook workbook = workbooks.get(fileName);
		
		if (workbook == null) {
			try {
				workbook = new XSSFWorkbook(fileName);
				workbooks.put(fileName, workbook);
			} catch (IOException ioe) {
				String err = "Failed to read Excel workbook with name: " + fileName;
				log.error(err);
				log.printStackTrace(ioe);
				throw new RuntimeException(ioe);
			}
		}
		
		return workbook;
	}
}
