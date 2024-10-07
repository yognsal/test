package com.mnet.middleware.utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;

public class PayloadExcelUtilities {
	private FrameworkLog log;
	private FileInputStream input;
	private FileOutputStream outputStream;
	private XSSFWorkbook wb;
	private XSSFSheet sheet;
	private static String defaultExcelLocation = FrameworkProperties.getProperty("DEFAULT_EXCEL_LOCATION");

	public enum PayloadAttribute {
		CUSTOMERID("Customer ID"), DEVICESERIAL("DeviceSerialNumber"), DEVICEMODEL("DeviceModelNumber"),
		TRANSMIITERSERIAL("TransmitterSerial"), EPISODEEGMDATEANDTIME("EpisodeEgmDateAndTime"),
		EPISODECOUNT("EpisodeEgmsCount"), SESSIONDATEANDTIME("SessionDateAndTime"), SESSIONTIMEZONE("SessionTimeZone"),
		CLINICALCOMMENTDATE("ClinicalCommentDate"), CLINICALCOMMENT("ClinicalCommentText"), ALERT_CODES("AlertCodes"), 
		ALERTS_OCCURENCE("AlertOccurrence"), EPISODEALERTCODE("EpisodeAlertCode"), SCHEDULEDFOLLOWUP("ScheduledFollowup"),
		SOURCE_CODE("SourceCode"), RC_NOTIFICATION_VALUE("AlertNotifications.AlertNotificationsContent"), EOS_ALERT_PRESENT("EOSAlertPresent"),EPISODE_ONGOING_DISPLAY_FLAG("EpisodeOngoingDisplayFlag"),
		EPISODE_EGM_TYPE("EpisodeEgmType"), TRANSMISSIONTYPE("TransmissionType"), EPISODEEGMDURATION("EpisodeDuration"), EPISODESTATUS("EpisodeStatus"), ZM_START_DATE("ZM.EventCollection.StartDate");
		

		private String rowName;

		private PayloadAttribute(String payloadKey) {
			this.rowName = payloadKey;
		}

		public String getRowName() {
			return this.rowName;
		}
	}
	
	public enum SourceCode {
		REMOTE("R"), INCLINIC("I"), NGQ_MOBILE_APP("N");
		
		private String columnName;

		private SourceCode(String sourcecodeValue) {
			this.columnName = sourcecodeValue;
		}

		public String getName() {
			return this.columnName;
		}
	}

	public PayloadExcelUtilities(FrameworkLog logger) {
		this(System.getProperty("user.dir") + defaultExcelLocation, logger);
	}

	/**
	 * Open excel file for edit
	 * 
	 * @param fileName
	 * @param testReport
	 */
	public PayloadExcelUtilities(String fileName, FrameworkLog logger) {
		log = logger;

		try {
			input = new FileInputStream(fileName);
			wb = new XSSFWorkbook(input);
			sheet = wb.getSheetAt(0);
		} catch (IOException e) {
			String err = "File not found with file-name: " + fileName;
			log.error(err);
			log.printStackTrace(e);
			throw new RuntimeException(err);
		}

	}

	/**
	 * Edit the cell info.
	 */
	public void editCell(HashMap<String, String> cellInfo) {
		for (String key : cellInfo.keySet()) {
			int rowNum = findRow(key);
			Cell cellToUpdate = null;
			cellToUpdate = sheet.getRow(rowNum).getCell(1);
			cellToUpdate.setCellValue(cellInfo.get(key));
		}
	}

	/**
	 * Edit any cell based on row number and cell number passed as parameter
	 */
	public void editCell(String value, int rowNum, int cellNo) {
		Cell cellToUpdate = null;
		cellToUpdate = sheet.getRow(rowNum).getCell(cellNo);
		cellToUpdate.setCellValue(value);
	}

	/**
	 * Close and write data to specified excel apth. Please rpovde full path
	 * 
	 * @param filename
	 */
	public void writeDataToExcel(String filename) {
		try {
			input.close();
			outputStream = new FileOutputStream(filename);
			wb.write(outputStream);
			wb.close();
			outputStream.close();
		} catch (IOException e) {
			String err = "No information found in file: " + filename;
			log.error(err);
			log.printStackTrace(e);
			throw new RuntimeException(err);
		}
	}

