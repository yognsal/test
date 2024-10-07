package com.mnet.database.utilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mnet.framework.database.DatabaseConnector;
import com.mnet.framework.database.QueryResult;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.middleware.utilities.TantoDriver.TantoTransmissionType;
import com.mnet.pojo.customer.Customer;

/**
 * Utilities for all database calls for customer/ clinic
 * 
 * @author NAIKKX12
 *
 */
public class CustomerDBUtilities extends DBUtilities {
	private TestReporter report;
	private DatabaseConnector database;
	
	public enum CustomerColumn {
		CUSTOMER_ADDRESS_ID("customer_address_id");
		
		private String customerColumn;

		private CustomerColumn(String customerColumn) {
			this.customerColumn = customerColumn;
		}

		public String gecustomerColumns() {
			return this.customerColumn;
		}
	}

	/**Placeholder value in SQL query for device_serial_num*/
	private static final String REPLACE_DEVICE_SERIAL = "<device_serial_num>";
	/**Placeholder value in SQL query for ddt_version_id*/
	private static final String REPLACE_DDT_VERSION = "<ddt_version_id>";
	/**Placeholder value in SQL query for customer_application_id*/
	private static final String REPLACE_CUSTOMER_APPLICATION = "<customer_application_id>";
	
	/**SQL query to fetch ddt_version_id and customer_application_id for given device serial*/
	private static final String SQL_DDT_LOOKUP = 
			"select ddt_version_id, ca.customer_application_id from customers.customer_application ca " +
			"join patients.customer_application_patient cap on ca.customer_application_id = cap.customer_application_id " +
			"join patients.patient_device pd on cap.patient_id = pd.patient_id " +
			"where pd.device_serial_num = '" + REPLACE_DEVICE_SERIAL + "'";
	/**SQL query to set ddt_version_id for given customer_applicaton_id*/
	private static final String SQL_DDT_UPDATE = 
			"update customers.customer_application set ddt_version_id = '" + REPLACE_DDT_VERSION + "'" +
			"where customer_application_id = '" + REPLACE_CUSTOMER_APPLICATION + "'";
	
	private static final String UNITY_DDT_VERSION = "31";
	private static final String MED_DDT_VERSION = "31";
	
	public CustomerDBUtilities(TestReporter testReporter, DatabaseConnector databaseConnector) {
		report = testReporter;
		database = databaseConnector;
		
		
	}

