package utils;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.jayway.jsonpath.JsonPath;

/**
 * @author Robin
 * @date 04/02/2021
 * @purpose: This class gets the UI layout from UI API and tries to make the
 *           xpath for all the elements 👼
 * @see: A lot of these methods are implemented using JSONPATH to parse the
 *       response we get from UI API
 */
public class SFPageBase2 extends PageBase {

	// ============================================================
	// --- Constructor & Variables Section ---
	// ============================================================
	protected static String uiapi_record_json;
	private static ArrayList<String> listoflabels;
	protected static HashMap<String, String> labelandtype;
	private String extractedObjectName;
    private static final int DEFAULT_WAIT_SECONDS = 30;

	public SFPageBase2(WebDriver driver) {
		super(driver);
	}

	// ============================================================
	// --- Authentication & Navigation Section ---
	// ============================================================
	public void loginToSalesforce(String loginUrl, String grantService, String clientId, String clientSecret,
			String username, String password) {
		HTTPClientWrapper.SFLogin_API(loginUrl, grantService, clientId, clientSecret, username, password);
	}

	public void waitForSFPagetoLoad() throws InterruptedException {
		// TODO: Replace Thread.sleep with WebDriverWait or FluentWait for better
		// performance and stability.
		Thread.sleep(3000);
		try {
			WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(50));

			ExpectedCondition<Boolean> jsLoad = d -> ((JavascriptExecutor) d)
					.executeScript("return document.readyState").toString().equals("complete");

			ExpectedCondition<Boolean> aurascriptLoad = d -> {
				String WAIT_FOR_AURA_SCRIPT = "return (typeof $A !== 'undefined' && $A && $A.metricsService.getCurrentPageTransaction().config.context.ept > 0)";
				String EPT_COUNTER_SCRIPT = "return ($A.metricsService.getCurrentPageTransaction().config.context.ept)";
				Boolean result = (Boolean) ((JavascriptExecutor) d).executeScript(WAIT_FOR_AURA_SCRIPT);
				if (Boolean.TRUE.equals(result)) {
					System.out.println("Experienced Page Load time: "
							+ ((JavascriptExecutor) d).executeScript(EPT_COUNTER_SCRIPT));
					return true;
				}
				return false;
			};

			if (wait1.until(jsLoad) && wait1.until(aurascriptLoad)) {
				System.out.println("Page load complete");
			} else {
				Thread.sleep(2000);
			}
		} catch (Exception e) {
			System.out.println("Exception in waitForSFPagetoLoad: " + e.getMessage());
			Thread.sleep(5000);
		}
	}

	public String getRecordUrl(String objectApiName, String recordId) {
		if (recordId == null || recordId.isEmpty()) {
			throw new IllegalArgumentException("recordId cannot be null/empty");
		}
		String base = utils.HTTPClientWrapper.getLoginInstanceUrl();
		if (base == null || base.isEmpty()) {
			throw new IllegalStateException("instanceUrl not initialized. Call SFLogin_API first.");
		}

		base = base.replaceAll("/+$$", "");
		return String.format("%s/lightning/r/%s/%s/view", base, objectApiName, recordId);
	}

	public void NavigateToRecord(String objectApiName, String recordId) {
		String url = getRecordUrl(objectApiName, recordId);
		System.out.println(url);
		try {
			driver.get(url);
			waitForSFPagetoLoad();
		} catch (Exception e) {
			System.out.println("Failed to open Lightning URL: " + e.getMessage());
			try {
				String base = utils.HTTPClientWrapper.getLoginInstanceUrl().replaceAll("/+$$", "");
				String fallback = base + "/" + recordId;
				System.out.println("Attempting fallback URL: " + fallback);
				driver.get(fallback);
				waitForSFPagetoLoad();
			} catch (Exception ex) {
				throw new RuntimeException("Could not navigate to record: " + recordId, ex);
			}
		}
	}

	public String getURL(String appname) {
		GetSFApps getSfApps = new GetSFApps();
		return getSfApps.getAppNavURL(appname);
	}

	public void appLauncher(String appname) throws InterruptedException {
		String accountappurl = getURL(appname);
		String cleanurl = accountappurl.replace("[\"", "").replace("\"]", "");
		openHomepage(cleanurl + "?eptVisible=1");
		waitForSFPagetoLoad();
	}

	// ============================================================
	// --- UI API Data Handling Section ---
	// ============================================================
	
	
	public void uiApiHitter(String recordId) throws Exception {
		uiapi_record_json = HTTPClientWrapper.runGetRequest("/ui-api/record-ui/" + recordId + "?formFactor=Large&modes=View,Edit").toString();

        try {
            
            JSONArray extractedObjectNames = new org.json.JSONArray(JsonPath.read(uiapi_record_json, "$..objectApiName").toString());
            extractedObjectName = extractedObjectNames.getString(0);
        } catch (Exception e) {
            // ignore and fallback
        }

        if (extractedObjectName == null || extractedObjectName.isEmpty()) {
            String prefix = recordId.substring(0, 3);
            extractedObjectName = ObjectPrefixCache.getObjectName(prefix);
            if (extractedObjectName == null) {
                throw new RuntimeException("Cannot resolve object name for recordId: " + recordId);
            }
            System.out.println(" Fallback used: Object name from prefix = " + extractedObjectName);
        } else {
            System.out.println(" Object name extracted from response: " + extractedObjectName);
        }
    }
	

	public static void sectionGetter() throws Exception {
		String apipath = "$..objectApiName";
		String sobjecttype = JsonPath.read(uiapi_record_json, apipath).toString();
		String sectionspath = "$.layouts." + sobjecttype + "..sections";
		JSONArray sectionsparent = new org.json.JSONArray(JsonPath.read(uiapi_record_json, sectionspath).toString());
		JSONArray sectionsarray = (JSONArray) sectionsparent.get(0);
		System.out.println("Count of Sections is : " + sectionsarray.length());
	}

	public static void labelGetter() throws Exception {
		String labelpath = "$..[?(@.editableForUpdate == true)].layoutComponents..label";
		JSONArray listofduplicatelabels = new org.json.JSONArray(
				JsonPath.read(uiapi_record_json, labelpath).toString());
		LinkedHashSet<String> labels = new LinkedHashSet<>();
		for (int i = 0; i < listofduplicatelabels.length(); i++) {
			labels.add((String) listofduplicatelabels.get(i));
		}
		listoflabels = new ArrayList<>();
		listoflabels.addAll(labels);
	}

	public static void dataTypeGetter() throws Exception {
		labelandtype = new HashMap<>();
		for (String label : listoflabels) {
			String typepath = "$..[?(@.label =='" + label + "')].dataType";
			Object result = JsonPath.read(uiapi_record_json, typepath);
			String datatype = ((List<?>) result).get(0).toString();
			labelandtype.put(label, datatype);
		}
	}
	
	public void loadFields() throws Exception {
        if (extractedObjectName == null || extractedObjectName.isEmpty()) {
            throw new IllegalStateException("Object name not resolved. Call uiApiHitter first.");
        }
        Map<String, MetadataCache.FieldInfo> fields = MetadataCache.getAllFields(extractedObjectName);
        listoflabels = new ArrayList<>(fields.keySet());
        labelandtype = new HashMap<>();
        for (String label : fields.keySet()) {
            labelandtype.put(label, fields.get(label).dataType);
        }
    }

	public void uiApiParser( String recordid) throws Exception {
		uiApiHitter(recordid);
		sectionGetter();
		loadFields();
	}

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
		return null;
	}

	public static String getFieldNameFromLabel(String label) throws Exception {
		String sobjecttype = JsonPath.read(uiapi_record_json, "$..objectApiName").toString();
		String fieldNamePath = "$.layouts." + sobjecttype + ".compactLayoutable.$..[?(@.label == '" + label
				+ "')].fieldName";
		Object result = JsonPath.read(uiapi_record_json, fieldNamePath);
		if (result instanceof List && !((List<?>) result).isEmpty()) {
			return ((List<?>) result).get(0).toString();
		}
		return null;
	}

	public String getRecordViewValue(String label) throws Exception {
		String valuePath = "$..[?(@.label == '" + label + "')].value";
		Object result = JsonPath.read(uiapi_record_json, valuePath);
		if (result instanceof List && !((List<?>) result).isEmpty()) {
			Object value = ((List<?>) result).get(0);
			if (value != null)
				return value.toString();
		}
		return null;
	}

	// ============================================================
	// --- Record ID & SOQL Query Section ---
	// ============================================================
	public String getRecordIdFromUiLabel_Optimized(String objectApiName, String uiLabel, String fieldValue)
			throws Exception {
		String fieldName = getFieldNameFromLabel_SOBJECT_DESCRIBE(objectApiName, uiLabel);
		if (fieldName == null)
			return null;
		String soql = String.format("SELECT Id FROM %s WHERE %s = '%s' LIMIT 1", objectApiName, fieldName, fieldValue);
		String encodedSoql = URLEncoder.encode(soql, "UTF-8");
		JSONObject response = HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);
		if (response != null && response.has("records") && response.getJSONArray("records").length() > 0) {
			return response.getJSONArray("records").getJSONObject(0).getString("Id");
		}
		return null;
	}

	public String getRecordId(String objectApiName, String fieldName, String fieldValue) {
		return getRecordId(objectApiName, new String[] { fieldName, fieldValue });
	}

	public String getRecordId(String objectApiName, String... conditions) {
		try {
			List<String> ids = getRecordIds(objectApiName, conditions);
			return ids.isEmpty() ? null : ids.get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public List<String> getRecordIds(String objectApiName, String... conditions) {
		List<String> ids = new ArrayList<>();
		try {
			if (conditions.length % 2 != 0)
				throw new IllegalArgumentException("Conditions must be in field-value pairs");
			StringBuilder whereClause = new StringBuilder();
			for (int i = 0; i < conditions.length; i += 2) {
				if (whereClause.length() > 0)
					whereClause.append(" AND ");
				whereClause.append(conditions[i]).append(" = '").append(conditions[i + 1]).append("'");
			}
			String soql = "SELECT Id FROM " + objectApiName + " WHERE " + whereClause;
			String encodedSoql = URLEncoder.encode(soql, "UTF-8");
			JSONObject response = HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);
			if (response != null && response.has("records")) {
				JSONArray records = response.getJSONArray("records");
				for (int i = 0; i < records.length(); i++)
					ids.add(records.getJSONObject(i).getString("Id"));
			}
		} catch (Exception e) {
		}
		return ids;
	}

	public String getRecordIdByUiLabel(String objectApiName, String label, String value) throws Exception {
		String fieldName = getFieldNameFromLabel(label);
		if (fieldName == null)
			return null;
		String soql = String.format("SELECT Id FROM %s WHERE %s = '%s' LIMIT 1", objectApiName, fieldName, value);
		String encodedSoql = URLEncoder.encode(soql, "UTF-8");
		JSONObject response = HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);
		if (response != null && response.has("records") && response.getJSONArray("records").length() > 0) {
			return response.getJSONArray("records").getJSONObject(0).getString("Id");
		}
		return null;
	}

	// ============================================================
	// --- Web Element Interaction Section ---
	// ============================================================
	public void clickEditByFieldLabel(String fieldLabel) throws InterruptedException {
		Thread.sleep(5000);
		String xpath = String.format("//div[contains(@class,'active')]//button[@title='Edit %s']", fieldLabel);
		SFClick(driver.findElement(By.xpath(xpath)));
	}

	public void clickOnGlobalSearchTextbox(String placeholderText) {
		String xpath = String.format("//button[@aria-label='%s']", placeholderText);
		SFClick(driver.findElement(By.xpath(xpath)));
	}

	public void clickSelectAllDownArrow(String placeholder) {
		String xpath = String.format(
				"//input[@data-value='%s']/ancestor::div[contains(@class,'slds-combobox__form-element')]//lightning-icon[@icon-name='utility:down']",
				placeholder);
		SFClick(driver.findElement(By.xpath(xpath)));
	}

	public void clickListboxOption(String optionText) {
		String xpath = String.format("//li[contains(@class,'slds-listbox__item')]//span[@title='%s']", optionText);
		SFClick(driver.findElement(By.xpath(xpath)));
	}

	public void selectFirstSuggestedValue() {
		WebElement firstVisibleSuggestion = driver.findElement(By.xpath(
				"(//search_dialog-instant-results-list//search_dialog-instant-result-item[.//span[normalize-space()!='']])[1]"));
		SFClick(firstVisibleSuggestion);
	}

	// Get field element by label
	public WebElement getFieldElementByLabel(String label) throws Exception {
	    String xpath = getFieldXPath(label);
	    return findElementWithWait(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
	}

	// Get error message element by label
	public WebElement getFieldElementErrorByLabel(String label) throws Exception {
	    String xpath = "//div[contains(@class,'active')]//div[@id=string(" +
	            "//input[@id=string(//label[text()='" + label + "']/@for)]/@aria-describedby | " +
	            "//textarea[@id=string(//label[text()='" + label + "']/@for)]/@aria-describedby | " +
	            "//button[@id=string(//label[text()='" + label + "']/@for)]/@aria-describedby" +
	            ")]";
	    return findElementWithWait(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
	}

	public void clickButtonByLabel(String buttonText) {
		String xpath = String.format("//div[contains(@class,\"active\")]//button[normalize-space(.)='%s']", buttonText);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
			WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
			SFClick(button);
		} catch (Exception e) {
			throw new RuntimeException("Could not click button: " + buttonText, e);
		}
	}

	// ============================================================
	// --- Form Handling Section ---
	// ============================================================
	// Fill form field by label and value
	public void formValueFiller(String label, String targetValue) throws Exception {
	    WebElement field = getFieldElementByLabel(label);
	    scrollIntoView(field);
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
	        case "TextArea":
	            clearAndSendKeys(field, targetValue);
	            break;
	        case "Picklist":
	            waitAndClick(field);
	            field.sendKeys(targetValue);
	            Thread.sleep(2000);
	            field.sendKeys(Keys.ENTER);
	            break;
	        case "MultiPicklist":
	            fillMultiPicklist(label, targetValue);
	            break;
	        case "Reference":
	            field.sendKeys(targetValue);
	            field.sendKeys(Keys.ARROW_DOWN);
	            Thread.sleep(2000);
	            field.sendKeys(Keys.ENTER);
	            break;
	        default:
	            throw new Exception("Unsupported field type: " + type);
	    }
	}

	// Overloaded version with custom wait time
	public void formValueFiller(String label, String targetValue, int waitInSeconds) throws Exception {
	    WebElement field = findElementWithWait(By.xpath(getFieldXPath(label)), waitInSeconds);
	    scrollIntoView(field);
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
	        case "TextArea":
	            clearAndSendKeys(field, targetValue);
	            break;
	        case "Picklist":
	            waitAndClick(field);
	            field.sendKeys(targetValue);
	            Thread.sleep(2000);
	            field.sendKeys(Keys.ENTER);
	            break;
	        case "MultiPicklist":
	            fillMultiPicklist(label, targetValue);
	            break;
	        case "Reference":
	            field.sendKeys(targetValue);
	            field.sendKeys(Keys.ARROW_DOWN);
	            Thread.sleep(2000);
	            field.sendKeys(Keys.ENTER);
	            break;
	        default:
	            throw new Exception("Unsupported field type: " + type);
	    }
	}

	// Clear input by label
	public void formValueFillerClearInput(String label) throws Exception {
	    WebElement field = getFieldElementByLabel(label);
	    scrollIntoView(field);
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
	        case "TextArea":
	        case "Reference":
	            field.clear();
	            break;
	        case "Picklist":
	            waitAndClick(field);
	            field.clear();
	            break;
	        case "MultiPicklist":
	            clearMultiPicklist(label);
	            break;
	        default:
	            throw new Exception("Unsupported field type: " + type);
	    }
	}
	
	// ===============================
	// Helper Methods
	// ===============================

	private String getFieldXPath(String label) throws Exception {
	    String type = labelandtype.get(label);
	    if (type == null) {
	        throw new Exception("Label not found: " + label);
	    }
	    switch (type) {
	        case "Reference":
	        case "String":
	        case "Url":
	        case "Int":
	        case "Phone":
	        case "Currency":
	        case "Double":
	        case "Date":
	        case "Boolean":
	        case "Email":
	            return "//div[contains(@class,'active')]//input[@id=string(//label[text()='" + label + "']/@for)]";
	        case "TextArea":
	            return "//div[contains(@class,'active')]//textarea[@id=string(//label[text()='" + label + "']/@for)]";
	        case "Picklist":
	            return "//div[contains(@class,'active')]//button[@id=string(//label[text()='" + label + "']/@for)] | " +
	                   "//div[contains(@class,'active')]//input[@id=string(//label[text()='" + label + "']/@for)]";
	        case "MultiPicklist":
	            return "//div[contains(@class,'active')]//div[contains(@class,'slds-form-element__label') and text()='" + label + "']/following-sibling::div//div[contains(@class,'slds-dueling-list')]";
	        default:
	            throw new Exception("Unsupported field type: " + type);
	    }
	}

	private WebElement findElementWithWait(By locator, int waitInSeconds) {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(waitInSeconds));
	    return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
	}


	public void waitAndClick(WebElement element) {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
	    SFClick(wait.until(ExpectedConditions.elementToBeClickable(element)));
	}

	private void clearAndSendKeys(WebElement element, String value) throws InterruptedException {
	    element.clear();
	    Thread.sleep(500);
	    element.sendKeys(value);
	}

	// Filling MultiPicklist
	private void fillMultiPicklist(String label, String targetValue) throws Exception {
	    WebElement container = getFieldElementByLabel(label);
	    WebElement availableList = container.findElement(By.xpath(".//ul[@data-source-list]"));
	    WebElement moveButton = container.findElement(By.xpath(".//button[@title='Move to Chosen']"));
	    String values = targetValue.replaceAll("[\\[\\]]", ""); // Remove brackets
	    String[] items = values.split(",\\s*");

	    for (String item : items) {
	        WebElement option = findOptionInListWithScroll(availableList, item);
	        scrollIntoView(option);
	        option.click();
	        Thread.sleep(5000);
	    }
	    waitAndClick(moveButton);
	    Thread.sleep(1000);
	}

	// Clearing MultiPicklist
	private void clearMultiPicklist(String label) throws Exception {
	    WebElement container = getFieldElementByLabel(label);
	    WebElement selectedList = container.findElement(By.xpath(".//ul[@data-selected-list]"));
	    WebElement moveButton = container.findElement(By.xpath(".//button[@title='Move to Available']"));

	    List<WebElement> options = selectedList.findElements(By.xpath(".//div[@role='option']"));
	    for (WebElement option : options) {
	        scrollIntoView(option);
	        option.click();
	        Thread.sleep(500);
	    }
	    if (!options.isEmpty()) {
	        moveButton.click();
	        Thread.sleep(1000);
	    }
	}

	// Scroll and find option in list
	private WebElement findOptionInListWithScroll(WebElement listElement, String optionText) throws Exception {
	    List<WebElement> options = listElement.findElements(By.xpath(".//div[@role='option']"));
	    for (WebElement option : options) {
	        if (option.getText().trim().equals(optionText)) {
	            return option;
	        }
	    }
	    throw new Exception("Option not found in list: " + optionText);
	}
	
	public void scrollList(WebElement listContainer) {
	    JavascriptExecutor js = (JavascriptExecutor) driver;
	    js.executeScript("arguments[0].scrollTop = arguments[0].scrollTop + 100;", listContainer);
	}
	
	public void scrollIntoView(WebElement element) {
	    JavascriptExecutor js = (JavascriptExecutor) driver;
	    js.executeScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", element);
	}

	// ============================================================
	// --- Assertions Section ---
	// ============================================================
	public void assertFormValueByLabel(String label, String expectedValue) throws Exception {
		WebElement we = getFieldElementByLabel(label);
		String actualValue = "input".equalsIgnoreCase(we.getTagName())? we.getAttribute("value"):we.getText();
		Assert.assertEquals(actualValue, expectedValue, "Field '" + label + "' value mismatch.");
	}
	
	public void assertFormErrorValueByLabel(String label, String expectedValue) throws Exception {
		WebElement we = getFieldElementErrorByLabel(label);
		String actualValue = "input".equalsIgnoreCase(we.getTagName())? we.getAttribute("value"):we.getText();
		Assert.assertTrue(actualValue.contains(expectedValue), "Field '" + label + "' value mismatch.");
	}

	public void assertFormValueSnags(String label, String expectedValue) throws Exception {
		WebElement we = driver.findElement(
				By.xpath("//ul[@class='errorsList slds-list_dotted slds-m-left_medium']/li/a[text()='" + label + "']"));
		Assert.assertEquals(we.getText(), expectedValue);
	}

	public static void verifyRequiredFields(String testdatajson, String objname) {
		String valuename = objname + "Name";
		String isrequiredexpected = readJsonFile(testdatajson, "$." + valuename + ".isRequired");
		String objjson = HTTPClientWrapper.runGetRequest("/sobjects/" + objname + "/describe/layouts/").toString();
		String jsonpath = "$..[?(@.label==\"Account Name\")]..required";
		String isrequiredactual = JsonPath.read(objjson, jsonpath).toString();
		Assert.assertTrue(isrequiredactual.contains(isrequiredexpected),
				"Required field validation failed for object: " + objname);
	}

	public void assertElementIsVisible(String fieldLabel, int timeoutInSeconds) {
		try {
			WebElement element = getFieldElementByLabel(fieldLabel);
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			wait.until(ExpectedConditions.visibilityOf(element));
			Assert.assertTrue(element.isDisplayed(), "Element with label '" + fieldLabel + "' is not visible.");
		} catch (Exception e) {
			Assert.fail("Element with label '" + fieldLabel + "' is not visible: " + e.getMessage());
		}
	}

	public void assertRecordExistsInDB(String objectApiName, String fieldLabel, String fieldValue) throws Exception {
		String recordId = getRecordIdByUiLabel(objectApiName, fieldLabel, fieldValue);
		Assert.assertNotNull(recordId, "Record with " + fieldLabel + " = '" + fieldValue + "' does not exist in DB.");
	}

	public void assertFieldLabelAndValue(String fieldLabel, String expectedValue) throws Exception {
		WebElement we = driver.findElement(By.xpath("//div[normalize-space()='" + fieldLabel
				+ "']//following-sibling::div[1]//lightning-formatted-text | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel
				+ "']//following-sibling::div[1]//div[contains(@class,'recordTypeName')]/span | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel
				+ "']//following-sibling::div[1]//lightning-formatted-address | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel + "']//following-sibling::div[1]//lightning-formatted-url"));
		String actualValue = we.getText();
		Assert.assertEquals(actualValue, expectedValue, "Field '" + fieldLabel + "' value mismatch.");
	}

	public void assertFieldLabelAndMap(String fieldLabel, int timeoutInSeconds) throws Exception {
		WebElement we = driver.findElement(By.xpath(
				"//div[normalize-space()='" + fieldLabel + "']//following-sibling::div[1]//lightning-static-map"));
		boolean isVisible = waitForElementToAppear(we, timeoutInSeconds);
		Assert.assertTrue(isVisible, "Element with label '" + fieldLabel + "' Map is not visible on the page.");
	}

	public void assertRecordViewValueContains(String fieldLabel, String partialValue) throws Exception {
		String actualValue = getRecordViewValue(fieldLabel);
		Assert.assertNotNull(actualValue);
		Assert.assertTrue(actualValue.contains(partialValue));
	}

	public void assertToastMessageAppeared(String expectedMessage) {
		String xpath = String.format(
				"//div[@class='slds-notify slds-notify_toast slds-theme_success']//span[contains(@class,'forceActionsText') and text()='%s']",
				expectedMessage);
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
		} catch (Exception e) {
			Assert.fail("Toast message did not appear: " + expectedMessage);
		}
	}

	public void assertRecordCountInListView(String objectApiName, int expectedCount, String... conditions)
			throws Exception {
		List<String> recordIds = getRecordIds(objectApiName, conditions);
		Assert.assertEquals(recordIds.size(), expectedCount);
	}

	public void assertPicklistValueIsPresent(String fieldLabel, String expectedValue) throws Exception {
		String path = String.format("$..[?(@.label == '%s')].picklistValues", fieldLabel);
		List<List<Object>> allValuesList = JsonPath.read(uiapi_record_json, path);
		if (allValuesList.isEmpty())
			Assert.fail("Picklist not found: " + fieldLabel);
		boolean valueFound = false;
		for (Object item : allValuesList.get(0)) {
			String value = JsonPath.read(item, "$.value");
			if (expectedValue.equals(value)) {
				valueFound = true;
				break;
			}
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

	// ============================================================
	// --- Wait Utilities Section ---
	// ============================================================

	/**
	 * Wait for an element to appear within timeout. TODO: Optimize by adding
	 * polling interval and exception ignoring.
	 */
	public boolean waitForElementToAppear(WebElement element, int timeoutInSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			wait.until(ExpectedConditions.visibilityOf(element));
			return element.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for an element to disappear. TODO: Can be optimized with FluentWait to
	 * better handle stale elements.
	 */
	public boolean waitForElementToDisappear(By locator, int timeoutInSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Wait for an element to be clickable. TODO: Replace usages of Thread.sleep
	 * with this method wherever possible.
	 */
	public WebElement waitForElementToBeClickable(By locator, int timeoutInSeconds) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
		return wait.until(ExpectedConditions.elementToBeClickable(locator));
	}

	/**
	 * Wait for presence of element in DOM (not necessarily visible).
	 */
	public WebElement waitForPresenceOfElement(By locator, int timeoutInSeconds) {
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
		return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
	}

	/**
	 * Wait for text to be present in element.
	 */
	public boolean waitForTextInElement(WebElement element, String text, int timeoutInSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			return wait.until(ExpectedConditions.textToBePresentInElement(element, text));
		} catch (Exception e) {
			return false;
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
	
	// ============================================================
	// --- Methods TO Deprecate / Improve ---
	// ============================================================
	
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
	
}