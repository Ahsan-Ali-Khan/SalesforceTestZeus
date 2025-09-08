package utils;

import static org.testng.Assert.assertEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONArray;
import org.openqa.selenium.By;
import org.openqa.selenium.By.ByName;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.jayway.jsonpath.JsonPath;



/*@author Robin
@date: 04/02/2021
@purpose: This class gets the UI layout from UI API and tries to make the xpath for all the elements 👼
@see: A lot of these methods are implemented using JSONPATH to parse the response we get from UI API*/


public class SFPageBase extends PageBase {

	public SFPageBase(WebDriver driver) {
		super(driver);
	}

	protected static String uiapi_record_json;
	private static ArrayList<String> listoflabels;
	protected static HashMap<String, String> labelandtype;

	public void loginToSalesforce(String loginUrl, String grantService, String clientId,
			String clientSecret, String username, String password) {
		HTTPClientWrapper.SFLogin_API(loginUrl, grantService, clientId, clientSecret, username, password);
	}

	public void waitForSFPagetoLoad() throws InterruptedException {
		// Below is a custom wait method specifically built for Salesforce based on the
		// concept of EPT
		// https://trailhead.salesforce.com/en/content/learn/modules/lightning-experience-performance-optimization/measure-lightning-experience-performance-and-experience-page-time-ept
		Thread.sleep(3000);
		try {
			WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(50));

			ExpectedCondition<Boolean> jsLoad = new ExpectedCondition<Boolean>() {
				@Override
				public Boolean apply(WebDriver driver) {
					return ((JavascriptExecutor) driver).executeScript("return document.readyState").toString()
							.equals("complete");
				}
			};

			ExpectedCondition<Boolean> aurascriptLoad = new ExpectedCondition<Boolean>() {
				@Override
				public Boolean apply(WebDriver driver) {
					String WAIT_FOR_AURA_SCRIPT = "return (typeof $A !== 'undefined' && $A && $A.metricsService.getCurrentPageTransaction().config.context.ept > 0)";
					String EPT_COUNTER_SCRIPT = "return ($A.metricsService.getCurrentPageTransaction().config.context.ept)";
					Boolean result = (Boolean) ((JavascriptExecutor) driver).executeScript((WAIT_FOR_AURA_SCRIPT));

					if (result.equals(true)) {
						System.out.println("Experienced Page Load time in milliseconds on the current page is : "
								+ ((JavascriptExecutor) driver).executeScript(EPT_COUNTER_SCRIPT));
						return true;
					} else {
						return false;
					}

				}
			};
			if (wait1.until(jsLoad) && wait1.until(aurascriptLoad)) {
				System.out.println("Page load complete");
			} else {
				Thread.sleep(2000);
			}
		}

