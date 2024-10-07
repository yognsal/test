package com.mnet.framework.web;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.reporting.FrameworkLog;

import io.github.bonigarcia.wdm.WebDriverManager;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

/**
 * Defines browser navigation functionality via Selenium Driver.
 * @version Spring 2023
 * @author Arya Biswas, Kranti Naik, Rishab Kotwal
 */
public class WebUtilities {
	
	private static final String BROWSER = FrameworkProperties.getProperty("BROWSER");
	/**Defines maximum waiting interval (in seconds) for loading page elements.*/
	private static final Long BROWSER_MAX_TIMEOUT = Long.parseLong(FrameworkProperties.getProperty("BROWSER_MAX_TIMEOUT"));
	/**Defines minimum waiting interval (in ms) for loading page elements. Should be kept as low as feasible.*/
	private static final Long BROWSER_MIN_TIMEOUT = Long.parseLong(FrameworkProperties.getProperty("BROWSER_MIN_TIMEOUT"));
	/**Scaling factor for browser resolution when taking scrolling screenshot.*/
	private static final float SCROLLING_SCREENSHOT_SCALE_FACTOR = 1.5f;
	/**Timeout between scrolls when taking scrolling screenshot*/
	private static final int SCROLLING_SCREENSHOT_TIMEOUT = 100;
	
	public enum Dropdown_SpaceSelector{
		NO_SPACE, BOTH_SPACE, LEFT_SPACE, RIGHT_SPACE
	};
	
	public enum SiblingType {
		FOLLOWING, PROCEEDING, ALL
	};
	
	public enum ControlType {
		CHECKBOX, DROPDOWN, OTHER, LINK
	};

	//common cssproperty.
	public enum CSSProperty {
		BACKGROUND_COLOR ("background-color"), TEXT_COLOR ("color"), 
		FONT_BOLD_STYLE ("font-weight"),BORDER_COLOR("border-color"), 
		CURSOR_TYPE ("cursor");
		
		private String property;

		private CSSProperty(String property) {
			this.property = property;
		}

		public String getProperty() {
			return this.property;
		}	
		
		public String getCSSValue() {
			return this.property;
		}
	}
	
	private WebDriver driver;
	private WebDriverWait driverWait;
	private FrameworkLog log;
	
	public WebUtilities(FrameworkLog frameworkLog) {
		log = frameworkLog;
		
		switch (BROWSER.toLowerCase()) {
			default:
			case "edge":
				WebDriverManager.edgedriver().setup();
				driver = new EdgeDriver();
				break;
			case "chrome":
				WebDriverManager.chromedriver().setup();
				driver = new ChromeDriver();
				break;
		}
		
		driver.manage().window().maximize();
		driverWait = new WebDriverWait(driver, BROWSER_MAX_TIMEOUT);
	}
	
	/**
	 * Navigates browser to destination URL.
	 * @param url Web address to open in browser.
	 * @param validationElement Unique element used to verify identity of page.
	 * @boolean true if page was opened and validated with expected element.
	 */
	public boolean loadWebpage(String url, By validationElement) {
		
		try {
			driver.navigate().to(url);
		} catch (WebDriverException wde) {
			log.error("Failed to navigate to URL: " + url);
			log.printStackTrace(wde);
			throw new RuntimeException(wde);
		}
		
		return waitTillElementVisible(validationElement);
	}
	
