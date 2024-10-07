package com.mnet.database.utilities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;

/**
 * Utilities for all common/generic database calls; not specific to any entity (customer or patient or user and so on)
 * @author NAIKKX12
 *
 */
public class CommonDBUtilities extends DBUtilities {
	private TestReporter report;
	private DatabaseConnector database;
	
	public enum CodeQualifier {
		Country("Country_Cd"), LegalJurisdiction("Legal_Jurisdiction_Cd"), TimeZone("Time_Zone_Cd"), Language("Language_Cd"), ContactType("Contact_Type_Cd"), Locale("Locale_Cd"),
		TelemetryType("Telemetry_Type_Cd"), RoutingPriority("Routing_Priority_Cd");

		private String codeQualifier;

		private CodeQualifier(String codeQualifier) {
			this.codeQualifier = codeQualifier;
		}

		public String getCodeQualifier() {
			return this.codeQualifier;
		}
	}
	
	public enum LookupCountryTableColumn{
		Country_Name("country_name"), Country_Abbr("country_abbr");
		
		private String tableColumn;

		private LookupCountryTableColumn(String tableColumn) {
			this.tableColumn = tableColumn;
		}

		public String getTableColumn() {
			return this.tableColumn;
		}
	}
	
	public CommonDBUtilities(TestReporter testReporter, DatabaseConnector databaseConnector) {
		report = testReporter;
		database = databaseConnector;
	}
	
	/**
	 * enum for region - UnitedStates is ROW whereas ALL - ignore region
	 * @author NAIKKX12
	 *
	 */
	public enum Region {
		ALL, ROW, EU}
	
	/**
	 * Update MFA email address
	 * @param email - new email to update with
	 * @param altEmail - old email to update
	 */
	public void updateMFA(String email, String altEmail) {
        String dbQuery = "update users.customer_account set mfa_email_address = '<email>' where mfa_email_address = '<default_email>'";
        dbQuery = dbQuery.replace("<email>", email.toLowerCase());
        dbQuery = dbQuery.replace("<default_email>", altEmail.toLowerCase());
        report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
        database.executeUpdate(dbQuery);
    }
	
	/**
	 * Update MFA email address with random email id
	 * @param altEmail - old email to update
	 */
	public void updateMFA(String altEmail) {
        updateMFA(CommonUtils.generateRandomEmail(), altEmail);
    }
	
	/**
	 * Update primary email address
	 */
	public void updatePrimaryEmail(String updatedEmail, String oldEmail) {
        String dbQuery = "update users.customer_account set email_address = '<updatedEmail>' where email_address = '<oldEmail>'";
        dbQuery = dbQuery.replace("<updatedEmail>", updatedEmail.toLowerCase());
        dbQuery = dbQuery.replace("<oldEmail>", oldEmail.toLowerCase());
        report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
        database.executeUpdate(dbQuery);
    }
	
	/**
	 * Update primary email address with random email id
	 */
	public void updatePrimaryEmail(String oldEmail) {
		updatePrimaryEmail(CommonUtils.generateRandomEmail(), oldEmail);
    }
	
	/**
	 * Return region specific country list
	 * Deprecated; Use getRegionBasedCountryList(Region region, LookupCountryTableColumn countryTableColumn)
	 */
	@Deprecated
	public List<String> getRegionbasedCountryList(Region region){
		String dbQuery;
		
		switch (region) {
		case ALL:
			dbQuery = "select * from lookup.country";
			break;
		case ROW:
		case EU:
			dbQuery = "select * from lookup.country c where data_sovereignty_region_id = '" + region.ordinal() + "'";
			break;
		default:
			report.logStep(ReportLevel.ERROR, region + " region not handled");
			return null;
		}
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		
		// Replacing Great Britain with United Kingdom as per discussion with developer
		// (SWR - PCN00035685)
		List<String> countries = database.executeQuery(dbQuery).getColumn("country_name");
		for(int i = 0; i < countries.size(); i++) {
			if(countries.get(i).equals("Great Britain")) {
				countries.remove(i);
				countries.add("United Kingdom");
				Collections.sort(countries);
			}
		}
		
		report.logStep(ReportLevel.INFO, "Country list in database: " + countries);
		
		return countries;
	}
	
	/**
	 * Return region specific country code list
	 */
	public List<String> getRegionBasedCountryList(Region region, LookupCountryTableColumn countryTableColumn) {
		String dbQuery;

		switch (region) {
		case ALL:
			dbQuery = "select * from lookup.country";
			break;
		case ROW:
		case EU:
			dbQuery = "select * from lookup.country c where data_sovereignty_region_id = '" + region.ordinal() + "'";
			break;
		default:
			report.logStep(ReportLevel.ERROR, region + " region not handled");
			return null;
		}
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);

