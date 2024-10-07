package com.mnet.framework.web;

import org.openqa.selenium.By;

import lombok.Getter;

/**
 * Base class for all page objects.
 * @version Spring 2023
 * @author Arya Biswas
 */
public abstract class MITEPage {
	
	/**Static URL associated with page, if applicable.*/
	@Getter
	private String url;
	
	/**Element always expected to be on the page. validate() will check for the presence of this element.*/
	@Getter
	private By pageValidationElement;
	
	protected WebUtilities webDriver;
	
	/**
	 * @param driver WebUtilties object owned by test class.
	 * @param validationElement An element always expected to be on the page. validatePage() will check for the presence of this element.
	 */
	public MITEPage(WebUtilities driver, By validationElement) {
		webDriver = driver;
		pageValidationElement = validationElement;
	}
	
	/**
	 * Initializes page with optional static url parameter.
	 * @param driverWebUtilties object owned by test class.
	 * @param validationElement An element always expected to be on the page.
	 * @param url Static web address of the page.
	 */
	public MITEPage(WebUtilities driver, By validationElement, String url) {
		this(driver, validationElement);
		this.url = url;
	}
	
	/**
	 * Opens URL associated with page in browser.
	 * @return true: If page was opened succesfully.
	 * false: If page URL is invalid or if page could not be validated with expected element.
	 */
	public boolean load() {
		return webDriver.loadWebpage(url, pageValidationElement);
	}
	
	/**
	 * Override this function in subclass if additional / alternate validation is required.
	 * @return true if and only if page contains expected page validation element
	 */
	public boolean validate() {
		return webDriver.waitTillElementVisible(pageValidationElement);
	}
}