		catch (Exception e) {
			System.out.println("Exception happened in waiting for page to load , so sleeping for 5 seconds");
			System.out.println("Exception is " + e.getMessage());
			Thread.sleep(5000);

		}
	}

	public static void uiApiHitter(String recordID) throws IOException {
		// This method call is the heart of the UI API based automation and gets the UI
		// API
		// Json for further operations ♥
		// Here 0015g00000S9lfUAAR is the record ID of an ACCOUNT, but the same API and
		// general methods below can be used for the other sbjects.
		uiapi_record_json = (HTTPClientWrapper
				.runGetRequest("/ui-api/record-ui/" + recordID + "?formFactor=Large&modes=View,Edit")).toString();
		System.out.println("JSON : " + uiapi_record_json);

		try (FileWriter file = new FileWriter(System.getProperty("user.dir")+"\\output.json")) {
			file.write(uiapi_record_json);
		}

	}

	public static void sectionGetter() throws Exception {
		// This method brings in the count of sections displayed on the UI
		String apipath = "$..objectApiName";
		String sobjecttype = JsonPath.read(uiapi_record_json, apipath).toString();
		String sectionspath = "$.layouts."+sobjecttype+"..sections";

		JSONArray sectionsparent = new org.json.JSONArray(JsonPath.read(uiapi_record_json, sectionspath).toString());
		JSONArray sectionsarray = (JSONArray) sectionsparent.get(0);
		System.out.println("Count of Sections is : " + sectionsarray.length());

	}

	public static void labelGetter() throws Exception {
		// These labels are gathered from layoutComponents as we get labels which are
		// actually displayed on the UI rather than all the fields for the sObject
		String labelpath = "$..[?(@.editableForUpdate == true)].layoutComponents..label";
		System.out.println("PageObjectMetadata >>> " + uiapi_record_json);
		JSONArray listofduplicatelabels = new org.json.JSONArray(JsonPath.read(uiapi_record_json, labelpath).toString());
		// As we are hitting modes=View, Edit, hence we are getting duplicates.
		LinkedHashSet<String> labels = new LinkedHashSet<String>();
		for (int i = 0; i < listofduplicatelabels.length(); i++) {
			labels.add((String) listofduplicatelabels.get(i));
		}

		listoflabels = new ArrayList<String>();
		listoflabels.addAll(labels);
		System.out.println("Labels are " + labels);

	}

	public static void dataTypeGetter() throws Exception {
		// This method fetches the data type for all labels from the UI API JSON
		labelandtype = new HashMap<>();
		for (int i = 0; i < listoflabels.size(); i++) {
			String label = listoflabels.get(i);
			String typepath = "$..[?(@.label =='" + label + "')].dataType";

			String datatype = null;
			Object result = JsonPath.read(uiapi_record_json, typepath);
			// Print the first element of the list only
			datatype = ((List<?>) result).get(0).toString();
			labelandtype.put(label, datatype);
		}
		labelandtype.entrySet().forEach(entry -> {
			System.out.println("Label : '" + entry.getKey() + "' & its type '" + entry.getValue() + "'");
		});
	}

	public void uiApiParser(String recordid) throws Exception {
		uiApiHitter(recordid);
		sectionGetter();
		labelGetter();
		dataTypeGetter();
	}


	/**
	 * Purpose: Gets the Salesforce field API name from its UI label by using the
	 * describeSObject API, which does not require a record ID. This resolves the
	 * cyclic dependency issue.
	 * * @param objectApiName The API name of the sObject (e.g., 'Account').
	 * @param label The UI label of the field (e.g., 'Account Name').
	 * @return The field's API name or null if not found.
	 * @throws Exception If there is an issue with the API call.
	 */
	public static String getFieldNameFromLabel_SOBJECT_DESCRIBE(String objectApiName, String label) throws Exception {
		String describeUrl = "/sobjects/" + objectApiName + "/describe";
		JSONObject describeResponse = HTTPClientWrapper.runGetRequest(describeUrl);

		if (describeResponse == null || !describeResponse.has("fields")) {
			throw new RuntimeException("Failed to get describe response for " + objectApiName);
		}

		JSONArray fields = describeResponse.getJSONArray("fields");
		for (int i = 0; i < fields.length(); i++) {
			JSONObject field = fields.getJSONObject(i);
			if (field.getString("label").equals(label)) {
				return field.getString("name");
			}
		}

		System.out.println("❌ Field API name for label '" + label + "' not found in " + objectApiName + " describe data.");
		return null;
	}

	/**
	 * Purpose: Finds a record ID by first converting the UI label to a field API name
	 * using the sObject describe API, and then querying for the record. This method
	 * resolves the cyclic dependency and is the most reliable approach.
	 *
	 * @param objectApiName The API name of the sObject (e.g., 'Account').
	 * @param uiLabel The UI label of the field to search on (e.g., 'Account Name').
	 * @param fieldValue The value of the field to search for.
	 * @return The record ID or null if the record is not found.
	 * @throws Exception if there is an issue with API calls.
	 */
	public String getRecordIdFromUiLabel_Optimized(String objectApiName, String uiLabel, String fieldValue) throws Exception {

		// Step 1: Use the describe API to get the field's API name from its UI label.
		// This removes the need for a recordId to find the label.
		String fieldName = getFieldNameFromLabel_SOBJECT_DESCRIBE(objectApiName, uiLabel);

		if (fieldName == null) {
			System.out.println("❌ Could not find a field API name for UI label '" + uiLabel + "'.");
			return null;
		}

		// Step 2: Use the found field name to perform a SOQL query and get the record ID.
		String soql = String.format("SELECT Id FROM %s WHERE %s = '%s' LIMIT 1", objectApiName, fieldName, fieldValue);
		String encodedSoql = URLEncoder.encode(soql, "UTF-8");

		JSONObject response = HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);

		if (response != null && response.has("records") && response.getJSONArray("records").length() > 0) {
			return response.getJSONArray("records").getJSONObject(0).getString("Id");
		} else {
			System.out.println("❌ No record found with " + uiLabel + " = '" + fieldValue + "'");
			return null;
		}
	}

	/**
	 * Build the Lightning record URL for an object + recordId.
	 * Example: https://instance.lightning.force.com/lightning/r/Lead/00Q.../view
	 */
	public String getRecordUrl(String objectApiName, String recordId) {
		if (recordId == null || recordId.isEmpty()) {
			throw new IllegalArgumentException("recordId cannot be null/empty");
		}
		String base = utils.HTTPClientWrapper.getLoginInstanceUrl();
		if (base == null || base.isEmpty()) {
			throw new IllegalStateException("instanceUrl not initialized. Call SFLogin_API first.");
		}

		// remove trailing slash if present
		base = base.replaceAll("/+$", "");

		// Construct Lightning URL
		String lightningUrl = String.format("%s/lightning/r/%s/%s/view", base, objectApiName, recordId);
		return lightningUrl;
	}

	/**
	 * Navigate the WebDriver to the Lightning record page and wait for page load.
	 * Example usage: objectListPage.NavigateToRecord("Lead", recordId);
	 */
	public void NavigateToRecord(String objectApiName, String recordId) {
		String url = getRecordUrl(objectApiName, recordId);
		System.out.println(url);
		try {
			driver.get(url);

			// wait for LEX page to fully load with your existing method
			waitForSFPagetoLoad();
		} catch (Exception e) {
			System.out.println("Failed to open Lightning URL: " + e.getMessage());
			// fallback: try the short classic-style URL which often redirects
			try {
				String base = utils.HTTPClientWrapper.getLoginInstanceUrl().replaceAll("/+$", "");
				String fallback = base + "/" + recordId;
				System.out.println("Attempting fallback URL: " + fallback);
				driver.get(fallback);
				waitForSFPagetoLoad();
			} catch (Exception ex) {
				System.out.println("Fallback navigation also failed: " + ex.getMessage());
				throw new RuntimeException("Could not navigate to record: " + recordId, ex);
			}
		}
	}

	/**
	 * Get record Id by a single filter (field = value).
	 * Example:
	 *   getRecordId("Lead", "LastName", "Smith");
	 */
	public String getRecordId(String objectApiName, String fieldName, String fieldValue) {
		return getRecordId(objectApiName, new String[]{fieldName, fieldValue});
	}

	/**
	 * Get first record Id by multiple conditions using varargs.
	 * Example:
	 *   getRecordId("Lead", "Email", "john@example.com", "Company", "Acme Inc");
	 */
	public String getRecordId(String objectApiName, String... conditions) {
		try {
			List<String> ids = getRecordIds(objectApiName, conditions);
			return ids.isEmpty() ? null : ids.get(0);
		} catch (Exception e) {
			System.out.println("Error while fetching record Id: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get all record Ids by multiple conditions using varargs.
	 * Example:
	 *   getRecordIds("Lead", "Status", "Open - Not Contacted", "Company", "Acme Inc");
	 */
	public List<String> getRecordIds(String objectApiName, String... conditions) {
		List<String> ids = new ArrayList<>();
		try {
			// Build WHERE clause dynamically
			if (conditions.length % 2 != 0) {
				throw new IllegalArgumentException("Conditions must be in field-value pairs");
			}

			StringBuilder whereClause = new StringBuilder();
			for (int i = 0; i < conditions.length; i += 2) {
				String field = conditions[i];
				String value = conditions[i + 1];

				if (whereClause.length() > 0) {
					whereClause.append(" AND ");
				}
				whereClause.append(field).append(" = '").append(value).append("'");
			}

			String soql = "SELECT Id FROM " + objectApiName + " WHERE " + whereClause;
			String encodedSoql = URLEncoder.encode(soql, "UTF-8");

			JSONObject response = HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);

			if (response != null && response.has("records")) {
				JSONArray records = response.getJSONArray("records");
				for (int i = 0; i < records.length(); i++) {
					ids.add(records.getJSONObject(i).getString("Id"));
				}
			}
		} catch (Exception e) {
			System.out.println("Error while fetching record Ids: " + e.getMessage());
			e.printStackTrace();
		}
		return ids;
	}

	public void clickEditByFieldLabel(String fieldLabel) throws InterruptedException {
		Thread.sleep(5000);
		String xpath = String.format("//div[contains(@class,'active')]//button[@title='Edit %s']", fieldLabel);	    
		SFClick(driver.findElement(By.xpath(xpath)));
	}

	public void clickOnGlobalSearchTextbox(String placeholderText) {
		String xpath = String.format("//button[@aria-label='%s']", placeholderText);
		WebElement globalSearch= driver.findElement(By.xpath(xpath));
		SFClick(globalSearch);

	}


	public void clickSelectAllDownArrow(String placeholder) {
		String xpath = String.format(
				"//input[@data-value='%s']/ancestor::div[contains(@class,'slds-combobox__form-element')]//lightning-icon[@icon-name='utility:down']",
				placeholder
				);
		WebElement downArrow= driver.findElement(By.xpath(xpath));
		SFClick(downArrow);
	}

	public void clickListboxOption(String optionText) {
		String xpath = String.format(
				"//li[contains(@class,'slds-listbox__item')]//span[@title='%s']",
				optionText
				);
		WebElement listBoxOption= driver.findElement(By.xpath(xpath));
		SFClick(listBoxOption);
	}

	public void selectFirstSuggestedValue()
	{
		WebElement firstVisibleSuggestion = driver.findElement(
				By.xpath("(//search_dialog-instant-results-list//search_dialog-instant-result-item[.//span[normalize-space()!='']])[1]")
				);
		SFClick(firstVisibleSuggestion);
	}


	public void formValueFiller(String label, String targetvalue) throws Exception {
		// This method automagically uses the label and datatypes to fill the form on
		// the fly
		// And reduces the pain for creation and maintenance of separate pageobjects and
		// web elements
		WebElement we;
		String type = labelandtype.get(label);
		switch (type) {
		case "String":
		case "Url":
		case "Int":
		case "Phone":
		case "Currency":
		case "Double":
		case "Date":
		case "Boolean":
		case "Email":
			Thread.sleep(5000);
			// Locator design inspired by
			// https://trailblazers.salesforce.com/_ui/core/chatter/groups/GroupProfilePage?g=0F93A000000DQPd&fId=0D54S000008HKSK
			we = driver.findElement(By.xpath("//input[@id=string(//label[text()='" + label + "']/@for)]"));
			we.sendKeys(targetvalue);
			System.out.println("Sent values as " + targetvalue);
			break;
		case "TextArea":
			we = driver.findElement(By.xpath("//textarea[@id=string(//label[text()='" + label + "']/@for)]"));
			we.sendKeys(targetvalue);
			System.out.println("Sent values as " + targetvalue);
			break;
		case "Picklist":
			we = driver.findElement(By.xpath("//button[@id=string(//label[text()='" + label + "']/@for)]"));
			waitAndClick(we);
			we.sendKeys(targetvalue);
			Thread.sleep(2000);
			we.sendKeys(Keys.ENTER);
			System.out.println("Selected " + targetvalue);
			break;
		case "Reference":
			we = driver.findElement(By.xpath("//input[@id=string(//label[text()='" + label + "']/@for)]"));
			we.sendKeys(Keys.ARROW_DOWN);
			Thread.sleep(2000);
			we.sendKeys(Keys.ENTER);
			System.out.println("Sent values as " + targetvalue);
			break;
		}

	}
	public void formValueFillerClearInput(String label) throws Exception {
		// This method automagically uses the label and datatypes to fill the form on
		// the fly
		// And reduces the pain for creation and maintenance of separate pageobjects and
		// web elements
		WebElement we;
		String type = labelandtype.get(label);
		switch (type) {
		case "String":
		case "Url":
		case "Int":
		case "Phone":
		case "Currency":
		case "Double":
		case "Date":
		case "Boolean":
		case "Email":
			Thread.sleep(5000);
			// Locator design inspired by
			// https://trailblazers.salesforce.com/_ui/core/chatter/groups/GroupProfilePage?g=0F93A000000DQPd&fId=0D54S000008HKSK
			we = driver.findElement(By.xpath("//input[@id=string(//label[text()='" + label + "']/@for)]"));
			we.clear();
			System.out.println("Clear values as ");
			break;
		case "TextArea":
			we = driver.findElement(By.xpath("//textarea[@id=string(//label[text()='" + label + "']/@for)]"));
			we.clear();
			System.out.println("Clear values as " );
			break;
		case "Picklist":
			we = driver.findElement(By.xpath("//button[@id=string(//label[text()='" + label + "']/@for)]"));
			waitAndClick(we);
			we.clear();
			System.out.println("Selected " );
			break;
		case "Reference":
			we = driver.findElement(By.xpath("//input[@id=string(//label[text()='" + label + "']/@for)]"));
			we.clear();
			System.out.println("Clear values as ");
			break;
		}

	}
	public void assertFormValue(String label, String expectedValue) throws Exception {
		// This method automagically uses the label and datatypes to fill the form on
		// the fly
		// And reduces the pain for creation and maintenance of separate pageobjects and
		// web elements
		WebElement we;
		String type = labelandtype.get(label);
		switch (type) {
		case "String":
		case "Url":
		case "Int":
		case "Phone":
		case "Currency":
		case "Double":
		case "Date":
		case "Boolean":
		case "Email":
			Thread.sleep(5000);
			// Locator design inspired by
			// https://trailblazers.salesforce.com/_ui/core/chatter/groups/GroupProfilePage?g=0F93A000000DQPd&fId=0D54S000008HKSK
			we = driver.findElement(By.xpath("//input[@id=string(//label[text()='" + label + "']/@for)]"));
			Assert.assertEquals(we.getText(), expectedValue, "Field '" + label + "' value mismatch.");
			break;
		case "TextArea":
			we = driver.findElement(By.xpath("//textarea[@id=string(//label[text()='" + label + "']/@for)]"));
			Assert.assertEquals(we.getText(), expectedValue, "Field '" + label + "' value mismatch.");
			break;
		case "Picklist":
			we = driver.findElement(By.xpath("//button[@id=string(//label[text()='" + label + "']/@for)]"));
			Assert.assertEquals(we.getText(), expectedValue, "Field '" + label + "' value mismatch.");
			break;
		case "Reference":
			we = driver.findElement(By.xpath("//input[@id=string(//label[text()='" + label + "']/@for)]"));
			Assert.assertEquals(we.getText(), expectedValue, "Field '" + label + "' value mismatch.");
			break;
		}

	}
	public void assertFormValueSnags(String label, String expectedValue) throws Exception {
		WebElement we;
		String type = labelandtype.get(label);
		Thread.sleep(5000);
		we = driver.findElement(By.xpath("//ul[@class='errorsList slds-list_dotted slds-m-left_medium']/li/a[text()='"+label+"']"));
		Assert.assertEquals(we.getText(), expectedValue, "Field '" + label + "' value mismatch.");
	}
	public static void verifyRequiredFields(String testdatajson, String objname) {
		// This method checks whether the specified value is mandatory in the UI or not
		String valuename = objname + "Name";
		String isrequiredexpected = readJsonFile(testdatajson, "$." + valuename + ".isRequired");

		String objjson = HTTPClientWrapper.runGetRequest("/sobjects/" + objname + "/describe/layouts/").toString();
		String jsonpath = "$..[?(@.label==\"Account Name\")]..required";

		String isrequiredactual = JsonPath.read(objjson, jsonpath).toString();
		System.out.print("Validating that the object contains the right mandatory fields");
		if (isrequiredactual.contains(isrequiredexpected)) {
			System.out.println(
					"THIS IS A TEST-------------------------------------SHOULD BE REPLACED BY TESTNG/JUNIT ASSERTS");
			System.out.println("Required fields verified correctly");
			System.out.println("--------------------------------------------------------------------------");

		} else {
			System.out.println(
					"THIS IS A TEST-------------------------------------SHOULD BE REPLACED BY TESTNG/JUNIT ASSERTS");
			System.out.println("Required fields couldnt be verified correctly");
			System.out.println("--------------------------------------------------------------------------");
		}

	}

	public void globalSearch(String searchTerm, String objectType) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

		try {
			// 1. Click the global search button (magnifier icon)
			WebElement searchBtn = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//button[contains(@class,'search-button')]")));
			searchBtn.click();

			// 2. Handle object type filter if provided
			if (objectType != null && !objectType.isEmpty()) {
				// Click dropdown arrow
				WebElement dropdownArrow = wait.until(ExpectedConditions.elementToBeClickable(
						By.xpath("//lightning-icon[contains(@class,'slds-input__icon_right')]")));
				dropdownArrow.click();

				// Locate the "Search: All" combobox input
				WebElement filterInput = wait.until(ExpectedConditions.elementToBeClickable(
						By.xpath("//input[contains(@class,'slds-combobox__input')]")));

				// Clear existing text completely
				filterInput.click();

				// Type the objectType (e.g. Lead)
				int times = 3;
				for(int reps = 0; reps < times ; reps++) {
					filterInput.click();
					filterInput.sendKeys(Keys.BACK_SPACE);
					Thread.sleep(1000l);
				}
				filterInput.sendKeys(objectType);

				// Wait and select the dropdown option
				WebElement option = wait.until(ExpectedConditions.elementToBeClickable(
						By.xpath("//div[contains(@class,'slds-listbox')]//lightning-base-combobox-item//span[normalize-space(text())='" + objectType + "s']")));
				option.click();
			}

			// 3. Enter the actual search term in search textbox
			WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("//input[@type='search' and contains(@class,'slds-input')]")));
			searchInput.clear();
			searchInput.sendKeys(searchTerm);

			// 4. Wait for instant result and click the first one
			WebElement firstResult = wait.until(ExpectedConditions.elementToBeClickable(
					By.xpath("(//search_dialog-instant-result-item)[1]")));
			firstResult.click();

			System.out.println("PASS: Search success: " + searchTerm + 
					(objectType == null ? " in All" : " in " + objectType));

		} catch (Exception e) {
			System.out.println("FAIL: globalSearch failed: " + e.getMessage());
		}
	}

	// Overloaded method (default = All)
	public void globalSearch(String searchTerm) {
		globalSearch(searchTerm, null);
	}



	public String getURL(String appname) { // Method to get SF Apps URL and simulate 9 dot navigation
		GetSFApps getSfApps = new GetSFApps();
		return getSfApps.getAppNavURL(appname);

	}

	public void appLauncher(String appname) throws InterruptedException {
		String accountappurl = getURL(appname);
		System.out.println("account URL is" + accountappurl);
		String cleanurl = accountappurl.replace("[\"", "").replace("\"]", "");
		System.out.println("Navigating to App URL as : " + cleanurl);
		openHomepage(cleanurl + "?eptVisible=1");

		waitForSFPagetoLoad();

	}

	/**
	 * Purpose: Gets a WebElement for a specific field based on its UI label by constructing a reliable
	 * locator from UI API metadata.
	 *
	 * @param label The UI label of the field.
	 * @return The located field element.
	 * @throws Exception If the field is not found or its type is unsupported.
	 */
	public WebElement getFieldElementByLabel(String label) throws Exception {
		if (labelandtype == null || labelandtype.isEmpty()) {
			throw new IllegalStateException("labelandtype map is not initialized. Call uiApiParser() first.");
		}
		String type = labelandtype.get(label);
		if (type == null) {
			throw new IllegalArgumentException("Field with label '" + label + "' not found in UI API data.");
		}
		String xpath;
		switch (type) {
		case "String":
		case "Url":
		case "Int":
		case "Phone":
		case "Currency":
		case "Double":
		case "Date":
		case "Boolean":
		case "Email":
		case "Reference":
			xpath = String.format("//input[@id=string(//label[text()='%s']/@for)]", label);
			break;
		case "TextArea":
			xpath = String.format("//textarea[@id=string(//label[text()='%s']/@for)]", label);
			break;
		case "Picklist":
			xpath = String.format("//button[@id=string(//label[text()='%s']/@for)]", label);
			break;
		default:
			throw new UnsupportedOperationException("Unsupported field type for label '" + label + "': " + type);
		}
		return driver.findElement(By.xpath(xpath));
	}

	/**
	 * Purpose: Finds a record's ID by first finding the field's API name from its UI label (using UI API),
	 * then performing a SOQL query (using SOAP API) to get the record ID.
	 *
	 * @param objectApiName The Salesforce object API name (e.g., 'Account').
	 * @param label The UI label of the field (e.g., 'Account Name').
	 * @param value The value of the field to search for.
	 * @return The record ID or null if the record is not found.
	 * @throws Exception If there is an issue with API calls or JSON parsing.
	 */
	public String getRecordIdByUiLabel(String objectApiName, String label, String value) throws Exception {
		if (uiapi_record_json == null || uiapi_record_json.isEmpty()) {
			throw new IllegalStateException("UI API JSON is not loaded. Call uiApiHitter() before this method.");
		}

		// Step 1: Get the field API name from the UI label using the UI API data
		String fieldName = getFieldNameFromLabel(label);
		if (fieldName == null) {
			return null;
		}

		// Step 2: Use the field API name to perform a SOQL query and get the record ID
		String soql = String.format("SELECT Id FROM %s WHERE %s = '%s' LIMIT 1", objectApiName, fieldName, value);
		String encodedSoql = URLEncoder.encode(soql, "UTF-8");

		JSONObject response = HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);

		if (response != null && response.has("records") && response.getJSONArray("records").length() > 0) {
			return response.getJSONArray("records").getJSONObject(0).getString("Id");
		} else {
			System.out.println("FAIL: No record found with " + label + " = '" + value + "'");
			return null;
		}
	}

	/**
	 * Purpose: Finds the Salesforce field API name (e.g., 'Account.Name') from its UI label
	 * (e.g., 'Account Name') by efficiently parsing the UI API JSON response.
	 *
	 * @param label The UI label of the field (e.g., 'Account Name').
	 * @return The field API name (e.g., 'Account.Name'), or null if not found.
	 */
	public static String getFieldNameFromLabel(String label) throws Exception {
		if (uiapi_record_json == null || uiapi_record_json.isEmpty()) {
			throw new IllegalStateException("uiapi_record_json is not initialized. Call uiApiHitter() first.");
		}
		try {
			String sobjecttype = JsonPath.read(uiapi_record_json, "$..objectApiName").toString();
			String fieldNamePath = "$.layouts." + sobjecttype + ".compactLayoutable.$..[?(@.label == '" + label + "')].fieldName";
			Object result = JsonPath.read(uiapi_record_json, fieldNamePath);
			if (result instanceof List && !((List<?>) result).isEmpty()) {
				return ((List<?>) result).get(0).toString();
			} else {
				System.out.println("FAIL: Field API name for label '" + label + "' not found in UI API JSON.");
				return null;
			}
		} catch (com.jayway.jsonpath.PathNotFoundException e) {
			System.out.println("FAIL: JSONPath for label '" + label + "' not found: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Purpose: Retrieves a field's value from the UI API JSON response, which is more reliable
	 * and faster than trying to locate and scrape the value from the UI itself.
	 *
	 * @param label The UI label of the field.
	 * @return The value of the field as a String, or null if not found.
	 */
	public String getRecordViewValue(String label) throws Exception {
		if (uiapi_record_json == null || uiapi_record_json.isEmpty()) {
			throw new IllegalStateException("uiapi_record_json is not initialized. Call uiApiHitter() first.");
		}

		// JSONPath to find the value of a field based on its label in a 'View' layout
		String valuePath = "$..[?(@.label == '" + label + "')].value";

		try {
			Object result = JsonPath.read(uiapi_record_json, valuePath);
			if (result instanceof List && !((List<?>) result).isEmpty()) {
				Object value = ((List<?>) result).get(0);
				if (value != null) {
					return value.toString();
				}
			}
		} catch (com.jayway.jsonpath.PathNotFoundException e) {
			System.out.println("FAIL: Field with label '" + label + "' not found in UI API JSON.");
		}
		return null;
	}

	/**
	 * Purpose: Clicks a button based on its visible text label, handling a variety of button types.
	 *
	 * @param buttonText The text displayed on the button.
	 */
	public void clickButtonByLabel(String buttonText) {
		String xpath = String.format("//button[normalize-space(.)='%s']", buttonText);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
			WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
			SFClick(button);
			System.out.println("PASS: Clicked button with label: '" + buttonText + "'.");
		} catch (Exception e) {
			System.out.println("FAIL: Failed to click button '" + buttonText + "': " + e.getMessage());
			throw new RuntimeException("Could not find or click the button with label: " + buttonText, e);
		}
	}

	/**
	 * Purpose: Waits for a specific field element to become visible on the page
	 * using a dynamic locator based on the field's UI label.
	 *
	 * @param label The UI label of the field.
	 * @param timeoutInSeconds The maximum time to wait in seconds.
	 * @return True if the element is found, false otherwise.
	 */
	public boolean waitForFieldToAppear(String label, int timeoutInSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			WebElement element = getFieldElementByLabel(label);
			wait.until(ExpectedConditions.visibilityOf(element));
			System.out.println("PASS: Field '" + label + "' appeared on the page.");
			return true;
		} catch (Exception e) {
			System.out.println("FAIL: Field '" + label + "' did not appear on the page within " + timeoutInSeconds + " seconds.");
			return false;
		}
	}
	public boolean waitForElementToAppear(WebElement we, int timeoutInSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			wait.until(ExpectedConditions.visibilityOf(we));
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	public void assertElementIsVisible(String fieldLabel, int timeoutInSeconds) {
		boolean isVisible = waitForFieldToAppear(fieldLabel, timeoutInSeconds);
		Assert.assertTrue(isVisible, "Element with label '" + fieldLabel + "' is not visible on the page.");
		System.out.println("PASS: Assertion Passed: Element '" + fieldLabel + "' is visible.");
	}

	public void assertRecordExistsInDB(String objectApiName, String fieldLabel, String fieldValue) throws Exception {
		String recordId = getRecordIdByUiLabel(objectApiName, fieldLabel, fieldValue);
		Assert.assertNotNull(recordId, "Record with " + fieldLabel + " = '" + fieldValue + "' does not exist in the database.");
		System.out.println("PASS: Assertion Passed: Record with " + fieldLabel + " = '" + fieldValue + "' exists in the database with ID: " + recordId);
	}

	public void assertFieldLabelAndValue(String fieldLabel, String expectedValue) throws Exception {
		//  String actualValue = getRecordViewValue(fieldLabel);
		WebElement we = driver.findElement(By.xpath("//div[normalize-space()='"+ fieldLabel +"']//following-sibling::div[1]//lightning-formatted-text | //div[contains(@class,'active')]//div[normalize-space()='"+ fieldLabel +"']//following-sibling::div[1]//div[contains(@class,'recordTypeName')]/span | //div[contains(@class,'active')]//div[normalize-space()='"+ fieldLabel +"']//following-sibling::div[1]//lightning-formatted-address"));
		String actualValue = we.getText();
		Assert.assertEquals(actualValue, expectedValue, "Field '" + fieldLabel + "' value mismatch.");
		System.out.println("PASS: Assertion Passed: Field '" + fieldLabel + "' value is '" + actualValue + "'.");
	}

	public void assertFieldLabelAndMap(String fieldLabel, int timeoutInSeconds) throws Exception {
		WebElement we = driver.findElement(By.xpath("//div[normalize-space()='"+ fieldLabel +"']//following-sibling::div[1]//lightning-static-map"));

		boolean isVisible = waitForElementToAppear(we, timeoutInSeconds);
		Assert.assertTrue(isVisible, "Element with label '" + fieldLabel + "' Map is not visible on the page.");
		System.out.println("PASS: Assertion Passed: Element '" + fieldLabel + "' Map is visible.");
	}

	public void assertRecordViewValueContains(String fieldLabel, String partialValue) throws Exception {
		String actualValue = getRecordViewValue(fieldLabel);
		Assert.assertNotNull(actualValue, "Field '" + fieldLabel + "' value is null, cannot check for partial value.");
		Assert.assertTrue(actualValue.contains(partialValue), 
				"Field '" + fieldLabel + "' value '" + actualValue + "' does not contain '" + partialValue + "'.");
		System.out.println("PASS: Assertion Passed: Field '" + fieldLabel + "' value contains '" + partialValue + "'.");
	}

	public void assertToastMessageAppeared(String expectedMessage) {
		String xpath = String.format("//div[@class='slds-notify slds-notify_toast slds-theme_success']//span[contains(@class,'forceActionsText') and text()='%s']", expectedMessage);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
			System.out.println("PASS: Assertion Passed: Toast message appeared with expected text: '" + expectedMessage + "'.");
		} catch (Exception e) {
			Assert.fail("FAIL: Toast message with text '" + expectedMessage + "' did not appear or was not found.");
		}
	}

	public void assertRecordCountInListView(String objectApiName, int expectedCount, String... conditions) throws Exception {
		List<String> recordIds = getRecordIds(objectApiName, conditions);
		int actualCount = recordIds.size();
		Assert.assertEquals(actualCount, expectedCount, 
				"Record count mismatch for " + objectApiName + " with conditions. Expected: " + expectedCount + ", Actual: " + actualCount);
		System.out.println("PASS: Assertion Passed: Correct number of records found. Count: " + actualCount);
	}


	/**
	 * Asserts that a picklist field contains a specific value as an option.
	 * This assertion is performed by checking the picklist values in the UI API JSON response.
	 * @param fieldLabel The UI label of the picklist field.
	 * @param expectedValue The value expected to be present in the picklist.
	 */
	public void assertPicklistValueIsPresent(String fieldLabel, String expectedValue) throws Exception {
		if (uiapi_record_json == null || uiapi_record_json.isEmpty()) {
			throw new IllegalStateException("uiapi_record_json is not initialized. Call uiApiHitter() first.");
		}

		// JSONPath to get all picklist values for a given label.
		String path = String.format("$..[?(@.label == '%s')].picklistValues", fieldLabel);

		try {
			List<List<Object>> allValuesList = JsonPath.read(uiapi_record_json, path);
			if (allValuesList.isEmpty()) {
				Assert.fail("Picklist with label '" + fieldLabel + "' not found in UI API JSON.");
			}

			List<Object> picklistValues = allValuesList.get(0);
			boolean valueFound = false;

			for (Object item : picklistValues) {
				String value = JsonPath.read(item, "$.value");
				if (expectedValue.equals(value)) {
					valueFound = true;
					break;
				}
			}

			Assert.assertTrue(valueFound, "Value '" + expectedValue + "' not found in picklist '" + fieldLabel + "'.");
			System.out.println("PASS: Assertion Passed: Value '" + expectedValue + "' is present in picklist '" + fieldLabel + "'.");

		} catch (com.jayway.jsonpath.PathNotFoundException e) {
			Assert.fail("Path for picklist '" + fieldLabel + "' not found: " + e.getMessage());
		}
	}

	/**
	 * Asserts that a field with the given label is read-only on the page.
	 * This assertion is performed by checking the UI API response for the 'editableForUpdate' property.
	 * @param fieldLabel The UI label of the field to check.
	 */
	public void assertFieldIsReadOnly(String fieldLabel) throws Exception {
		if (uiapi_record_json == null || uiapi_record_json.isEmpty()) {
			throw new IllegalStateException("uiapi_record_json is not initialized. Call uiApiHitter() first.");
		}

		String path = "$..[?(@.label == '" + fieldLabel + "')].editableForUpdate";

		try {
			List<Boolean> results = JsonPath.read(uiapi_record_json, path);

			if (results.isEmpty()) {
				Assert.fail("Field with label '" + fieldLabel + "' not found in UI API JSON.");
			}

			boolean isEditable = results.get(0);
			Assert.assertFalse(isEditable, "Field '" + fieldLabel + "' is unexpectedly editable.");
			System.out.println("PASS: Assertion Passed: Field '" + fieldLabel + "' is read-only.");

		} catch (com.jayway.jsonpath.PathNotFoundException e) {
			Assert.fail("Path for field '" + fieldLabel + "' not found: " + e.getMessage());
		}
	}

	/**
	 * Asserts that a field with the given label is editable on the page.
	 * This assertion is performed by checking the UI API response for the 'editableForUpdate' property.
	 * @param fieldLabel The UI label of the field to check.
	 */
	public void assertFieldIsEditable(String fieldLabel) throws Exception {
		if (uiapi_record_json == null || uiapi_record_json.isEmpty()) {
			throw new IllegalStateException("uiapi_record_json is not initialized. Call uiApiHitter() first.");
		}

		String path = "$..[?(@.label == '" + fieldLabel + "')].editableForUpdate";

		try {
			List<Boolean> results = JsonPath.read(uiapi_record_json, path);

			if (results.isEmpty()) {
				Assert.fail("Field with label '" + fieldLabel + "' not found in UI API JSON.");
			}

			boolean isEditable = results.get(0);
			Assert.assertTrue(isEditable, "Field '" + fieldLabel + "' is not editable as expected.");
			System.out.println("PASS: Assertion Passed: Field '" + fieldLabel + "' is editable.");

		} catch (com.jayway.jsonpath.PathNotFoundException e) {
			Assert.fail("Path for field '" + fieldLabel + "' not found: " + e.getMessage());
		}
	}

	/**
	 * Asserts that a field with the given label is visually marked as required on the UI.
	 * This is verified by checking for the red asterisk next to the label.
	 * @param fieldLabel The UI label of the field to check.
	 */
	public void assertFieldIsRequired(String fieldLabel) {
		String xpath = String.format("//label[text()='%s']//span[@class='slds-required']", fieldLabel);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
			System.out.println("PASS: Assertion Passed: Field '" + fieldLabel + "' is marked as required.");
		} catch (Exception e) {
			Assert.fail("FAIL: Field '" + fieldLabel + "' is not marked as required.");
		}
	}

	/**
	 * Asserts that a specified data value is present within a table or list view on the page.
	 * This method is ideal for validating newly created records or filtered list views.
	 * @param value The text value to search for in the table.
	 */
	public void assertDataIsPopulatedInTable(String value) {
		// This XPath is generic and looks for the text anywhere inside a table or div with slds-table class
		String xpath = String.format("//table[contains(@class,'slds-table')]//td[normalize-space(.)='%s'] | //div[contains(@class,'slds-table')]//span[normalize-space(.)='%s']", value, value);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
			System.out.println("PASS: Assertion Passed: Value '" + value + "' found in the table.");
		} catch (Exception e) {
			Assert.fail("FAIL: Value '" + value + "' not found in any table on the page.");
		}
	}

	/**
	 * Asserts the number of records in a related list by reading the count from the UI.
	 * This is faster than a SOQL query and validates the UI state directly.
	 * @param relatedListLabel The label of the related list (e.g., "Contacts").
	 * @param expectedCount The expected number of records in the list.
	 */
	public void assertRelatedListCount(String relatedListLabel, int expectedCount) {
		String xpath = String.format("//a[contains(@class,'tabHeader')]/span[normalize-space()='%s']/following-sibling::span", relatedListLabel);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			WebElement countElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
			String countText = countElement.getText().trim().replaceAll("[^0-9]", "");
			int actualCount = Integer.parseInt(countText);

			Assert.assertEquals(actualCount, expectedCount, 
					"Related list count mismatch for '" + relatedListLabel + "'. Expected: " + expectedCount + ", Actual: " + actualCount);

			System.out.println("PASS: Assertion Passed: Related list '" + relatedListLabel + "' has the correct count of " + actualCount + ".");
		} catch (Exception e) {
			Assert.fail("FAIL: Related list count assertion failed for '" + relatedListLabel + "': " + e.getMessage());
		}
	}

	/**
	 * Asserts that a section or component on the page is either visible or hidden.
	 * This is useful for validating dynamic page layouts.
	 * @param sectionLabel The label of the section or component.
	 * @param shouldBeVisible True to assert the section is visible, false to assert it's hidden.
	 */
	public void assertSectionVisibility(String sectionLabel, boolean shouldBeVisible) {
		String xpath = String.format("//div[@class='slds-card__header']//h2/span[@title='%s']", sectionLabel);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			if (shouldBeVisible) {
				wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
				System.out.println("PASS: Assertion Passed: Section '" + sectionLabel + "' is visible.");
			} else {
				wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath(xpath)));
				System.out.println("PASS: Assertion Passed: Section '" + sectionLabel + "' is not visible.");
			}
		} catch (Exception e) {
			if (shouldBeVisible) {
				Assert.fail("FAIL: Section '" + sectionLabel + "' is not visible as expected.");
			} else {
				Assert.fail("FAIL: Section '" + sectionLabel + "' is unexpectedly visible.");
			}
		}
	}

	/**
	 * Asserts that a button or input field is disabled on the UI.
	 * This is crucial for verifying UI state changes based on business rules.
	 * @param elementLabelOrText The label or text of the element to check.
	 */
	public void assertElementIsDisabled(String elementLabelOrText) {
		String xpath = String.format("//button[normalize-space(.)='%s' and @disabled] | //input[@id=string(//label[text()='%s']/@for) and @disabled]", elementLabelOrText, elementLabelOrText);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
			wait.until(ExpectedConditions.attributeToBe(By.xpath(xpath), "disabled", "true"));
			System.out.println("PASS: Assertion Passed: Element '" + elementLabelOrText + "' is disabled.");
		} catch (Exception e) {
			Assert.fail("FAIL: Element '" + elementLabelOrText + "' is unexpectedly enabled or not found.");
		}
	}

}