		List<String> countries = database.executeQuery(dbQuery).getColumn(countryTableColumn.getTableColumn());
		
		// Replacing Great Britain with United Kingdom as per discussion with developer
		// (SWR - PCN00035685)
		if(countryTableColumn == LookupCountryTableColumn.Country_Name) {
			for(int i = 0; i < countries.size(); i++) {
				if(countries.get(i).equals("Great Britain")) {
					countries.remove(i);
					countries.add("United Kingdom");
					Collections.sort(countries);
				}
			}
		}

		report.logStep(ReportLevel.INFO, "Country list in database based on "+countryTableColumn.getTableColumn()+" column : " + countries);

		return countries;

	}
	
	/**
	 * Return the country list from lookup table
	 */
	public List<String> getCountryList() {
		List<String> countries = getDataForLookupCode("Country_Cd");
		report.logStep(ReportLevel.INFO, "Country list in database: " + countries);
		return countries;
	}
	
	/**
	 * Return list of languages form lookup table
	 */
	public List<String> getLanguageList() {
		List<Map<String, String>> data = getDataForLookupCode(CodeQualifier.Language);
		List<String> contents = new ArrayList<>();
				
		for (Map<String, String> map : data) {
			contents.add(map.get("code_desc"));
		}
		report.logStep(ReportLevel.INFO, "Language list in database: " + contents);
		return contents;
	}
	
	/**
	 * Get all available holidays from the database
	 */
	public List<Map<String, String>> getHolidays() {
		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		Map<String, String> row = new HashMap<String, String>();
		String dbQuery = "select * from lookup.holiday h";
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = database.executeQuery(dbQuery);

		List<List<String>> holidays = queryResult.getAllRows();
		List<String> columnNames = queryResult.getColumnNames();
		
		for (int rowIndex = 0; rowIndex < holidays.size(); rowIndex++) {
			row = new HashMap<String, String>();
			for (int colIndex= 0; colIndex < columnNames.size(); colIndex++) {
				row.put(columnNames.get(colIndex), holidays.get(rowIndex).get(colIndex));
			}
			content.add(row);
		}
		return content;
	}
	
	/**
	 * Return the info based on code qualifier from lookup table
	 */
	@Deprecated
	// use getDataForLookupCode(CodeQualifier codeQualifier) instead
	private List<String> getDataForLookupCode(String codeQualifier) {
		String query = "select * from lookup.code c where code_qualifier like '%" + codeQualifier + "%';";
		report.logStep(ReportLevel.INFO, "Executing database query: " + query);
		
		return database.executeQuery(query).getColumn("code_desc");
	}
	
	/**
	 * Return the info based on code qualifier from lookup table
	 */
	public List<Map<String, String>> getDataForLookupCode(CodeQualifier codeQualifier) {
		List<Map<String, String>> content = new ArrayList<Map<String, String>>();
		Map<String, String> row = new HashMap<String, String>();
		
		String query = "select * from lookup.code c where code_qualifier = '" + codeQualifier.getCodeQualifier() + "';";
		report.logStep(ReportLevel.INFO, "Executing database query: " + query);
		
		QueryResult queryResult = database.executeQuery(query);

		List<List<String>> allRows = queryResult.getAllRows();
		List<String> columnNames = queryResult.getColumnNames();
		
		for (int rowIndex = 0; rowIndex < allRows.size(); rowIndex++) {
			row = new HashMap<String, String>();
			for (int colIndex= 0; colIndex < columnNames.size(); colIndex++) {
				row.put(columnNames.get(colIndex), allRows.get(rowIndex).get(colIndex));
			}
			content.add(row);
		}
		return content;
	}
	
	/**
	 * Return the code description for the passed code
	 */
	public String getCodeDescription(String code) {
		String query = "select code_desc from lookup.code c where code_id='<code_id>'";
		query = query.replace("<code_id>", code);
		report.logStep(ReportLevel.INFO, "Executing database query: " + query);
		
		return database.executeQuery(query).getFirstCellValue();
	}
	
	/**
	 * Return all details based on the code id whichs is passed in as parameter
	 */
	public List<Map<String, String>> getCodeDetails(String code) {
		String query = "select * from lookup.code c where code_id='<code_id>'";
		query = query.replace("<code_id>", code);
		report.logStep(ReportLevel.INFO, "Executing database query: " + query);
		
		return DBUtilities.getDBContents(report, database, query);
	}
	
	/**
	 * Method to wait after events where back dating in database is required. As per
	 * the information provided, database updates itself after every quater.
	 */
	public void asaTimer() {
		report.logStep(ReportLevel.INFO, "Running ASA Timer to update database because of backdate");
		int currentMinute = LocalDateTime.now().getMinute();
		try {
			if(0 <= currentMinute && currentMinute < 15) {
				report.logStep(ReportLevel.INFO, "Nearest exit for ASA timer is 15th Minute");
				Thread.sleep((15-currentMinute)*60000);
			}
			if(15 <= currentMinute && currentMinute < 30) {
				report.logStep(ReportLevel.INFO, "Nearest exit for ASA timer is 30th Minute");
				Thread.sleep((30-currentMinute)*60000);
			}
			if(30 <= currentMinute && currentMinute < 45) {
				report.logStep(ReportLevel.INFO, "Nearest exit for ASA timer is 45th Minute");
				Thread.sleep((45-currentMinute)*60000);
			}
			if(45 <= currentMinute && currentMinute <= 59) {
				report.logStep(ReportLevel.INFO, "Nearest exit for ASA timer is 00th Minute");
				//Adding addtional minute to match with 00th minute
				Thread.sleep((59-currentMinute)*60000+60000);
			}
		} catch (InterruptedException e) {
			String err = "Error occurred during sleep";
			report.logStep(ReportLevel.ERROR, err);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * Get software product based on filter if provided
	 */
	public List<Map<String, String>> getSoftwareVersions(String filterColumn, String filterValue) {
		String query = "select * from system.software_product_version";
		return getDBContents(report, database, query, filterColumn, filterValue);
	}
	
	/** Get any overridden database filed value */
	public String getDBOverride(String dbField, String deviceSerial) {
		
		String dbValue = getTransmitterOverride(dbField, deviceSerial);	
		if (dbValue != null) {
			return dbValue;
		}
		
		dbValue = getDeviceOverride(dbField, deviceSerial);		
		if (dbValue != null) {
			return dbValue;
		}
		
		dbValue = getPatientOverride(dbField, deviceSerial);		
		if (dbValue != null) {
			return dbValue;
		}
		
		return getClinicOverride(dbField, deviceSerial);
	}
	
	/** Queries database for transmitter-level database value */
	private String getTransmitterOverride(String dbField, String deviceSerial) {
		return database.executeQuery("select pts.switch_value" +
                " from patients.patient_transmitter_switch pts" +
                " join extinstruments.transmitter_profile_switch tps on tps.transmitter_profile_switch_id = pts.transmitter_profile_switch_id" +
                " join patients.patient ptnt on ptnt.patient_id = pts.patient_id" +
                " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and tps.switch_name = '" + dbField + "'").getFirstCellValue();
	}
	
	/** Queries database for device-level database value */
	private String getDeviceOverride(String dbField, String deviceSerial) {
		return database.executeQuery("select dps.device_switch_default_value" +
                " from extinstruments.device_product_switch dps" +
                " join extinstruments.transmitter_profile_switch tps on dps.transmitter_profile_switch_id = tps.transmitter_profile_switch_id" +
                " join devices.device_product dp on dps.device_product_id = dp.device_product_id" +
                " join patients.patient_device pd on dp.device_product_id = pd.device_product_id" +
                " join patients.patient ptnt on pd.patient_id = ptnt.patient_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'" +
                " and tps.switch_name = '" + dbField + "'").getFirstCellValue();
	}
	
	/** Queries database for patient-level database value */
	private String getPatientOverride(String dbField, String deviceSerial) {
		return database.executeQuery("select ptnt." + dbField + 
                " from patients.patient ptnt" +
                " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'")
				.getFirstCellValue();
	}
	
	/** Queries database for clinic-level database value */
	private String getClinicOverride(String dbField, String deviceSerial) {
		return database.executeQuery("select cust." + dbField +
				" from customers.customer cust" +
                " join customers.customer_application ca on cust.customer_id = ca.customer_id" +
                " join patients.customer_application_patient cap on cap.customer_application_id = ca.customer_application_id" +
                " join patients.patient ptnt on ptnt.patient_id = cap.patient_id" +
                " join patients.patient_device pd on pd.patient_id = ptnt.patient_id" +
                " join devices.device_product dp on dp.device_product_id = pd.device_product_id" +
                " where pd.device_serial_num = '" + deviceSerial + "'").getFirstCellValue();
	}
}