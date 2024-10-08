package com.mnet.reporting.utilities;

import java.util.Map;
import java.util.Set;

import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.FileUtilities;
import com.mnet.reporting.utilities.GraylogReporting.Microservice;

/**
 * Represents a set of logs retrieved from Graylog.
 * Provides capability to save logs to the test logging path.
 * @author Arya Biswas
 * @version Spring 2023
 */
public class GraylogArchive {

	private FrameworkLog log;
	private TestReporter testReport;
	private FileUtilities fileManager;
	private Map<Microservice, String> loggedReports;
	private Set<Microservice> services;
	
	protected GraylogArchive(FrameworkLog frameworkLog, TestReporter testReporter, Map<Microservice, String> graylogReports) {
		log = frameworkLog;
		testReport = testReporter;
		loggedReports = graylogReports;
		
		fileManager = new FileUtilities(log);
		services = graylogReports.keySet();
	}
	
	/**
	 * Write all Graylog reports retrieved to the test logging path
	 * and link to them in the test's Extent Report.*/
	public void writeAll() {
		for (Microservice service : services) {
			String filename = service.toString() + "_" + FrameworkLog.getCurrentTimestamp() + ".csv";

			fileManager.writeToFile(log.getLogDirectory() + filename, loggedReports.get(service));

			testReport.logHeader(ReportLevel.INFO, "<a href='../" + log.getRelativeLogDirectory() + filename + "'>"
					+ "Logs from " + service.toString() + "</a>");
		}
		
		log.info("Graylog microservice logs added to report");
	}
	
	/**
	 * Returns the raw report text associated with the provided microservice (if it exists).
	 * If the microservice log is not associated with this archive, returns null.*/
	public String getReportText(Microservice microservice) {
		return loggedReports.get(microservice);
	}
	
	/**
	 * Returns the raw report text associated with all microservices in this archive.
	 * Recommended to use {@link #getReportText(Microservice)} instead if this archive contains more than one microservice.*/
	public String getReportText() {
		String reportText = "";
		for (Microservice microservice : services) {
			reportText += getReportText(microservice) + "\n";
		}
		
		return reportText;
	}
}