	/**
	 * Close the input stream
	 */
	public void closeStream() {
		try {
			input.close();
			wb.close();
		} catch (IOException e) {
			String err = "No file found to close";
			log.error(err);
			log.printStackTrace(e);
			throw new RuntimeException(err);

		}
	}

	/**
	 * Function to get the specified cell value
	 * 
	 * @param attribute - This is the payload attribute which we set from ENUM and
	 *                  it gets the value from the corresponding cell in the payload
	 *                  excel
	 */

	public String getCellValue(String filename, PayloadAttribute attribute) {
		try {
			FileInputStream input1 = new FileInputStream(filename);
			XSSFWorkbook wb1 = new XSSFWorkbook(input1);
			XSSFSheet sheet1 = wb1.getSheetAt(0);
			int rowNum = findRow(attribute.getRowName());
			Cell readCell = null;
			readCell = sheet1.getRow(rowNum).getCell(1);
			input1.close();
			wb1.close();
			return readCell.toString();
		} catch (FileNotFoundException ffe) {
			String err = "File not found with filename: " + filename;
			log.error(err);
			log.printStackTrace(ffe);
			throw new RuntimeException(err);
		} catch (IOException ie) {
			String err = "No information found in file: " + filename;
			log.error(err);
			log.printStackTrace(ie);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * Get cell value for specific cell by passing row number and cell number as
	 * paramter.
	 * 
	 * @implNote We can get cell value either by passing the starting value of row
	 *           or we can get by directly passing the row number. If passing row
	 *           number please remember to pass rowValue as null
	 */
	public String getCellValue(String filename, String rowValue, int rowNumber, int cellNo) {
		int rowNum = -1;
		try {
			FileInputStream input1 = new FileInputStream(filename);
			XSSFWorkbook wb1 = new XSSFWorkbook(input1);
			XSSFSheet sheet1 = wb1.getSheetAt(0);
			if(rowValue==null) {
				rowNum = rowNumber;
			}else {
				rowNum = findRow(rowValue);
			}
			
			Cell readCell = null;
			readCell = sheet1.getRow(rowNum).getCell(cellNo);
			input1.close();
			wb1.close();
			return readCell.toString();
		} catch (FileNotFoundException ffe) {
			String err = "File not found with filename: " + filename;
			log.error(err);
			log.printStackTrace(ffe);
			throw new RuntimeException(err);
		} catch (IOException ie) {
			String err = "No information found in file: " + filename;
			log.error(err);
			log.printStackTrace(ie);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * Get the data of all rows under a particular column passed in as parameter
	 */
	public List<String> getColumnValues(String columnName) {
		List<Cell> cells = new ArrayList<Cell>();
		List<String> cellValues = new ArrayList<>();
		int columnNo = -1;
		Row firstRow = sheet.getRow(0);

		for (Cell cell : firstRow) {
			if (cell.getStringCellValue().equals(columnName)) {
				columnNo = cell.getColumnIndex();
			}
		}

		if (columnNo != -1) {
			for (Row row : sheet) {
				Cell cell = row.getCell(columnNo);
				if (cell == null || cell.getCellType() == CellType.BLANK) {
					// Nothing in the cell in this row, skip it
				} else {
					cells.add(cell);
				}
			}
		}
		
		
		for(Cell cell : cells) {
			cellValues.add(cell.getStringCellValue());
		}
		return cellValues;
	}

	/**
	 * Function to find the row which contains the content passed as parameter
	 * 
	 * @param content
	 * @return
	 */
	public int findRow(String content) {
		for (Row row : sheet) {
			for (Cell cell : row) {
				if (cell.getCellType() == CellType.STRING) {
					if (cell.getRichStringCellValue().getString().trim().equals(content)) {
						return row.getRowNum();
					}
				}
			}
		}
		return -1;
	}

}