	/**
	 * verify customer exists or deleted; true if created/exists
	 * 
	 * @param customerName
	 * @return
	 */
	public boolean customerExists(String customerName) {
		HashMap<String, String> customerDetails = getCustomer(customerName);
		// NOTE: Name column is 3rd and deleteFlag is 17th column
		String nameColumn = "name", deleteFlag = "deleted_flg";

		if (customerDetails.size() > 0) {
			if (customerDetails.get(nameColumn).equals(customerName) && customerDetails.get(deleteFlag).equals("0")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * verify location created/exists with given name and HQ Flag for mentioned
	 * customer/clinic
	 * 
	 * @param customerName
	 * @param locationName
	 * @param hqFlag
	 * @return
	 */
	public boolean verifyLocation(String customerName, String locationName, String hqFlag) {
		String dbQuery = "select * from customers.customer_location cl where customer_id = '<customer_id>'";

		String customerId = getCustomer(customerName).get("customer_id");
		dbQuery = dbQuery.replace("<customer_id>", customerId);
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);

		List<List<String>> locationDatails = database.executeQuery(dbQuery).getAllRows();

		for (int index = 0; index < locationDatails.size(); index++) {
			// 1 - location Name column index & 3 - HQ Flag column index
			if (locationDatails.get(index).get(1).equals(locationName)
					&& locationDatails.get(index).get(3).equals(hqFlag)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verify webApp & DDT version for given customer
	 * 
	 * @param customerName
	 * @param webAppVersionId
	 * @param idDDT
	 * @return
	 */
	public boolean verifyWebAppDDTVersion(String customerName, String webAppVersionId, String idDDT) {
		String dbQuery = "select webapp_version_id, ddt_version_id from customers.customer_application ca where customer_id = '<customer_id>'";

		String customerId = getCustomer(customerName).get("customer_id");
		dbQuery = dbQuery.replace("<customer_id>", customerId);
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);

		List<String> info = database.executeQuery(dbQuery).getFirstRow();
		if (info.get(0).equals(webAppVersionId) && info.get(1).equals(idDDT)) {
			return true;
		}
		return false;
	}

	public boolean verifyCustomerDetails(Customer customer) {
		boolean success = true;

		HashMap<String, String> customerDetails = getCustomer(customer.getCustomerName());

		if (!customerDetails.get("name").equals(customer.getCustomerName())) {
			report.logStep(ReportLevel.ERROR,
					"Customer name - " + customer.getCustomerName() + " does not match in database record");
			success = false;
		}

		String dbQuery = "select code_desc from lookup.code where code_id = '" + customerDetails.get("customer_type_cd")
				+ "'";
		report.logStep(ReportLevel.INFO, "Executing database query to extract customer type: " + dbQuery);
		QueryResult queryResult = database.executeQuery(dbQuery);
		
		if (!queryResult.getFirstRow().get(0).equals(customer.getCustomerType().getCustType())) {
			report.logStep(ReportLevel.ERROR, "Customer Type - " + customer.getCustomerType().getCustType()
					+ " does not match in database record");
			success = false;
		}

		return success;
	}

	/**
	 * Read customer details from database based on customer name provided
	 */
	public HashMap<String, String> getCustomer(String customerName) {
		HashMap<String, String> details = new HashMap<>();
		String dbQuery = "select * from customers.customer where name = '<customer_name>'";
		dbQuery = dbQuery.replace("<customer_name>", customerName);
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = database.executeQuery(dbQuery);
		
		List<String> values = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			details.put(columnNames.get(index), values.get(index));
		}
		return details;
	}
	
	/**
	 * Read customer address details from database
	 */
	public List<Map<String, String>> getCustomerAddressDetails(String filterColumn, String filterValue) {
		String query = "select * from customers.customer_address";
		return getDBContents(report, database, query, filterColumn, filterValue);
	}
	
	/**
	 * Read Customer Location Details from database based on column name and its value provided
	 */
	public HashMap<String, String> getCustomerLocationDetails(String columnName, String value) {
		HashMap<String, String> details = new HashMap<>();
		String dbQuery = "select * from customers.customer_location cl where " + columnName + " = '" + value + "';";
		
		report.logStep(ReportLevel.INFO, "Executing database query: " + dbQuery);
		QueryResult queryResult = database.executeQuery(dbQuery);
		
		List<String> values = queryResult.getFirstRow();
		List<String> columnNames = queryResult.getColumnNames();

		for (int index = 0; index < columnNames.size(); index++) {
			details.put(columnNames.get(index), values.get(index));
		}
		return details;
	}

	/**
	 * Get devices based on filter if provided
	 */
	public List<Map<String, String>> getDevices(String filterColumn, String filterValue) {
		String query = "select * from devices.jurisdiction_device";
		return getDBContents(report, database, query, filterColumn, filterValue);
	}
	
	/**
	 * Get alert groups based on filter if provided
	 */
	public List<Map<String, String>> getAlertGroup(String filterColumn, String filterValue) {
		String query = "select * from alerts.alert_group";
		return getDBContents(report, database, query, filterColumn, filterValue);
	}
	
	/**
	 * Get customer applications based on filter if provided
	 */
	public List<Map<String, String>> getCustomerApplication(String filterColumn, String filterValue) {
		String query = "select * from customers.customer_application";
		return getDBContents(report, database, query, filterColumn, filterValue);
	}
	
	public void setDDTVersion(TantoTransmissionType transmissionType, String deviceSerial) {
		String ddtLookupQuery = SQL_DDT_LOOKUP.replace(REPLACE_DEVICE_SERIAL, deviceSerial);
		
		String ddtVersion = (transmissionType == TantoTransmissionType.MED) ? MED_DDT_VERSION : UNITY_DDT_VERSION;
		
		report.logStep(ReportLevel.INFO, "Running database lookup query: <textarea>" + ddtLookupQuery + " </textarea>");
		
		List<String> queryResult = database.executeQuery(ddtLookupQuery).getFirstRow();
		
		String currentDDTVersion = queryResult.get(0);
		
		if (currentDDTVersion.equals(ddtVersion)) {
			report.logStep(ReportLevel.INFO, "Clinic ddt_version_id is already set to: " + ddtVersion);
			return;
		}
		
		String customerApplicationId = queryResult.get(1);
		
		report.logStep(ReportLevel.INFO, "Setting clinic ddt_version_id = " + ddtVersion);
		String ddtUpdateQuery = 
				SQL_DDT_UPDATE.replace(REPLACE_DDT_VERSION, ddtVersion).replace(REPLACE_CUSTOMER_APPLICATION, customerApplicationId);
		
		report.logStep(ReportLevel.INFO, "Running database update statement: <textarea>" + ddtUpdateQuery + "</textarea>");
		database.executeUpdate(ddtUpdateQuery);
	}
	
	/**
	 * Get customer address based on filter if provided
	 */
	public List<Map<String, String>> getCustomerAddress(CustomerColumn customerColumns, String filterValue) {
		String query = "select * from customers.customer_address";
	    return getDBContents(report, database, query, customerColumns.gecustomerColumns(), filterValue);
	}
}