	/**
	 * Halts execution until designated webpage element is visible. 
	 * @param element Webpage element obtained from page object.
	 * @return true if element is present and visible on page.
	 */
	public boolean waitTillElementVisible(By element) {
		try {
			driverWait.until(ExpectedConditions.visibilityOfElementLocated(element));
		} catch (TimeoutException te) {
			log.warn("Failed to locate web element: " + element.toString());
			log.printStackTrace(te);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Halts execution until designated webpage element is visible and overlay element is hidden.
	 * @param element Webpage element obtained from page object.
	 * @param overlay Temporary webpage element obscuring view.
	 * @return true if element is present and visible on page.
	 */
	public boolean waitTillElementVisibleAndLoaded(By element, By overlay) {
		staticWait(BROWSER_MIN_TIMEOUT); // wait for overlay to become visible
		
		try {
			waitTillElementHidden(overlay);
		} catch(RuntimeException re) {
			log.warn("Overlay element was never hidden or was not present: " + overlay.toString());
			log.printStackTrace(re);
			return false;
		}
		
		if (!waitTillElementVisible(element)) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Halts execution for time (in ms) defined in properties file.
	 * @implNote waitTillElementVisible(By) is generally preferred over staticWait().
	 * @param millis Time to wait in milliseconds
	 * @see BROWSER_MAX_TIMEOUT, BROWSER_MIN_TIMEOUT
	 */
	public void staticWait(Long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ie) {
			log.error("Thread execution interrupted during staticWait()");
			log.printStackTrace(ie);
			throw new RuntimeException(ie);
		}
	}
	
	/**
	 * Identifies visibility of element on page.
	 * @implNote Does not guarantee that page has been loaded - use waitTillElementVisible(By) if page may still be loading.
	 * @return true if element is already visible on page. 
	 */
	public boolean containsElement(By element) {
		try {
			return findWebElement(element).isDisplayed();
		} catch (NoSuchElementException nse){
			log.warn("Failed to locate web element: " + element.toString());
			return false;
		}		
	}

	/**
	 * Clicks on the specified webpage element. Does not guarantee that click will not be intercepted.
	 * If an overlay element is present, use clickElementWaitFor(By, By) instead.
	 * @param element Webpage element derived from page object.
	 */
	public void clickElement(By element) {
		waitTillElementClickable(element);
		findWebElement(element).click();
	}
	
	/**
	 * Clicks on the specified webpage element, accounting for any elements (loading icon, etc) that may intercept the click.
	 * @param element Webpage element derived from page object.
	 * @param overlay Temporary webpage element obscuring clickable object.
	 */
	public void clickElementWaitFor(By element, By overlay) {
		staticWait(BROWSER_MIN_TIMEOUT); // wait for overlay to become visible
		waitTillElementHidden(overlay);
		
		waitTillElementClickable(element);
		
		findWebElement(element).click();
	}
	
	/**
	 * Searches for element text that contains the regex pattern specified. Pauses web driver up to timeout period while searching.
	 */
	public boolean elementContainsText(By element, String regex) {
		Pattern regexPattern = Pattern.compile(regex);
		
		try {
			driverWait.until(ExpectedConditions.textMatches(element, regexPattern));
		} catch (TimeoutException te) {
			String err = "Text could not be located with regex pattern: " + regex + " in element: " + element.toString();
			log.error(err);
			log.printStackTrace(te);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Select/ un-select Radio Button type control
	 * Replace with new method with additional parameter
	 * 
	 * @param checked - select radio button if true else un-select
	 * @param element to be checked/ unchecked
	 * @author NAIKKX12
	 */
	@Deprecated
	// Call to be replaced with selectRadioOption(checked, element, false) 
	public void selectRadioOption(boolean checked, By element) {
		if (isRadioSelected(element) != checked) {
			clickElement(element);
		}
	}
	
	/**
	 * Select/ un-select Radio Button type control
	 * 
	 * @param checked - select radio button if true else un-select
	 * @param element to be checked/ unchecked
	 * @author NAIKKX12
	 */
	public void selectRadioOption(boolean checked, By element, boolean useDefaultMethod) {
		if (isRadioSelected(element, useDefaultMethod) != checked) {
			try {
				clickElement(element);				
			} catch (Exception e) {
				JavascriptExecutor executor = (JavascriptExecutor) driver;
				executor.executeScript("arguments[0].click();", driver.findElement(element));
			}
		}
	}
	
	/**
	 * Verify if Radio Button is selected or not
	 * *** Use revised method with additional parameter
	 * @param element - Radio Button element to verify for
	 * @return boolean - true if selected else false
	 * @author NAIKKX12
	 */
	@Deprecated
	// Call to be replaced with isRadioSelected(element, false)
	public boolean isRadioSelected(By element) {
		waitTillElementClickable(element);
		return verifyAttributeValue(element, "class", "mat-radio-checked");			
	}
	
	/**
	 * Verify if Radio Button is selected or not
	 * 
	 * @param element - Radio Button element to verify for
	 * @param useDefaultMethod - use selenium provided method 'isSelected'
	 * @return boolean - true if selected else false
	 * @author NAIKKX12
	 */
	public boolean isRadioSelected(By element, boolean useDefaultMethod) {
		if (useDefaultMethod) {
			WebElement pageElement = findWebElement(element);
			waitTillElementClickable(element);
			return pageElement.isSelected();			
		} else {
			waitTillElementClickable(element);
			return verifyAttributeValue(element, "class", "mat-radio-checked");			
		}
	}
	
	/**
	 * Select/ un-select CheckBox type control
	 * 
	 * @param checked - select CheckBox if true else un-select
	 * @param element to be checked/ unchecked
	 * @author NAIKKX12
	 */
	public void selectCheckBox(boolean checked, By element) {
		if (isCheckBoxSelected(element) != checked) {
			clickElement(element);
		}
	}
	
	/**
	 * Verify if CheckBox is selected or not
	 * 
	 * @param element - CheckBox element to verify for
	 * @return boolean - true if selected else false
	 * @author NAIKKX12
	 */
	public boolean isCheckBoxSelected(By element) {
		waitTillElementClickable(element);
		WebElement pageElement = findWebElement(element);
		List<WebElement> childElements = pageElement.findElements(By.xpath("./child::*"));
		WebElement child = pageElement;
		for (WebElement webelement : childElements) {
			if (webelement.getAttribute("type").equals("checkbox")) {
				child = webelement;
				break;
			}
		}
		return child.isSelected();
	}
	
	/**
	 * Sends text to the specified webpage element. Clears out any existing text in the field. replace by enterText(text, element, false) method
	 * @param text Text to be entered in designated field. If value is empty or null, does nothing.
	 * @param element Webpage element derived from page object.
	 */
	@Deprecated
	public void enterText(String text, By element) {
		if (text == null) {
			log.warn("Text to enter is null; correct it if required");
			return;
		}
		if (StringUtils.isEmpty(text)) {
			log.warn("Text to enter is empty; correct it if required");
		}
		
		WebElement pageElement = findWebElement(element);
		
		pageElement.clear();
		pageElement.sendKeys(text);
	}
	
	/**
	 * Enter text either using sendkeys() or actions class based on control behavior
	 * @param useAction - enter text using Action class if true
	 */
	public void enterText(String text, By element, boolean useAction) {
		if (text == null) {
			log.warn("Text to enter is null; correct it if required");
			return;
		}
		if (StringUtils.isEmpty(text)) {
			clearText(element);
		}
		WebElement pageElement = findWebElement(element);
		if (useAction) {
			Actions action = new Actions(driver);
			action.moveToElement(pageElement).doubleClick().sendKeys(text).build().perform();
		} else {
			pageElement.clear();
			clearText(element);
			pageElement.sendKeys(text);
		}
	}
	
	/**
	 * Enter text by actions class - replace by enterText(text, element, true) method
	 * @param text
	 * @param element
	 * @author NAIKKX12
	 */
	@Deprecated
	public void enterTextByActions(String text, By element) {
		if (text == null) {
			log.warn("Text to enter is null; correct it if required");
			return;
		}
		if (StringUtils.isEmpty(text)) {
			log.warn("Text to enter is empty; correct it if required");
		}
		
		WebElement pageElement = findWebElement(element);
		
		Actions action = new Actions(driver);
		action.moveToElement(pageElement).doubleClick().sendKeys(text).build().perform();
	}
	
	/**
	 * Clear element content using Actions class
	 * @param element
	 * @author NAIKKX12
	 */
	public void clearText(By element) {
		Actions action = new Actions(driver);
		selectAllKeyboardEvent(element);
		action.sendKeys(Keys.DELETE).build().perform();
	}
	
	/**
	 * 
	 * Perform keyboard event on the web element
	 * 
	 * @param key     - key to enter
	 * @param element
	 * 
	 * @author NAIKKX12
	 */
	public void enterText(Keys key, By element) {
		WebElement pageElement = findWebElement(element);
		pageElement.clear();
		pageElement.sendKeys(key);
	}
	
	/**
	 * Returns true if text is present and visible in as a substring of the element text.
	 */
	public boolean findTextInElement(String text, By element) {
		WebElement pageElement = findWebElement(element);
		return pageElement.getText().contains(text);
	}
	
	/**
	 * Takes screenshot of current web driver viewport and saves it to target path.
	 */
	public void takeScreenshot(String path) {
		File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
		
		try {
			FileUtils.copyFile(screenshot, new File(path));
		} catch (IOException ioe) {
			log.error("Failed to save screenshot at path: " + path);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	/**
	 * Takes screenshot of current web driver instance by scrolling the viewport and saves it to target path.
	 */
	public void takeScrollingScreenshot(String path) {
		Screenshot screenshot = new AShot().shootingStrategy(
				ShootingStrategies.viewportPasting(
						ShootingStrategies.scaling(SCROLLING_SCREENSHOT_SCALE_FACTOR), SCROLLING_SCREENSHOT_TIMEOUT)
							).takeScreenshot(driver);
		
		try {
			ImageIO.write(screenshot.getImage(), "jpg", new File(path));
		} catch (IOException ioe) {
			log.error("Failed to save screenshot at path: " + path);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	/**
	 * Closes all windows and quit the web driver.
	 */
	public void quitDriver() {
		driver.quit();
	}
	
	/**
	 * Find elements matching with element specified
	 * 
	 * @param element
	 * @return list of elements matching with provided element ID
	 * @author NAIKKX12
	 */
	public List<WebElement> findMatchingElementList(By element) {
		return driver.findElements(element);
	}
	
	/**
	 * Method to extract content of text field type elements
	 * 
	 * @param element - element to extract text
	 * @return String content of the element
	 * @author NAIKKX12
	 */
	public String getTextFieldValue(By element) {
		WebElement pageElement = findWebElement(element);
		return pageElement.getAttribute("value");
	}
	
	/**
	 * Method to extract content of drop-down type elements
	 * 
	 * @param element - element to extract text
	 * @return String content of the element
	 * @author NAIKKX12
	 */
	public String getDropDownValue(By element) {
		WebElement pageElement = findWebElement(element);
		return pageElement.getText();
	}
	
	/**
	 * Select target value from drop-down type control
	 * 
	 * @param dropdown Dropdown page element
	 * @param value Name of element to be selected from dropdown
	 * @author NAIKKX12
	 */
	public void selectValueFromMatDropdown(String value, By element, Dropdown_SpaceSelector sp) {
		if (value == null) {
			log.warn("Text to select is null; correct it if required");
			return;
		}
		
		clickElement(element);
		switch(sp) {
			case NO_SPACE:
				clickElement(By.xpath("//span[.='" + value + "']"));
				break;
			case BOTH_SPACE:
				clickElement(By.xpath("//span[.=' " + value + " ']"));
				break;
			case LEFT_SPACE:
				clickElement(By.xpath("//span[.=' " + value + "']"));
				break;
			case RIGHT_SPACE:
				clickElement(By.xpath("//span[.='" + value + " ']"));
				break;
		}
	}
	
	/**
	 * Search and select target value in mat type drop-down element
	 * 
	 * @param value         to be selected
	 * @param element       - drop down element
	 * @param searchElement - Search element from the drop-down
	 * @author NAIKKX12
	 */
	public void searchSelectValueInMatDropdown(String value, By element, By searchElement) {
		if (value == null) {
			log.warn("Text to select is null; correct it if required");
			return;
		}
		clickElement(element);
		WebElement pageElement = findWebElement(searchElement);
		clearText(searchElement);
		pageElement.sendKeys(value);
		pageElement.sendKeys(Keys.ENTER);
	}
	
	/**
	 * Read data from dynamic table element
	 * @param element
	 * @return List of List with each internal list item is column data whereas external list contains
	 * list of rows
	 */
	public ArrayList<ArrayList<String>> getTableData(By element) {
		ArrayList<ArrayList<String>> tableContent = new ArrayList<ArrayList<String>>();
		ArrayList<String> temp = new ArrayList<String>();
		WebElement table = driver.findElement(element);
		WebElement ele;
		
		List<WebElement> colHeaders = new ArrayList<WebElement>();//  table.findElements(By.xpath("./thead/tr/th[*]"));
			
		if (colHeaders.size() == 0) {
			int index = 1;
			while (true) {
				if (table.findElements(By.xpath("./thead/tr/th[" +index+ "]")).size() == 0) {
					break;
				}
				colHeaders.add(table.findElements(By.xpath("./thead/tr/th[" +index+ "]")).get(0));
				index++;
			}
		}
		for (int index = 0; index < colHeaders.size(); index++) {
			temp.add(colHeaders.get(index).getText());
		}
		tableContent.add(temp);
		
		List<WebElement> rows = table.findElements(By.xpath("./tbody/tr[*]"));
		for (int rowIndex = 1; rowIndex <= rows.size(); rowIndex++) {
			temp = new ArrayList<String>();
			for (int colIndex = 1; colIndex <= colHeaders.size(); colIndex++) {
				ele = table.findElement(By.xpath("./tbody/tr["+rowIndex+"]/td["+colIndex+"]"));
				temp.add(ele.getText());
			}
			tableContent.add(temp);
		}
		
		for (int index = 0; index < tableContent.size(); index++) {
			tableContent.get(index).toString();
		}
		return tableContent;
	}
	
	/**
	 * Function to extract given column data from the table mentioned
	 * @param tableElement
	 * @param columnName
	 * @return
	 */
	public ArrayList<String> getColumnData(By tableElement, String columnName) {
		ArrayList<ArrayList<String>> tableContent = getTableData(tableElement);
		ArrayList<String> columnData = new ArrayList<String>();
		
		int columnIndex = -1;
		columnIndex = tableContent.get(0).indexOf(columnName);
		if (columnIndex >= 0) {
			for (int index = 1; index < tableContent.size(); index++) {
				columnData.add(tableContent.get(index).get(columnIndex));
			}
		}
		return columnData;
	}
	
	
	/**
	 * Searches whether the element contains the specific class or not
	 * @param element Webpage element obtained from page object.
	 * @param class name we want to verify
	 * @return true if class is part of the element
	 * @author kotwarx2
	 */
	public boolean verifyAttributeValue(By element, String attribute, String attributeValue) {
		WebElement elem = driver.findElement(element);
		String classes = elem.getAttribute(attribute);
		for(String c : classes.split(" ")) {
			if(c.equals(attributeValue)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Switches to the specified iFrame and checks for the presence of element in that
	 * particular iFrame
	 * @param elementInIframe - The element we need to check if present in iFrame
	 * 		  iframeLocator - The locator to find the iFrame
	 * @return None
	 * @author kotwarx2
	 */
	public void switchToIFrame(By elementInIframe, By iframeLocator) {
		if(findMatchingElementList(elementInIframe).size() > 0) {
			driver.switchTo().defaultContent();
		}else {
			driver.switchTo().frame(driver.findElement(iframeLocator));
		}
		
	}
	
	/**
	 * Check if element is clickable or not
	 * @param The element we need to check for
	 * @return true if element clickable else false
	 * @author kotwarx2
	 */
	public boolean elementClickable(By element) { 
		WebElement elem = driverWait.until(ExpectedConditions.elementToBeClickable(element));
		if(elem.isEnabled()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Halts execution until designated webpage element is invisible
	 * @param The element that needs to be invisible before we proceed
	 * @return None
	 * @author kotwarx2
	 */
	public void elementInvisible(By element) {
		driverWait.until(ExpectedConditions.invisibilityOfElementLocated(element));
	}
	
	/**
	 * Utility to refresh the webpage
	 * @param None
	 * @return None
	 * @author kotwarx2
	 */
	public void refreshPage() {
		driver.navigate().refresh();
	}
	
	/**
	 * Function to return the current web-page URL
	 * @return string containing the URL
	 */
	public String getCurrentUrl() {
		return driver.getCurrentUrl();
	}

	/**
	 * Function to perform keyboard operations
	 * @param key
	 */
	public void handleKeyPress(Keys key) {
		Actions action = new Actions(driver);
		action.sendKeys(key).build().perform();
	}
	
	/**
	 * Select all content using keyboard event
	 * @param element
	 */
	public void selectAllKeyboardEvent(By element) {
		WebElement pageElement = findWebElement(element);
		clickElement(element);
		pageElement.sendKeys(Keys.chord(Keys.CONTROL+"A"));
	}
	
	/**
	 * Move to specified element
	 * @param element
	 */
	public void moveToElement(By element) {
		WebElement pageElement = findWebElement(element);
		new Actions(driver).moveToElement(pageElement).perform();
	}
	
	/**
	 * Switch to mentioned iframe
	 * @param element
	 */
	public void switchFrame(By element) {
		WebElement frameElement = findWebElement(element);
		driver.switchTo().frame(frameElement);
	}
	
	/**
	 * switch back to default web page (if currently if dealing with iframe)
	 */
	public void switchToDefault() {
		driver.switchTo().defaultContent();
	}
	
	/**
	 * Return the max browser timeout set in ms
	 * @return
	 */
	public long maxBrowserTimeout() {
		return BROWSER_MAX_TIMEOUT * 1000l;
	}
	
	/**
	 * return the min browser timeout set in ms
	 * @return
	 */
	public long minBrowserTimeout() {
		return BROWSER_MIN_TIMEOUT;
	}
	
	/**
	 * To zoom in browser window
	 * @param attempt - Total number of times to zoom in; in each attempt, it zoom in by 10%
	 */
	public void browserZoomIn(int attempt) {
		int index = 1;
		try {
			Robot robot = new Robot();
			while (index <= attempt) {
				robot.keyPress(KeyEvent.VK_CONTROL);
				robot.keyPress(KeyEvent.VK_MINUS);
				robot.keyRelease(KeyEvent.VK_MINUS);
				robot.keyRelease(KeyEvent.VK_CONTROL);
				index++;
			}
		} catch (AWTException awte) {
			log.error("Failed to create robot object");
			log.printStackTrace(awte);
			throw new RuntimeException(awte);
		}
	}
	
	/**
	 * Get drop-down list content (Mat drop-down to be clicked to be visible)
	 * @param element
	 * @return
	 */
	public List<String> getMatDropDownContent(By element) {
		WebElement pageElement = findWebElement(element); 
		List<WebElement> childElements = pageElement.findElements(By.xpath("./child::*"));
		List<String> content = new ArrayList<>();
        for (WebElement ele : childElements) {
        	content.add(ele.getText());
        }
        clickElement(element);
        return content;
	}
	
	public List<String> getMatDropDownContent(By element, By subElement) {
		clickElement(element);
		WebElement pageElement = findWebElement(subElement); 
		List<WebElement> childElements = pageElement.findElements(By.xpath("./child::*"));
		List<String> content = new ArrayList<>();
        for (WebElement ele : childElements) {
        	content.add(ele.getText());
        }
        return content;
	}
	
	/**
	 * return true if element is enabled
	 * @param element
	 * @return
	 */
	@Deprecated
	//TODO: this function can be replaced with isEnabled(By, ControlType) function
	public boolean isEnabled(By element) {
		WebElement uiElement = findWebElement(element);
		return uiElement.isEnabled();
	}
	
	/**
	 * return true if element is enabled; customized since default method not working for checkbox & dropdown type controls
	 * @param element
	 * @return
	 */
	public boolean isEnabled(By element, ControlType controlType) {
		WebElement pageElement = findWebElement(element);
		switch (controlType) {
		case CHECKBOX:
			List<WebElement> childElements = pageElement.findElements(By.xpath("./child::*"));
			WebElement child = pageElement;
			for (WebElement webelement : childElements) {
				if (webelement.getAttribute("type").equals("checkbox")) {
					child = webelement;
					break;
				}
			}
			return child.isEnabled();
		case DROPDOWN:
			return !pageElement.getAttribute("class").contains("mat-select-disabled");
		case LINK:
			return pageElement.getAttribute("disabled") == null || pageElement.getAttribute("disabled").equals("false") ? true : false;			
		case OTHER:
			return pageElement.isEnabled();
		}
		return false;
	}
		
	/**
	 * Get list of all active tabs in the webpage
	 */
	public ArrayList<String> getAllWindows(){
		driverWait.until(ExpectedConditions.numberOfWindowsToBe(2));
		ArrayList<String> handles = new ArrayList<String>(driver.getWindowHandles());
		log.info(handles.toString());
		return handles;
	}
	
	/**
	 * switch to new tab and get the address of active tab
	 */
	public String getNewWindowAdded() {
		ArrayList<String> allWindows = getAllWindows();
		driver.switchTo().window(allWindows.get(1));
		staticWait(BROWSER_MIN_TIMEOUT * 10);
		return driver.getCurrentUrl();
	}
	
	/**
	 * Close window and switch back to parent/default page
	 */
	public void closeNewWindow() {
		ArrayList<String> allWindows = getAllWindows();
		driver.switchTo().window(allWindows.get(1));
		staticWait(BROWSER_MIN_TIMEOUT);
		driver.close();
		driver.switchTo().window(allWindows.get(0));
	}
	
	/**
	 * Get provided property value of the element
	 */
	public String getCssValue(By element, CSSProperty property) {
		WebElement webElement = driver.findElement(element);
		return webElement.getCssValue(property.getProperty());
	}
	
	/**
	 * Get siblings - following, proceeding, all
	 */
	public List<WebElement> getSiblings(By element, SiblingType siblingType) {
		List<WebElement> siblings = new ArrayList<>();
		
		WebElement node = findWebElement(element);
		
		switch (siblingType) {
		case FOLLOWING:
			return node.findElements(By.xpath("following-sibling::*"));
		case PROCEEDING:
			return node.findElements(By.xpath("preceding-sibling::*"));
		case ALL:
			siblings.addAll(node.findElements(By.xpath("preceding-sibling::*")));
			siblings.addAll(node.findElements(By.xpath("following-sibling::*")));		
			return siblings;
		default:
			log.error(siblingType + " not handled");
			return null;
		}	
	}
	
	/**
	 * Select target value from drop-down type control (Alternate way in case of org.openqa.selenium.ElementClickInterceptedException)
	 * 
	 * @param dropdown Dropdown page element
	 * @param panel element identifier as string
	 * @author NAIKKX12
	 */
	public void selectFromListPanel(String value, By listElement, String listPanelIdentifier) {
		int  matOption = 1;
		String panelListItem = listPanelIdentifier + "/mat-option[<matOption>]";
		
		if (value == null) {
			log.warn("Text to select is null; correct it if required");
			return;
		}
		
		clickElement(listElement);
		
		By element;
		boolean elementFound = false;
		while (true) {
			element = By.xpath(panelListItem.replace("<matOption>", String.valueOf(matOption)));
			if (findMatchingElementList(element).size() == 0) {
				break;
			}
			
			if (findWebElement(element).getText().trim().equals(value)){
				clickElement(element);
				elementFound = true;
				break;
			}
			matOption++;
		}
		
		if (!elementFound) {
			String err = "Failed to select " + value + " in dropdown";
			log.error(err);
			throw new RuntimeException(err);
		}
	}

	/**
	 * Hover over an Element
	 * @param element Webpage element obtained from page object.
	 * @return None
	 * @author kaisasx
	 */
	public void hoverElement(By element)
	{
		Actions action = new Actions(driver);
		waitTillElementClickable(element);
		WebElement pageElement = findWebElement(element);

		action.moveToElement(pageElement).build().perform();
	}
	
	//Click on an element uusing javaScriptExecutor
	public void clickButton(By element)
	{
		JavascriptExecutor js = (JavascriptExecutor) driver;
		js.executeScript("arguments[0].click();", driver.findElement(element));		
		
	}
	
	/*
	 * Helper functions
	 */
	
	/**Wrapper for webDriver.findElement to handle exceptions with custom log message*/
	private WebElement findWebElement(By element) {
		try {
			return driver.findElement(element);
		} catch (NoSuchElementException nse) {
			String err = "Failed to locate web element: " + element.toString();
			log.error(err);
			log.printStackTrace(nse);
			throw new RuntimeException(err);
		}
	}
	
	private void waitTillElementClickable(By element) {
		try {
			driverWait.until(ExpectedConditions.elementToBeClickable(element));
		} catch (TimeoutException te) {
			log.error("Element could not be idenitified as clickable: " + element.toString());
			log.printStackTrace(te);
			throw new RuntimeException(te);
		}
	}
	
	private void waitTillElementHidden(By element) {
		try {
			driverWait.until(ExpectedConditions.invisibilityOfElementLocated(element));
		} catch (TimeoutException te) {
			log.error("Element was not hidden: " + element.toString());
			log.printStackTrace(te);
			throw new RuntimeException(te);
		}
	}
}
