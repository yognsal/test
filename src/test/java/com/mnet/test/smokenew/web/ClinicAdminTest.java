
package com.mnet.test.smoke.web;

import java.util.ArrayList;
import java.util.List;

import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.mnet.database.utilities.CommonDBUtilities;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.TestReporter.AssertionLevel;
import com.mnet.framework.reporting.TestReporter.ReportLevel;
import com.mnet.framework.utilities.CommonUtils;
import com.mnet.pojo.clinic.admin.ClinicAdminProfile;
import com.mnet.pojo.clinic.admin.ClinicExportOptions;
import com.mnet.pojo.clinic.admin.ClinicHoliday;
import com.mnet.pojo.clinic.admin.ClinicReportSettings;
import com.mnet.pojo.clinic.admin.ClinicScheduleMsg.SchedulingMethod;
import com.mnet.pojo.clinic.admin.ClinicUser;
import com.mnet.pojo.clinic.admin.ClinicUser.UserType;
import com.mnet.pojo.clinic.admin.DirectAlertHomeTransmitter;
import com.mnet.pojo.customer.Customer;
import com.mnet.reporting.utilities.GraylogReporting;
import com.mnet.webapp.pageobjects.clinic.ClinicNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicAdminNavigationBar;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicCommentsPage;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicExportOptionsPage;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicHoursHolidaysPage;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicLocationsPage;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicProfilePage;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicReportSettingsPage;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicScheduleMessagingPage;
import com.mnet.webapp.pageobjects.clinic.administration.ClinicUsersPage;
import com.mnet.webapp.pageobjects.clinic.administration.DirectAlertMerlinAtHomePage;
import com.mnet.webapp.utilities.CustomerUtilities;
import com.mnet.webapp.utilities.LoginUtilities;

/**
 * 
 * Sanity testcase - Edit and verify clinic administration setting (all tabs)
 * 
 * @author NAIKKX12
 *
 */
public class ClinicAdminTest extends MITETest implements GraylogReporting {

