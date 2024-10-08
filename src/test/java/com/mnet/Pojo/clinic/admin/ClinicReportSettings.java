package com.mnet.pojo.clinic.admin;

/**
 * pojo class for Clinic Administration -> Report Settings
 * 
 * @author NAIKKX12
 *
 */
public class ClinicReportSettings {

	private boolean allAlertEpisodes;
	private boolean directTrendReports;
	private boolean wrapUpOverview;

	public ClinicReportSettings(boolean setDefaultValues) {
		if (setDefaultValues) {
			// TODO: Set default values to possible class variables
		}
	}

	public boolean isAllAlertEpisodes() {
		return allAlertEpisodes;
	}

	public void setAllAlertEpisodes(boolean allAlertEpisodes) {
		this.allAlertEpisodes = allAlertEpisodes;
	}

	public boolean isDirectTrendReports() {
		return directTrendReports;
	}

	public void setDirectTrendReports(boolean directTrendReports) {
		this.directTrendReports = directTrendReports;
	}

	public boolean isWrapUpOverview() {
		return wrapUpOverview;
	}

	public void setWrapUpOverview(boolean wrapUpOverview) {
		this.wrapUpOverview = wrapUpOverview;
	}
}