	private ClinicNavigationBar clinicNavigationBar;
	private ClinicAdminNavigationBar clinicAdminNavigationBar;
	private ClinicProfilePage clinicProfilePage;
	private ClinicScheduleMessagingPage clinicScheduleMessagingPage;
	private ClinicHoursHolidaysPage clinicHoursHolidayPage;
	private DirectAlertMerlinAtHomePage homeTransmitterPage;
	private ClinicReportSettingsPage clinicReportSettingPage;
	private ClinicExportOptionsPage clinicExportOptionsPage;
	private ClinicCommentsPage clinicalCommentsPage;
	private ClinicUsersPage clinicUsersPage;
	private ClinicLocationsPage clinicLocationsPage;
	private Customer customerInfo;
	private LoginUtilities loginUtils;
	private CommonDBUtilities commonDBUtils;
	private CustomerUtilities customerUtils;

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.WEBAPP);
		attributes.add(TestAttribute.EMAIL);
		attributes.add(TestAttribute.DATABASE);
		super.initialize(context);

		// Initialize test-specific page objects
		clinicNavigationBar = new ClinicNavigationBar(webDriver, report);
		clinicAdminNavigationBar = new ClinicAdminNavigationBar(webDriver, report);
		clinicProfilePage = new ClinicProfilePage(webDriver, report);
		clinicScheduleMessagingPage = new ClinicScheduleMessagingPage(webDriver, report);
		clinicHoursHolidayPage = new ClinicHoursHolidaysPage(webDriver, report);
		homeTransmitterPage = new DirectAlertMerlinAtHomePage(webDriver, report);
		clinicReportSettingPage = new ClinicReportSettingsPage(webDriver, report);
		clinicExportOptionsPage = new ClinicExportOptionsPage(webDriver, report);
		clinicalCommentsPage = new ClinicCommentsPage(webDriver, report);
		clinicUsersPage = new ClinicUsersPage(webDriver, report);
		clinicLocationsPage = new ClinicLocationsPage(webDriver, report);
		loginUtils = new LoginUtilities(report, email, webDriver);
		commonDBUtils = new CommonDBUtilities(report, database);
		customerUtils = new CustomerUtilities(report, webDriver);
	}

	@Override
	@BeforeMethod
	public void setup(Object[] parameters) {
		super.setup(parameters);

		customerInfo = new Customer();
		Customer customer = manualCustomerSetup ? customerUtils.manualCustomerSetupAndLogin(log, email, 1, true)
				: customerUtils.automatedCustomerSetupAndLogin(customerInfo, email, true);
		if(customer == null) {
			report.logStep(ReportLevel.FAIL, "Failed to authorize application");
		}
	}

	@Test
	public void clinicAdministration() {
		
		if(!clinicNavigationBar.validate()) {
			report.logStepWithScreenshot(ReportLevel.FAIL, "Failed to load navigation bar");
		}
		
		clinicNavigationBar.viewClinicAdministrationPage();
		report.assertWithScreenshot(clinicAdminNavigationBar.validate(), true,
				"Navigated and loaded Clinic Administration webpage succesfully.",
				"Failed to navigate and load Clinic Administration page");

		boolean result = true;

		// Navigate, edit and verify clinic profile page page
		clinicAdminNavigationBar.viewClinicProfilePage(true);
		result = clinicProfilePage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true, "Loaded Clinic Profile page succesfully.",
				"Failed to navigate and load Clinic Profile page");
		if (result) {
			// Edit Clinic profile setting
			ClinicAdminProfile clinicProfile = new ClinicAdminProfile();
			clinicProfile.setLocation(CommonUtils.randomAlphanumericString(15));
			clinicProfile.setSecurityStamp(CommonUtils.randomAlphanumericString(6));
			clinicProfile.setMainPhone_CityCode(String.valueOf(CommonUtils.getRandomNumber(10, 99)));
			if (clinicProfilePage.editClinicProfile(true)) {
				clinicProfilePage.editClinicDetails(null, clinicProfile.getLocation(), null, null);
				report.logStepWithScreenshot(ReportLevel.INFO, "Edited clinic profile -> details");
				clinicProfilePage.editMainPhone(null, clinicProfile.getMainPhone_CityCode(),
						clinicProfile.getMainPhone_Number());
				report.logStepWithScreenshot(ReportLevel.INFO, "Edited clinic profile -> Main Phone");
				clinicProfilePage.editSecurityStamp(clinicProfile.getSecurityStamp());
				report.logStepWithScreenshot(ReportLevel.INFO, "Edited clinic profile -> Security Stamp");
				clinicProfilePage.saveClinicProfile();
				if (clinicProfilePage.isChangeSaved()) {
					result = clinicProfilePage.verifyClinicDetails(null, clinicProfile.getLocation(), null, null)
							&& clinicProfilePage.verifyMainPhone(null, clinicProfile.getMainPhone_CityCode(),
									clinicProfile.getMainPhone_Number())
							&& clinicProfilePage.verifySecurityStamp(clinicProfile.getSecurityStamp());
					report.assertWithScreenshot(AssertionLevel.SOFT, result, true,
							"Allow to update clinic profile page successfully", "Failed to update clinic profile page");
				} else {
					if (clinicProfilePage.IsRandomPopupDisplayed()) {
						report.logStepWithScreenshot(ReportLevel.ERROR,
								"Unexpected error/ behavior while saving clinic profile");
					}
					report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
							"Clinic Profile page changes verified and successful",
							"FAILED: Clinic profile verification");
				}
			}
		}
		// Navigate, edit and verify clinic scheduling & Messaging page
		result = true;
		clinicAdminNavigationBar.viewClinicScheduleMessagingPage();
		result = clinicScheduleMessagingPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true,
				"Loaded Scheduling & Messaging page succesfully.",
				"Failed to navigate and load Scheduling & Messaging page");
		if (result) {
			if (clinicScheduleMessagingPage.editSchedulingMessagingSetting(true)) {
				clinicScheduleMessagingPage.selectSchedulingMethod(SchedulingMethod.SMART);
				clinicScheduleMessagingPage.setTransmitterSetting(true, true, false, false);
				clinicScheduleMessagingPage.saveSchedulingMessagingPage();
				clinicScheduleMessagingPage.waitMessagePopup();
				clinicScheduleMessagingPage.confirmSettingChanges();
				clinicScheduleMessagingPage.waitMessagePopup();
				clinicScheduleMessagingPage.confirmSettingChanges();
				clinicScheduleMessagingPage.waitMessagePopup();
				clinicScheduleMessagingPage.confirmSettingChanges();
				if (clinicScheduleMessagingPage.isChangeSaved()) {
					result = clinicScheduleMessagingPage.verifySchedulingMethod(SchedulingMethod.SMART)
							&& clinicScheduleMessagingPage.verifyTransmitterSetting(true, true, false, false);
					report.assertWithScreenshot(AssertionLevel.SOFT, result, true,
							"Allow to update Scheduling & Messaging page successfully",
							"Failed to update Scheduling & Messaging page");
				} else {
					if (clinicScheduleMessagingPage.IsRandomPopupDisplayed()) {
						report.logStepWithScreenshot(ReportLevel.ERROR,
								"Unexpected error/ behavior while saving scheduling & messaging page changes");
					}
					report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
							"Scheduling & Messaging page changes verified and successful",
							"FAILED: Scheduling & Messaging verification");
				}
			}
		}
		// Navigate, edit & verify clinic hours and holiday page
		result = true;
		clinicAdminNavigationBar.viewClinicHoursHolidayPage();
		result = clinicHoursHolidayPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true,
				"Loaded Clinic hours / holidays page succesfully.",
				"Failed to navigate and load the page : Clinic hours / holidays");
		if (result) {
			ClinicHoliday holiday = new ClinicHoliday();
			if (clinicHoursHolidayPage.editClinicHoursHolidayPage(true)) {
				clinicHoursHolidayPage.addHoliday(holiday);
				clinicHoursHolidayPage.clickAddHolidayLink();
				clinicHoursHolidayPage.saveHoursHolidayPage();
				clinicHoursHolidayPage.waitMessagePopup();
				clinicHoursHolidayPage.confirmMessage();
				clinicHoursHolidayPage.waitMessagePopup();
				clinicHoursHolidayPage.confirmMessage();
				if (clinicHoursHolidayPage.isChangeSaved()) {
					report.assertWithScreenshot(AssertionLevel.SOFT, clinicHoursHolidayPage.verifyHoliday(holiday),
							true, "Allow to Clinic hours / holidays page successfully",
							"Failed to update clinic hours / holidays page");
				} else {
					if (clinicHoursHolidayPage.IsRandomPopupDisplayed()) {
						report.logStepWithScreenshot(ReportLevel.ERROR,
								"Unexpected error/ behavior while saving Hours/ Holiday page changes");
					}
					report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
							"Clinic Hours/Holidays page changes verified and successful",
							"FAILED: Clinic Hours/Holidays verification");
				}
			}
		}

		// Navigate, edit & verify Home Transmitter page
		result = true;
		clinicAdminNavigationBar.viewClinicHomeTransmitterPage();
		result = homeTransmitterPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true,
				"Loaded Merlin@Home Transmitter page succesfully", "Failed to load Merlin@Home Transmitter page");
		if (result) {
			DirectAlertHomeTransmitter homeTransmitter = new DirectAlertHomeTransmitter();
			homeTransmitter.setContactMainPhoneAreaCode(String.valueOf(CommonUtils.getRandomNumber(100, 999)));
			homeTransmitter.setContactTextMsg(CommonUtils.generateRandomEmail());
			if (homeTransmitterPage.editHomeTrasmitter(true)) {
				homeTransmitterPage.editContactMessageAndEmail(homeTransmitter.getContactTextMsg(), null);
				homeTransmitterPage.editContactMainPhone(null, homeTransmitter.getContactMainPhoneAreaCode(), null);
				homeTransmitterPage.saveHomeTrasmitter();
				homeTransmitterPage.waitForConfirmPopup();
				homeTransmitterPage.confirmSettingChanges();
				homeTransmitterPage.waitForConfirmPopup();
				homeTransmitterPage.informSettingChanges();
				if (homeTransmitterPage.isChangeSaved()) {
					result = homeTransmitterPage.verifyContactMainPhone(null,
							homeTransmitter.getContactMainPhoneAreaCode(), null)
							&& homeTransmitterPage.verifyContactMessageAndEmail(homeTransmitter.getContactTextMsg(),
									null);
					report.assertWithScreenshot(AssertionLevel.SOFT, result, true,
							"Allow to update Merlin@Home Transmitter page successfully",
							"Failed to update Merlin@Home Transmitter page");
				} else {
					if (homeTransmitterPage.IsRandomPopupDisplayed()) {
						report.logStepWithScreenshot(ReportLevel.ERROR,
								"Unexpected error/ behavior while saving Merlin@Home Transmitter page changes");
					}
					report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
							"Merlin@Home Transmitter page changes verified and successful",
							"FAILED: Merlin@Home Transmitter verification");
				}
			}
		}
		// Navigate, edit & verify Report Setting page
		result = true;
		clinicAdminNavigationBar.viewClinicReportSettingsPage();
		result = clinicReportSettingPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true, "Loaded Report Settings page succesfully.",
				"Failed to navigate and load the page : Report Settings");
		if (result) {
			ClinicReportSettings reportSettings = new ClinicReportSettings(false);
			reportSettings.setDirectTrendReports(true);
			reportSettings.setWrapUpOverview(true);
			if (clinicReportSettingPage.editReportSetting(true)) {
				clinicReportSettingPage.editAllDiagnostics(false, false, false, false, false, false, true);
				clinicReportSettingPage.editWrapupOverivew(true);
				clinicReportSettingPage.saveReportSettingsPage();
				clinicReportSettingPage.waitMessagePopup();
				clinicReportSettingPage.confirmMessage();
				clinicReportSettingPage.waitMessagePopup();
				clinicReportSettingPage.confirmMessage();
			}
			if (clinicReportSettingPage.isChangeSaved()) {
				result = clinicReportSettingPage.verifyAllDiagnostics(false, false, false, false, false, false, true)
						&& clinicReportSettingPage.verifyWrapupOverivew(true);
				report.assertWithScreenshot(AssertionLevel.SOFT, result, true,
						"Updated Report Settings page successfully", "Failed to update Report Settings page");
			} else {
				if (clinicReportSettingPage.IsRandomPopupDisplayed()) {
					report.logStepWithScreenshot(ReportLevel.ERROR,
							"Unexpected error/ behavior while saving Report Settiing page changes");
				}
				report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
						"Report Settings page changes verified and successful", "FAILED: Report Settings verification");
			}
		}
		// Navigate, edit & verify Export Options page
		result = true;
		clinicAdminNavigationBar.viewClinicExportOptionsPage();
		result = clinicExportOptionsPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true, "Loaded Export Options page succesfully.",
				"Failed to navigate and load Export Options page");
		if (result) {
			ClinicExportOptions exportOptions = new ClinicExportOptions();
			exportOptions.setSetupPCData(true);
			if (clinicExportOptionsPage.editClinicExportOptions(true)) {
				clinicExportOptionsPage.editExportToPC(exportOptions.isSetupPCData());
				clinicExportOptionsPage.saveExportOptionsPage();
				clinicExportOptionsPage.waitMessagePopup();
				clinicExportOptionsPage.confirmMessage();
				clinicExportOptionsPage.waitMessagePopup();
				clinicExportOptionsPage.confirmMessage();
			}
			if (clinicExportOptionsPage.isChangeSaved()) {
				report.assertWithScreenshot(AssertionLevel.SOFT,
						clinicExportOptionsPage.verifyExportToPC(exportOptions.isSetupPCData()), true,
						"Allow to update Export Options page", "Failed to update export options page");
			} else {
				if (clinicExportOptionsPage.IsRandomPopupDisplayed()) {
					report.logStepWithScreenshot(ReportLevel.ERROR,
							"Unexpected error/ behavior while saving export options page");
				}
				report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
						"Export Options page changes verified and successful", "FAILED: Export Options verification");
			}
		}
		// Navigate, edit and verify clinical comments page
		result = true;
		clinicAdminNavigationBar.viewClinicCommentsPage();
		result = clinicalCommentsPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true, "Loaded Clinical Comments page succesfully.",
				"Failed to navigate and load Clinical Comments page");
		if (result) {
			if (clinicalCommentsPage.editClinicalCommentsPage(true)) {
				clinicalCommentsPage.editFreeFormTextCheck(true);
				clinicalCommentsPage.editPresetCommentsCheck(false);
				clinicalCommentsPage.saveClinicalCommentsPage();
				clinicalCommentsPage.waitMessagePopup();
				clinicalCommentsPage.confirmMessage();
				clinicalCommentsPage.waitMessagePopup();
				clinicalCommentsPage.confirmMessage();
			}
			if (clinicalCommentsPage.isChangeSaved()) {
				report.assertWithScreenshot(AssertionLevel.SOFT,
						clinicalCommentsPage.verifyFreeFormTextCheck(true)
								&& clinicalCommentsPage.verifyPresetCommentsCheck(false),
						true, "Update to Clinical Comments page are successful",
						"Failed to update clinical comments page");
			} else {
				if (clinicalCommentsPage.IsRandomPopupDisplayed()) {
					report.logStepWithScreenshot(ReportLevel.ERROR,
							"Unexpected error/ behavior while saving clinical comments page changes");
					report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
							"Clinical Comments page changes verified and successful",
							"FAILED: Clinical Comments verification");
				}
			}
		}
		// Navigate, edit and verify Clinic Users page
		result = true;
		clinicAdminNavigationBar.viewClinicUsersPage();
		result = clinicUsersPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true, "Loaded Clinic Users page succesfully.",
				"Failed to navigate and load Clinic Users page");
		if (result) {
			ArrayList<ClinicUser> usersToAdd = new ArrayList<ClinicUser>();
			ClinicUser user = new ClinicUser();
			usersToAdd.add(user);
			user = new ClinicUser();
			user.setUserType(UserType.ALLIED_PROFESSIONAL);
			usersToAdd.add(user);
			user = new ClinicUser();
			user.setUserType(UserType.ASSISTANT);
			usersToAdd.add(user);
			clinicUsersPage.addUsers(usersToAdd);
			if (clinicUsersPage.isChangeSaved()) {
				report.assertWithScreenshot(AssertionLevel.SOFT, clinicUsersPage.verifyUsers(usersToAdd), true,
						"Update to Clinic Users page are successful", "Failed to update clinic Users page");
			} else {
				if (clinicLocationsPage.IsRandomPopupDisplayed()) {
					report.logStepWithScreenshot(ReportLevel.ERROR,
							"Unexpected error/ behavior while saving clinic locations page");
				}
				report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
						"Clinic Users page changes verified and successful", "FAILED: Clinic Users verification");
			}
		}
		// Navigate, edit and verify Clinic Locations page
		result = true;
		clinicAdminNavigationBar.viewClinicLocationsPage();
		result = clinicLocationsPage.validate();
		report.assertWithScreenshot(AssertionLevel.SOFT, result, true, "Loaded Clinic Locations page succesfully.",
				"Failed to navigate Clinic Locations page");
		if (result) {
			List<String> locations = new ArrayList<String>();
			locations.add(CommonUtils.randomAlphanumericString(15));
			clinicLocationsPage.addLocations(locations);

			if (clinicLocationsPage.isChangeSaved()) {
				report.assertWithScreenshot(AssertionLevel.SOFT, clinicLocationsPage.verifyLocation(locations), true,
						"Update to Clinic Locations page are successful", "Failed to update clinic Locations page");
			} else {
				if (clinicLocationsPage.IsRandomPopupDisplayed()) {
					report.logStepWithScreenshot(ReportLevel.ERROR,
							"Unexpected error/ behavior while saving clinic locations page");
				}
				report.assertWithScreenshot(AssertionLevel.SOFT, false, true,
						"Clinic location page changes verified and successful", "FAILED: Clinic location verification");
			}
		}
	}

	@Override
	@AfterMethod
	public void cleanup(ITestResult result) {
		loginUtils.logout();
		if(!manualCustomerSetup) {
			commonDBUtils.updateMFA(customerInfo.getMainContact_EmailID());
		}
		if (!result.isSuccess()) {
			fetchGraylogReports(result, report, Microservice.ALL_MICROSERVICES);
		}

		super.cleanup(result);
	}
}