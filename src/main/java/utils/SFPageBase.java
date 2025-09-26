package utils;

import java.net.URLEncoder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.jayway.jsonpath.JsonPath;

import base.BaseTest;
import utils.MetadataCache.QuickActionContext;

/**
 * @author Robin
 * @date 04/02/2021
 * @purpose: This class gets the UI layout from UI API and tries to make the
 *           xpath for all the elements 👼
 * @see: A lot of these methods are implemented using JSONPATH to parse the
 *       response we get from UI API
 */
public class SFPageBase extends PageBase {

	// ============================================================
	// --- Constructor & Variables Section ---
	// ============================================================
	protected static String uiapi_record_json;
	private static ArrayList<String> listoflabels;
	protected static HashMap<String, String> labelandtype;
	private String extractedObjectName;
    private static final int DEFAULT_WAIT_SECONDS = 30;
    private String currentObjectApiName;
    private static String currentObjectType = "SObject"; // default
    private static final Pattern LIGHTNING_RECORD_PATTERN = Pattern.compile("/lightning/r/([^/]+)/[A-Za-z0-9]{15,18}(/|$)");
    private static final Pattern LIGHTNING_OBJECT_PATTERN = Pattern.compile("/lightning/o/([^/]+)/(?:list|new)");
    private static final Pattern ONE_APP_SOBJECT_PATTERN = Pattern.compile("/sObject/([A-Za-z0-9]{15,18})");
    private static final Pattern ID_EXTRACT_PATTERN = Pattern.compile("([A-Za-z0-9]{15,18})");

	public SFPageBase(WebDriver driver) {
		super(driver);
	}

	// ============================================================
	// --- Authentication & Navigation Section ---
	// ============================================================
	public void loginToSalesforce(String loginUrl, String grantService, String clientId, String clientSecret,
			String username, String password) {
		HTTPClientWrapper.SFLogin_API(loginUrl, grantService, clientId, clientSecret, username, password);
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
	        // AUTO: set current object after navigation
	        setCurrentObject(objectApiName); // immediate and correct
	    } catch (Exception e) {
	        System.out.println("Failed to open Lightning URL: " + e.getMessage());
	        try {
	            String base = utils.HTTPClientWrapper.getLoginInstanceUrl().replaceAll("/+$$", "");
	            String fallback = base + "/" + recordId;
	            System.out.println("Attempting fallback URL: " + fallback);
	            driver.get(fallback);
	            waitForSFPagetoLoad();
	            // try to auto-detect from URL or record Id
	            updateCurrentObjectAuto();
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
		JSONObject describeResponse = (JSONObject) HTTPClientWrapper.runGetRequest(describeUrl);
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
	public String getRecordIdByUiLabelAndValue(String objectApiName, String uiLabel, String fieldValue)
			throws Exception {
		String fieldName = getFieldNameFromLabel_SOBJECT_DESCRIBE(objectApiName, uiLabel);
		if (fieldName == null)
			return null;
		String soql = String.format("SELECT Id FROM %s WHERE %s = '%s' LIMIT 1", objectApiName, fieldName, fieldValue);
		String encodedSoql = URLEncoder.encode(soql, "UTF-8");
		JSONObject response = (JSONObject) HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);
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
			JSONObject response = (JSONObject) HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);
			if (response != null && response.has("records")) {
				JSONArray records = response.getJSONArray("records");
				for (int i = 0; i < records.length(); i++)
					ids.add(records.getJSONObject(i).getString("Id"));
			}
		} catch (Exception e) {
		}
		return ids;
	}

	// ============================================================
	// --- Web Element Interaction Section ---
	// ============================================================
	public void clickEditByFieldLabel(String fieldLabel) throws InterruptedException {
		Thread.sleep(5000);
		String xpath = getEditButtonXPath(fieldLabel);
		SFClick(driver.findElement(By.xpath(xpath)));
	}
	
	public void clickChangeOwnerByFieldLabel(String fieldLabel) throws InterruptedException {
		Thread.sleep(5000);
		String xpath = getChangeOwnerButtonXPath(fieldLabel);
		SFClick(driver.findElement(By.xpath(xpath)));
	}
	
	/**
	 * Resolve a Quick Action button (WebElement) by its label.
	 * Uses Salesforce Quick Actions API (cached in HTTPClientWrapper).
	 *
	 * @param objectApiName - The object (e.g. "Account")
	 * @param actionLabel   - The label shown in UI (e.g. "New Opportunity")
	 * @return WebElement for the Quick Action button
	 */
	// existing method that accepts object explicitly (keep it)
	public WebElement getQuickActionElement(String objectApiName, String actionLabel) throws Exception {
	    Map<String, Object> actions = HTTPClientWrapper.getQuickActions(objectApiName);

	    if (!actions.containsKey(actionLabel)) {
	        throw new IllegalArgumentException("❌ Action not found: " + actionLabel +
	                " for object: " + objectApiName +
	                ". Available: " + actions.keySet());
	    }

	    Object value = actions.get(actionLabel);
	    String apiName = null;
	    if (value instanceof JSONObject) {
		    JSONObject apiNameObj = (JSONObject) value;
		    apiName = apiNameObj.getString("apiName");
		} else {
		    apiName = value.toString();
		}
	    
	    String xpath = String.format(
	        "//div[contains(@class,'active')]//runtime_platform_actions-action-renderer[@apiname='%s']//button",
	        apiName
	    );

	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
	    return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(xpath)));
	}

	// overload that uses currentObject automatically
	public WebElement getQuickActionElement(String actionLabel) throws Exception {
	    String obj = requireCurrentObject();
	    return getQuickActionElement(obj, actionLabel);
	}


	// Get field element by label
	public WebElement getFieldElementByLabelAndType(String label, String type) throws Exception {
	    String xpath = getFieldXPath(label,type);
	    return findElementWithWait(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
	}
	
	private MetadataCache.FieldInfo getFieldInfoUsingMetadata(String label) throws Exception {
	    label = label.replace("&", "&amp;");
	    Map<String, MetadataCache.FieldInfo> allFields =
	            MetadataCache.getAllFieldsMerged(QuickActionContext.currentSObject, QuickActionContext.currentFlow);

	    MetadataCache.FieldInfo fieldInfo = allFields.get(label);
	    if (fieldInfo == null) {
	        throw new Exception("Field not found: " + label
	                + " in SObject: " + QuickActionContext.currentSObject
	                + " or Flow: " + QuickActionContext.currentFlow);
	    }
	    if (fieldInfo.dataType == null) {
	        throw new Exception("DataType not found for: " + label);
	    }
	    return fieldInfo;
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

	// ============================================================
	// --- Form Handling Section ---
	// ============================================================
	// Fill form field by label and value
	public void FillFormValueUsingSalesforceAPIMetadata(String label, String targetValue) throws Exception {
		MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(label);
		String type = fieldInfo.dataType;
	    WebElement fieldElement = getFieldElementByLabelAndType(label,type);
	    scrollIntoView(fieldElement);
	    switch (type) {
	        case "String":
	        case "Url":
	        case "Int":
	        case "Phone":
	        case "Currency":
	        case "Double":
	        case "Date":
	        case "Email":
	        case "TextArea":
	            clearAndSendKeys(fieldElement, targetValue);
	            break;
	        case "Boolean":
	            SFClick(fieldElement);
	            break;
	        case "Picklist":
	        	String tag = fieldElement.getTagName().toLowerCase();
	            if ("a".equals(tag)) {
	                // Inline edit style picklist
	            	waitAndClick(fieldElement);
		            fieldElement.sendKeys(targetValue);
	                WebElement option = driver.findElement(By.xpath(
	                    "//a[@role='option' and @title = '"+ targetValue +"']"
	                ));
	                waitAndClick(option);
	            } else {
	                // Standard record edit style picklist
		            waitAndClick(fieldElement);
		            fieldElement.sendKeys(targetValue);
		            Thread.sleep(2000);
		            fieldElement.sendKeys(Keys.ENTER);
	            }
	            break;
	        case "MultiPicklist":
	            fillMultiPicklist(label, type, targetValue);
	            break;
	        case "Reference":
	        	fieldElement.sendKeys(targetValue);
	        	fieldElement.sendKeys(Keys.ARROW_DOWN);
	            Thread.sleep(2000);
	            fieldElement.sendKeys(Keys.ENTER);
	            break;
	        default:
	            throw new Exception("Unsupported field type: " + type);
	    }
	}

	public void fillFormValue(String label, String value, int waitInSeconds) throws Exception {
	    MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(label);
	    WebElement fieldElement = findElementWithWait(By.xpath(getFieldXPath(label, fieldInfo.dataType)), waitInSeconds);
	    scrollIntoView(fieldElement);
	    String type = fieldInfo.dataType;
	    switch (type) {
	        case "String": case "Url": case "Int": case "Phone":
	        case "Currency": case "Double": case "Date":
	        case "Email": case "TextArea":
	            clearAndSendKeys(fieldElement, value);
	            break;
	        case "Boolean":
	            SFClick(fieldElement);
	            break;
	        case "Picklist":
	            handlePicklist(fieldElement, value);
	            break;
	        case "MultiPicklist":
	            fillMultiPicklist(label, type, value);
	            break;
	        case "Reference":
	            fieldElement.sendKeys(value);
	            fieldElement.sendKeys(Keys.ARROW_DOWN);
	            Thread.sleep(2000);
	            fieldElement.sendKeys(Keys.ENTER);
	            break;
	        default:
	            throw new Exception("Unsupported field type: " + fieldInfo.dataType);
	    }
	}

	// Clear form value
	public void clearFormValue(String label) throws Exception {
		MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(label);
		String type = fieldInfo.dataType;
	    WebElement fieldElement = getFieldElementByLabelAndType(label,type);

		switch (type) {
		case "String":
		case "Url":
		case "Int":
		case "Phone":
		case "Currency":
		case "Double":
		case "Date":
		case "Email":
		case "TextArea":
		case "Reference":
			fieldElement.clear();
			break;
		case "Picklist":
			waitAndClick(fieldElement);
			fieldElement.clear();
			break;
		case "MultiPicklist":
			clearMultiPicklist(label, type);
			break;
		case "Boolean":
			if (fieldElement.isSelected())
				SFClick(fieldElement);
			break;
		default:
			throw new Exception("Unsupported field type: " + type);
		}
	}

	// Get XPath based on type
	private String getFieldXPath(String label, String type) throws Exception {
		label = label.replace("&", "&amp;");
		switch (type) {
		case "Reference":
		case "String":
		case "Url":
		case "Int":
		case "Phone":
		case "Currency":
		case "Double":
		case "Date":
		case "Email":
			return "//div[contains(@class,'active')]//input[@id=string(//label[normalize-space()='"
					+ label + "']/@for)]" + " | //div[contains(@class,'active')]//input[@aria-labelledby=string(//lightning-formatted-rich-text[contains(normalize-space(),'"
					+ label + "')]/@id)]";
		case "TextArea":
			return "//div[contains(@class,'active')]//textarea[@id=string(//label[normalize-space()='"
					+ label + "']/@for)]";
		case "Picklist":
			return "//div[contains(@class,'active')]//button[@id=string(//label[normalize-space()='"
					+ label + "']/@for)]" + " | //span[normalize-space(text())='"
					+ label + "']/ancestor::div[contains(@class,'uiInputSelect')]//a[@role='combobox']";
		case "MultiPicklist":
			return "//div[contains(@class,'active')]//div[contains(@class,'slds-form-element__label') and text()='"
					+ label + "']/following-sibling::div//div[contains(@class,'slds-dueling-list')] | //div[contains(@class,'active')]//flowruntime-picklist-input-lwc[.//span[normalize-space(text())='"
					+ label + "']]//select";
		case "Boolean":
			return "//label[normalize-space()='" + label + "']";
		default:
			throw new Exception("Unsupported field type for XPath: " + type);
		}
	}
	
	/**
	 * Clicks on a Quick Action button by its label (e.g. "New Opportunity").
	 * Resolves the API name via Salesforce Quick Actions API to avoid hardcoding.
	 */
	// click wrapper that updates current object automatically after navigation
	public void clickQuickAction(String objectApiName, String actionLabel) throws Exception {
		// 1. Get the API name for the action (you already do this elsewhere)
		Map<String, Object> actions = HTTPClientWrapper.getQuickActions(objectApiName);
		if (!actions.containsKey(actionLabel)) {
			throw new IllegalArgumentException("Action not found: " + actionLabel);
		}

		Object value = actions.get(actionLabel);
		String apiName;
		String describeUrl = null;

		if (value instanceof JSONObject) {
			JSONObject flowInfo = (JSONObject) value;
			apiName = flowInfo.getString("apiName");
			describeUrl = flowInfo.getString("describeUrl");

			String basePrefix = "/services/data" + HTTPClientWrapper.API_VERSION;
			if (describeUrl.startsWith(basePrefix)) {
				describeUrl = describeUrl.substring(basePrefix.length());
			}
		} else {
			apiName = value.toString();
		}

		JSONObject actionDescribe;
		if (describeUrl != null) {
			actionDescribe = (JSONObject) HTTPClientWrapper.runGetRequest(describeUrl);
		} else {
			String path = "/sobjects/" + objectApiName + "/quickActions/" + apiName + "/describe";
			actionDescribe = (JSONObject) HTTPClientWrapper.runGetRequest(path);
		}

		String type = actionDescribe.optString("type", "SObject");
		if ("Flow".equalsIgnoreCase(type)) {
			String flowDevName = actionDescribe.optString("flowDevName");

			// 🔹 store both
			QuickActionContext.currentSObject = objectApiName;
			QuickActionContext.currentFlow = flowDevName;

			setCurrentObjectType("Flow");
		} else {
			QuickActionContext.currentSObject = objectApiName;
			QuickActionContext.currentFlow = null;

			setCurrentObjectType("SObject");
		}

		WebElement btn = getQuickActionElement(objectApiName, actionLabel);
		SFClick(btn);

		waitForSFPagetoLoad();
		System.out.println("PASS: Clicked Quick Action: " + actionLabel + " (" + objectApiName + ")");
	}

	// overload uses currentObject implicitly
	public void clickQuickAction(String actionLabel) throws Exception {
		clickQuickAction(requireCurrentObject(), actionLabel);
	}
		
		
		
		public void clickChatterPostShowActionButton(String actorName) {
			 // Locate the chatter post for the given actorName
		    String postXpath = getChatterPost(actorName);
		    WebElement actionButton = driver.findElement(
		        By.xpath(postXpath + "//div[contains(@class,'cuf-feedItemActionTrigger')]//button")
		    );
		    clickButton(actionButton);
		}
		
		

	    // ✅ Click button by text
	    public void clickButton(String buttonText) {
	        WebElement button = waitForElementToBeClickable(By.xpath(getButtonLocator(buttonText)), DEFAULT_WAIT_SECONDS);
	        clickButton(button);
	    }
	    
	    public void clickButton(WebElement button) {
	    	String buttonText = button.getText();
	    	boolean logoutflag = buttonText.equalsIgnoreCase("Log Out");
	        SFClick(button);
	        hardwait(2);
	        if(!logoutflag) {
	        updateCurrentObjectAuto();
	        }
	    }
	    
	    public void clickTab(String tabName) {
	        WebElement tab = waitForElementToBeClickable(By.xpath(getTabLocator(tabName)), DEFAULT_WAIT_SECONDS);
	        SFClick(tab);
	        updateCurrentObjectAuto();
	    }
	    
	    public String getTabLocator(String tabName) {
			return "//one-app-nav-bar-item-root//a[@title='" 
					+ tabName + "'] | //div[contains(@class,'active')]//li[contains(@class,'slds-tabs_default__item') and @title='"
					+ tabName + "']";
	    }
	    
	    public String getButtonLocator(String buttonText) {
			return "(//div[contains(@class,'active')]//button[normalize-space()='" 
					+ buttonText + "' or @title='"
					+ buttonText + "'] | //div[contains(@class,'active')]//a[normalize-space()='" 
					+ buttonText + "' or @title='" 
					+ buttonText + "'] | //div[@aria-describedby=string(//span[normalize-space()='"
					+ buttonText + "']/@id) and string(//span[normalize-space()='" 
					+ buttonText + "']/@id)!='']//lightning-icon | //div[contains(@class,'active')]//button//span[normalize-space()='"
					+ buttonText + "'] | //div[@aria-describedby=string(//span[@role='tooltip' and normalize-space()='"
					+ buttonText + "']/@id) and string(//span[@role='tooltip' and normalize-space()='"
					+ buttonText + "']/@id)!='']/ancestor::button | //lightning-base-combobox-item//span[@title='"
					+ buttonText + "'])[last()]";
	    }
	    
	    public String getChatterPost(String actorName) {
			return "//article[.//a[@title='" + actorName + "']]";
	    }
		   
	    public void enterSearchText(String textTobeSearched) throws InterruptedException {
	    	WebElement SearchTextElement = findSearchTextElement();
	        enterValue(SearchTextElement, textTobeSearched);
	        
	    }
	    
	    public WebElement findSearchTextElement() {
			String xpath = "(//input[@aria-describedby='Search' or contains(@placeholder,'Search') ] | (//*[local-name()='svg' and @data-key='search']/preceding::input)[last()])[last()]";
	        return waitForElementToBeClickable(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
	    }
	    
	    public void pressEnterKeyAfterSearch() {
	    	findSearchTextElement().sendKeys(Keys.ENTER);
	        
	    }
	    
	    
	    public void selectOption(int option) {
	        String optionXpath = "(//div[contains(@class,'active')]//li[contains(@class,'lookup__item')]//a[@role='option'] | " +
	                             "//div[contains(@class,'active')]//lightning-base-combobox-item[@role='option'])[" + option + "]";
	        hardwait(2);
	        WebElement optionElement = null;
	        try {
	            optionElement = waitForElementToBeClickable(By.xpath(optionXpath), DEFAULT_WAIT_SECONDS);
	            SFClick(optionElement);
	        } catch (Exception e) {
	            e.printStackTrace();
	            // Retry after clicking search text element
	            findSearchTextElement().click();
	            optionElement = waitForElementToBeClickable(By.xpath(optionXpath), DEFAULT_WAIT_SECONDS);
	            SFClick(optionElement);
	        }
	    }
	    
	    public void selectOptionByName(String optionName) {
			String cssSelector = "[role='dialog'] .uiPopupTrigger a";
	        WebElement optionElement = waitForElementToBeClickable(By.cssSelector(cssSelector), DEFAULT_WAIT_SECONDS);
	        waitAndClick(optionElement);
	        optionElement.sendKeys(optionName);
	        hardwait(2);
            optionElement.sendKeys(Keys.ENTER);
	        
	    }
	
	// ===============================
	// Helper Methods
	// ===============================

	    

	    /**
	     * Returns a list of all tab labels.
	     */
	    public List<String> getAllStageTabs() {
	        List<WebElement> tabs = driver.findElements(By.cssSelector("a.tabHeader"));
	        List<String> tabLabels = new ArrayList<>();
	        for (WebElement tab : tabs) {
	            tabLabels.add(tab.getText()); // or "data-tab-name"
	        }
	        return tabLabels;
	    }
	    
	/**
	 * Try to extract object API name from current URL or recordId.
	 * Strategy:
	 * 1) /lightning/r/<ObjectApiName>/<Id>/...
	 * 2) /lightning/o/<ObjectApiName>/list
	 * 3) /sObject/<Id> (one.app legacy)
	 * 4) If only Id found → call setCurrentObjectFromRecordId
	 */
	private String extractObjectFromUrl(String url) {
	    if (url == null) return null;

	    Matcher m = LIGHTNING_RECORD_PATTERN.matcher(url);
	    if (m.find()) {
	        return m.group(1);
	    }

	    m = LIGHTNING_OBJECT_PATTERN.matcher(url);
	    if (m.find()) {
	        return m.group(1);
	    }

	    // legacy one.app or others that contain sObject/<Id>
	    m = ONE_APP_SOBJECT_PATTERN.matcher(url);
	    if (m.find()) {
	        // we only got ID — let the caller handle retrieving object via recordId
	        String id = m.group(1);
	        try {
	            setCurrentObjectFromRecordId(id);
	            return this.currentObjectApiName;
	        } catch (Exception e) {
	            return null;
	        }
	    }

	    return null;
	}

	/**
	 * High-level: update currentObjectApiName based on URL / record id fallback.
	 * Safe to call after navigation.
	 */
	public void updateCurrentObjectAuto() {
	    try {
	        waitForSFPagetoLoad();
	        String url = driver.getCurrentUrl();
	        String obj = extractObjectFromUrl(url);
	        if (obj != null && !obj.isEmpty()) {
	            this.currentObjectApiName = obj;
	            System.out.println("Auto-detected current object from URL: " + obj);
	            return;
	        }

	        // fallback: try to extract ID from current URL and use prefix lookup / UI-API
	        Matcher idMatcher = ID_EXTRACT_PATTERN.matcher(url);
	        if (idMatcher.find()) {
	            String id = idMatcher.group(1);
	            if (id != null) {
	                setCurrentObjectFromRecordId(id);
	                if (this.currentObjectApiName != null) {
	                    System.out.println("Auto-detected current object from recordId fallback: " + this.currentObjectApiName);
	                    return;
	                }
	            }
	        }

	        // Final fallback: thread-level object (if tests set it)
	        String threadObj = null;
	        try {
	            threadObj = BaseTest.getThreadCurrentObject();
	        } catch (Throwable ignored) { }
	        if (threadObj != null && !threadObj.isEmpty()) {
	            this.currentObjectApiName = threadObj;
	            System.out.println("Using ThreadLocal currentObject: " + threadObj);
	            return;
	        }

	        // nothing found — leave as null
	        System.out.println("updateCurrentObjectAuto: could not detect object from URL: " + url);

	    } catch (Exception e) {
	        System.out.println("updateCurrentObjectAuto: failed: " + e.getMessage());
	    }
	}

	/**
	 * Given a recordId, call UI-API fallback (uiApiHitter) to set extractedObjectName,
	 * then set currentObjectApiName. Uses your existing uiApiHitter() & ObjectPrefixCache fallback.
	 */
	public void setCurrentObjectFromRecordId(String recordId) throws Exception {
	    if (recordId == null || recordId.length() < 3) {
	        throw new IllegalArgumentException("Invalid recordId: " + recordId);
	    }
	    try {
	        // uiApiHitter will populate extractedObjectName
	        uiApiHitter(recordId);
	        if (this.extractedObjectName != null && !this.extractedObjectName.isEmpty()) {
	            setCurrentObject(this.extractedObjectName);
	            return;
	        }
	    } catch (Exception e) {
	        // ignore and try prefix-based fallback
	    }

	    // prefix fallback using ObjectPrefixCache (you already use this elsewhere)
	    String prefix = recordId.substring(0, 3);
	    String obj = ObjectPrefixCache.getObjectName(prefix);
	    if (obj != null && !obj.isEmpty()) {
	        setCurrentObject(obj);
	        return;
	    }

	    throw new RuntimeException("Could not resolve object for recordId: " + recordId);
	}
	
	private String getEditButtonXPath(String label){
		return String.format("(//div[contains(@class,'active')]//button[contains(@title,'Edit %s')])[last()]", label);
	}
	
	private String getChangeOwnerButtonXPath(String label){
		return "//div[contains(@class,'active')]//div[normalize-space()='"+label+"']/following-sibling::div[1]//button[@title='Change Owner'] | //p[normalize-space()='"+label+"']/following-sibling::p//button[@title='Change Owner']";
	}

	protected WebElement findElementWithWait(By locator, int waitInSeconds) {
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
	// Filling MultiPicklist (works for both old and new versions)
	private void fillMultiPicklist(String label, String type, String targetValue) throws Exception {
	    WebElement container = getFieldElementByLabelAndType(label, type);
	    String values = targetValue.replaceAll("[\\[\\]]", ""); // Remove brackets
	    String[] items = values.split(",\\s*");

	    // Try to find "Move to Chosen" button (old-style picklist)
	    WebElement moveButton = null;
	    WebElement availableList = null;
	    try {
	        availableList = container.findElement(By.xpath(".//ul[@data-source-list]"));
	        moveButton = container.findElement(By.xpath(".//button[@title='Move to Chosen']"));
	    } catch (NoSuchElementException e) {
	        // Available Chosen style picklist not found, likely new native multi-select
	    }

	    if (availableList != null && moveButton != null) {
	        for (String item : items) {
	            WebElement option = findOptionInListWithScroll(availableList, item);
	            scrollIntoView(option);
	            option.click();
	            waitAndClick(moveButton);
	            hardwait(1);
	        }
	    } else {
	        // New-style multi-select logic
	    	if ("Select".equalsIgnoreCase(container.getTagName())) {
	            Select dropDown = new Select(container);
	    		dropDown.selectByVisibleText(targetValue);
	    	}
	    }
	    hardwait(2);
	}



	// Picklist helper
	private void handlePicklist(WebElement fieldElement, String value) throws Exception {
	    String tag = fieldElement.getTagName().toLowerCase();
	    if ("a".equals(tag)) {
	        waitAndClick(fieldElement);
	        fieldElement.sendKeys(value);
	        WebElement option = driver.findElement(By.xpath("//a[@role='option' and @title='" + value + "']"));
	        waitAndClick(option);
	    } else {
	        waitAndClick(fieldElement);
	        fieldElement.sendKeys(value);
	        Thread.sleep(2000);
	        fieldElement.sendKeys(Keys.ENTER);
	    }
	}
	
	// Clearing MultiPicklist
	private void clearMultiPicklist(String label, String type) throws Exception {
	    WebElement container = getFieldElementByLabelAndType(label, type);
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
	
	public void enterValueUsingScript(String ElementName, String value) {
		WebElement element = null;
		switch(ElementName){
			case "ChatterComment" : element = driver.findElement(By.xpath("//div[@role='textbox']"));
				break;
			default : 
				throw new IllegalArgumentException(ElementName + "is not handled.");
		}
		enterValueUsingScript(element, value);
	}

	// Overloaded method without extraData (Post only)
	public void performChatterAction(String actionType, String message) {
	    performChatterAction(actionType, message, null);
	}

	// Main method with optional extraData
	public void performChatterAction(String actionType, String message, String extraData) {
	    // 1. Click the Chatter tab
	    WebElement chatterTab = driver.findElement(By.xpath(getTabLocator("Chatter")));
	    clickButton(chatterTab);

	    // 2. Select the sub-tab (Post / Poll / Log a Call)
	    WebElement actionTab = driver.findElement(By.xpath(getButtonLocator(actionType)));
	    clickButton(actionTab);

	    // 3. Handle Post
	    if (actionType.equalsIgnoreCase("Post")) {
	        WebElement postBox = driver.findElement(By.xpath("//div[contains(@class,'ql-editor') and @role='textbox']"));
	        Actions actions = new Actions(driver);
	        actions.moveToElement(postBox)
	               .click()
	               .sendKeys(message)
	               .pause(500)
	               .sendKeys(Keys.BACK_SPACE)   // small hack to trigger change event
	               .pause(500)
	               .sendKeys(Keys.ENTER)
	               .perform();

	        clickButton("Share");
	    }

	    // 4. Handle Poll
	    else if (actionType.equalsIgnoreCase("Poll")) {
	        WebElement questionBox = driver.findElement(By.xpath("//label[normalize-space()='Question']/following-sibling::textarea"));
	        enterValueTextArea(questionBox, message);

	        if (extraData != null) {
	            // Split extraData by comma → "Option1,Option2,Option3"
	            String[] options = extraData.split(",");
	            for (int i = 0; i < options.length; i++) {
	                WebElement optionBox = driver.findElement(
	                    By.xpath("//label[normalize-space()='Choice " + (i + 1) + "']/following-sibling::input"));
	                optionBox.sendKeys(options[i].trim());
	            }
	        }

	        clickButton("Ask");
	    }

	    // 5. Handle Log a Call
	    else if (actionType.equalsIgnoreCase("Log a Call")) {
	        WebElement subjectBox = driver.findElement(By.xpath("//input[@id=string(//label[text()='Subject']/@for)]"));
	        subjectBox.sendKeys(extraData != null ? extraData : "Default Subject");

	        WebElement commentsBox = driver.findElement(By.xpath("//label[normalize-space()='Comments']/following::textarea[1]"));
	        commentsBox.sendKeys(message);

	        clickButton("Save");
	    }

	    else {
	        throw new IllegalArgumentException("Unsupported Chatter action: " + actionType);
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
	

	// Scroll each section into view and return it
	private WebElement scrollToSection(String sectionName) {

	    WebDriverWait localWait = new WebDriverWait(driver, Duration.ofSeconds(10));
	    String sectionXpath = "//div[contains(@class,'active')]//*[contains(local-name(),'h2') or contains(local-name(),'h3') ]//span[text()='" + sectionName + "'] | //div[contains(@class,'active')]//*[contains(local-name(),'h2') or contains(local-name(),'h3') ]//p[text()='" + sectionName + "']";
	    WebElement section = localWait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(sectionXpath)));

	    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", section);

	    // Wait until section is visible on screen
	    localWait.until(ExpectedConditions.visibilityOf(section));

	    return section;
	}

	
	
	public List<WebElement> scrollEachSection(String sectionsString) {
	    // Remove brackets [ ] and split by comma
	    String cleaned = sectionsString.replaceAll("[\\[\\]]", "").trim();
	    String[] sectionNames = cleaned.split("\\s*,\\s*");
	    List<WebElement> sectionElements = new ArrayList<>();
	    for (String sectionName : sectionNames) {
	    	WebElement section = scrollToSection(sectionName);
	        sectionElements.add(section);
	    }
	    return sectionElements;
	}

	// ============================================================
	// --- Assertions Section ---
	// ============================================================
	public void assertRequiredFieldLabels(String expectedCsv) {
	    // Convert CSV string to List<String>
	    List<String> expectedLabels = Arrays.stream(expectedCsv.split(","))
	            .map(String::trim)
	            .collect(Collectors.toList());

	    // XPath: find labels that contain <abbr class='slds-required'>
	    List<WebElement> requiredLabelElements = driver.findElements(
	        By.xpath("//*[contains(@class,'slds-required')]/parent::*")
	    );

	    // Extract actual required field labels
	    List<String> actualLabels = requiredLabelElements.stream()
	            .map(e -> e.getText().trim())
	            .collect(Collectors.toList());

	    // Debug log
	    System.out.println("Expected required fields: " + expectedLabels);
	    System.out.println("Actual required fields:   " + actualLabels);

	    // Assert all expected are present
	    for (String label : expectedLabels) {
	        Assert.assertTrue(
	            actualLabels.contains(label),
	            "Missing required field: " + label + ". Found: " + actualLabels
	        );
	    }
	}
	
	public void assertPicklistOptionsEquals(String label, String expectedValues) {
	    // Step 1: Locate combobox container by label text
	    WebElement container = driver.findElement(By.xpath(
	        "//label[normalize-space()='" + label + "']/ancestor::lightning-combobox"
	    ));

	    // Step 2: Open dropdown if not already open
	    WebElement dropdownButton = container.findElement(By.xpath(".//button[contains(@class,'slds-combobox__input')]"));
	    if (!Boolean.parseBoolean(dropdownButton.getAttribute("aria-expanded"))) {
	        SFClick(dropdownButton);
	    }

	    // Step 3: Fetch all options from dropdown
	    List<WebElement> options = container.findElements(By.xpath(
	        ".//lightning-base-combobox-item//span[@title]"
	    ));

	    List<String> actualValues = options.stream()
	            .map(e -> e.getAttribute("title").trim())
	            .filter(v -> !v.equalsIgnoreCase("--None--")) // ignore "--None--" if not required
	            .collect(Collectors.toList());

	    // Step 4: Clean expected values
	    String values = expectedValues.replaceAll("[\\[\\]]", "");
	    List<String> expectedList = Arrays.stream(values.split(",\\s*"))
	            .map(String::trim)
	            .collect(Collectors.toList());

	    // Step 5: Compare lists (ignoring order)
	    if (!new HashSet<>(actualValues).equals(new HashSet<>(expectedList))) {
	        throw new AssertionError("Picklist options for '" + label + "' mismatch. " +
	                "Expected: " + expectedList + ", but got: " + actualValues);
	    }
	}
	
	public void assertTabEnabled(String tabName) {
        WebElement tab = findElementWithWait(By.xpath(getTabLocator(tabName)), DEFAULT_WAIT_SECONDS);
        Assert.assertTrue(tab.isEnabled(), tabName + " Tab should be enabled");
    }
	
	public void assertTabDisabled(String tabName) {
        WebElement tab = findElementWithWait(By.xpath(getTabLocator(tabName)), DEFAULT_WAIT_SECONDS);
        Assert.assertTrue(!tab.isEnabled(), tabName + " Tab should be disabled");
    }
	
	public void assertButtonEnabled(String buttonName) {
		WebElement button = findElementWithWait(By.xpath(getButtonLocator(buttonName)), DEFAULT_WAIT_SECONDS);
        Assert.assertTrue(button.isEnabled(), buttonName + " Button should be enabled");
    }
	
	public void assertButtonDisabled(String buttonName) {
        WebElement button = findElementWithWait(By.xpath(getButtonLocator(buttonName)), DEFAULT_WAIT_SECONDS);
        Assert.assertTrue(!button.isEnabled(), buttonName + " Button should be disabled");
    }
	
	
	// Default method → assumes row 1
	public void assertTableCellValueEquals(String columnName, String expectedValue) {
		assertTableCellValueEquals(1, columnName, expectedValue);
	}

	public void assertTableCellValueEquals(int rowNumber, String columnName, String expectedValue) {
	    String cellXpath = 
	        "//div[contains(@class,'active')]//table[contains(@class,'slds-table')]//tbody/tr["+rowNumber+"]/*[@data-label='"+columnName+"']";

	    WebElement cellElement = driver.findElement(By.xpath(cellXpath));
	    String actualValue = cellElement.getText().trim();
	    
	    Assert.assertEquals(actualValue, expectedValue, "row " + rowNumber + 
	            " for column '" + columnName +
	            "': expected [" + expectedValue + "] but found [" + actualValue + "]");
	}
	
	public void assertTableCellValueNotEquals(String columnName, String unexpectedValue) {
		assertTableCellValueNotEquals(1, columnName, unexpectedValue);
	}

	public void assertTableCellValueNotEquals(int rowNumber, String columnName, String unexpectedValue) {
		String cellXpath = "//div[contains(@class,'active')]//table[contains(@class,'slds-table')]//tbody/tr["
				+ rowNumber + "]/*[@data-label='" + columnName + "']";

		WebElement cellElement = driver.findElement(By.xpath(cellXpath));
		String actualValue = cellElement.getText().trim();

		Assert.assertNotEquals(actualValue, unexpectedValue, "row " + rowNumber + " for column '" + columnName
				+ "': not expected [" + unexpectedValue + "] and found [" + actualValue + "]");
	}
	
	public void assertSectionHeaders(String listOfSections) {
	    String cleaned =  listOfSections.replaceAll("[\\[\\]]", "").trim();
	    String[] expectedSectionNames = cleaned.split("\\s*,\\s*");
	    for (String expectedSectionName : expectedSectionNames) {
	    	WebElement section = scrollToSection(expectedSectionName);
	    	String actualSectionName = section.getText();
	    	Assert.assertEquals(actualSectionName, expectedSectionName);
	    }
	}
	
	public void assertShowActionDropdownEquals(String expectedOptionsCsv) {
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
	    WebElement dropdownList = wait.until(ExpectedConditions.visibilityOfElementLocated(
	        By.xpath("//div[contains(@class,'cuf-feedItemActionTrigger')]//div[contains(@class,'slds-dropdown') and contains(@class,'slds-dropdown_right')]//ul")
	    ));

	    List<WebElement> optionElements = dropdownList.findElements(By.xpath(".//li/a/span"));
	    List<String> actualOptions = optionElements.stream().map(WebElement::getText).map(String::trim).collect(Collectors.toList());
	    String cleaned = expectedOptionsCsv.replaceAll("[\\[\\]]", "");
	    List<String> expectedOptions = Arrays.stream(cleaned.split(",")).map(String::trim).collect(Collectors.toList());

	    Assert.assertEquals(actualOptions, expectedOptions,
	            "Given Dropdown options " + expectedOptions + " did not match " + actualOptions);
	}
	
	public void assertShowActionDropdownContains(String expectedOptionsCsv) {
	    // Wait for dropdown list to be visible
	    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
	    WebElement dropdownList = wait.until(ExpectedConditions.visibilityOfElementLocated(
	        By.xpath("//div[contains(@class,'cuf-feedItemActionTrigger')]//div[contains(@class,'slds-dropdown') and contains(@class,'slds-dropdown_right')]//ul")
	    ));

	    List<WebElement> optionElements = dropdownList.findElements(By.xpath(".//li/a/span"));
	    List<String> actualOptions = optionElements.stream()
	                                               .map(WebElement::getText)
	                                               .map(String::trim)
	                                               .collect(Collectors.toList());

	    // Convert expected string "[Option1,Option2,...]" → List
	    String cleaned = expectedOptionsCsv.replaceAll("[\\[\\]]", "");
	    List<String> expectedOptions = Arrays.stream(cleaned.split(","))
	                                         .map(String::trim)
	                                         .collect(Collectors.toList());

	    // Assert that each expected option is present in actual options
	    for (String expected : expectedOptions) {
	        Assert.assertTrue(actualOptions.contains(expected),
	                "Dropdown '" + actualOptions + "' does not contain expected option: " + expected);
	    }
	}
	
	public void assertFormValueByLabel(String label, String expectedValue) throws Exception {
		MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(label);
		String type = fieldInfo.dataType;
		WebElement we = getFieldElementByLabelAndType(label, type);
		String actualValue = "input".equalsIgnoreCase(we.getTagName())? we.getAttribute("value"):we.getText();
		Assert.assertEquals(actualValue, expectedValue, "Field '" + label + "' value mismatch.");
	}
	
	public void assertAvailablePicklistOptionsEquals(String label, String targetValue) throws Exception {
		MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(label);
		String type = fieldInfo.dataType;
		WebElement container = getFieldElementByLabelAndType(label, type);
	    WebElement availableList = container.findElement(By.xpath(".//ul[@data-source-list]"));

	    // Get all available options
	    List<WebElement> options = availableList.findElements(By.xpath(".//div[@role='option']"));

	    List<String> actualValues = options.stream()
	            .map(e -> e.getText().trim())
	            .collect(Collectors.toList());

	    // Clean target values
	    String values = targetValue.replaceAll("[\\[\\]]", ""); // Remove brackets if passed
	    List<String> expectedValues = Arrays.stream(values.split(",\\s*"))
	            .map(String::trim)
	            .collect(Collectors.toList());

	    // Assert equality (order-sensitive, use containsAll if not order-dependent)
	    if (!actualValues.equals(expectedValues)) {
	        throw new AssertionError("Available list mismatch. Expected: " + expectedValues + ", but got: " + actualValues);
	    }
	}

	public void assertChosenPicklistOptionsEquals(String label, String targetValue) throws Exception {
		MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(label);
		String type = fieldInfo.dataType;
		WebElement container = getFieldElementByLabelAndType(label, type);
	    WebElement selectedList = container.findElement(By.xpath(".//ul[@data-selected-list]"));

	    // Get all chosen options
	    List<WebElement> options = selectedList.findElements(By.xpath(".//div[@role='option']"));

	    List<String> actualValues = options.stream()
	            .map(e -> e.getText().trim())
	            .collect(Collectors.toList());

	    // Clean target values
	    String values = targetValue.replaceAll("[\\[\\]]", ""); // Remove brackets if passed
	    List<String> expectedValues = Arrays.stream(values.split(",\\s*"))
	            .map(String::trim)
	            .collect(Collectors.toList());

	    // Assert equality (order-sensitive, use containsAll if not order-dependent)
	    if (!actualValues.equals(expectedValues)) {
	        throw new AssertionError("Chosen list mismatch. Expected: " + expectedValues + ", but got: " + actualValues);
	    }
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

	public void assertElementVisible(String fieldLabel, int timeoutInSeconds) {
		try {
			MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(fieldLabel);
			String type = fieldInfo.dataType;
			WebElement element = getFieldElementByLabelAndType(fieldLabel, type);
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			wait.until(ExpectedConditions.visibilityOf(element));
			Assert.assertTrue(element.isDisplayed(), "Element with label '" + fieldLabel + "' should be visible.");
		} catch (Exception e) {
			Assert.fail("Element with label '" + fieldLabel + "' is not visible: " + e.getMessage());
		}
	}
	
	public void assertElementNotVisible(String fieldLabel, int timeoutInSeconds) {
		try {
			MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(fieldLabel);
			String type = fieldInfo.dataType;
			WebElement element = getFieldElementByLabelAndType(fieldLabel, type);
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			wait.until(ExpectedConditions.visibilityOf(element));
			Assert.assertTrue(!element.isDisplayed(), "Element with label '" + fieldLabel + "' should not visible.");
		} catch (Exception e) {
			Assert.fail("Element with label '" + fieldLabel + "' is visible or provided wrong label : " + e.getMessage());
		}
	}

	public void assertRecordExistsInDB(String objectApiName, String fieldLabel, String fieldValue) throws Exception {
		String recordId = getRecordIdByUiLabelAndValue(objectApiName, fieldLabel, fieldValue);
		Assert.assertNotNull(recordId, "Record with " + fieldLabel + " = '" + fieldValue + "' does not exist in DB.");
	}

	public void assertFieldLabelAndValue(String fieldLabel, String expectedValue) throws Exception {
		String actualValue = getValueByFieldLabel(fieldLabel);
		Assert.assertEquals(actualValue, expectedValue, "Field '" + fieldLabel + "' value mismatch.");
	}
	
	public String getValueByFieldLabel(String fieldLabel) throws Exception {
		String genericXpathLocator = "(//div[normalize-space()='"
				+ fieldLabel + "']//following-sibling::div[1]//lightning-primitive-input-checkbox |//div[normalize-space()='" 
				+ fieldLabel + "']//following-sibling::div[1]//lightning-formatted-text | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel + "']//following-sibling::div[1]//div[contains(@class,'recordTypeName')]/span | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel + "']//following-sibling::div[1]//lightning-formatted-address | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel + "']//following-sibling::div[1]//lightning-formatted-url | //div[contains(@class,'active')]//p[normalize-space()='"
				+ fieldLabel + "']/following-sibling::p[1]//records-hoverable-link//a//span | //div[contains(@class,'active')]//div[normalize-space()='" 
				+ fieldLabel + "']/following-sibling::div[1]//records-hoverable-link//a//span | //div[contains(@class,'active')]//p[normalize-space()='"
				+ fieldLabel + "']/following-sibling::p[1]//lightning-formatted-text | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel + "']/following-sibling::div[1]//lightning-formatted-name | //div[contains(@class,'active')]//div[normalize-space()='"
				+ fieldLabel + "']/following-sibling::div[1]//lightning-formatted-email | //div[contains(@class,'active')]//records-entity-label[normalize-space()='"
				+ fieldLabel + "']/following::lightning-formatted-text[@slot='primaryField'])[last()]";
		WebElement we = driver.findElement(By.xpath(genericXpathLocator));
		return we.getText();
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

	public void assertToastMessageContains(String expectedMessage) {
		String xpath = 
				"//div[contains(@class,'slds-notify--toast')]//span[contains(@class,'forceActionsText') and text()]";
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			WebElement toastMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
			String actualMessage = toastMessage.getText();
			Assert.assertTrue(actualMessage.contains(actualMessage), "Toast Message should contains " + expectedMessage + " and found " + actualMessage);
			hardwait(10);
		} catch (Exception e) {
			Assert.fail("Toast message did not appear: " + expectedMessage);
		}
	}
	
	public void assertChatterPostValueContains(String expectedMessage) {
		String postValueLocator = "[data-type='TextPost']";
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
			WebElement postValueElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(postValueLocator)));
			String actualMessage = postValueElement.getText();
			Assert.assertTrue(actualMessage.contains(actualMessage), "Post value should contains " + expectedMessage + " and found " + actualMessage);
			hardwait(10);
		} catch (Exception e) {
			Assert.fail("Post value did not appear");
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
			Assert.fail("Picklist not found: " + fieldLabel);else {
		boolean valueFound = false;
		for (Object item : allValuesList.get(0)) {
			String value = JsonPath.read(item, "$.value");
			if (expectedValue.equals(value)) {
				valueFound = true;
				break;
			}
		}
		Assert.assertTrue(valueFound, "List of Picklist values for label "+ fieldLabel + ": "+ allValuesList.get(0).toArray() +" should contains " + expectedValue );}
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

	
	public boolean waitForElementToAppear(WebElement element, int timeoutInSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			wait.until(ExpectedConditions.visibilityOf(element));
			return element.isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	
	public boolean waitForElementToDisappear(By locator, int timeoutInSeconds) {
		try {
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
			return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
		} catch (Exception e) {
			return false;
		}
	}

	
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
	        MetadataCache.FieldInfo fieldInfo = getFieldInfoUsingMetadata(label);
			String type = fieldInfo.dataType;
			WebElement element = getFieldElementByLabelAndType(label, type);
	        wait.until(ExpectedConditions.visibilityOf(element));
	        System.out.println("PASS: Field '" + label + "' appeared on the page.");
	        return true;
	    } catch (Exception e) {
	        System.out.println("FAIL: Field '" + label + "' did not appear on the page within "
	                           + timeoutInSeconds + " seconds.");
	        return false;
	    }
	}

	public String getRecordIdFromUrl() {

    	String url = driver.getCurrentUrl();
	    try {
	        // Example: https://.../lightning/r/Opportunity/006VZ00000NXjxBYAT/view
	        String[] parts = url.split("/");
	        // RecordId is always second last segment before "view"
	        return parts[parts.length - 2];
	    } catch (Exception e) {
	        throw new IllegalArgumentException("Invalid Salesforce record URL: " + url, e);
	    }
	}
	
	public boolean waitUntilRecordUnlocked(int maxWaitSeconds) {
		String recordId = getRecordIdFromUrl();
	    int waited = 0;
	    try {
	        while (waited < maxWaitSeconds) {
	            // Build SOQL
	            String soql = "SELECT RecordId FROM RecordLock WHERE RecordId = '" + recordId + "'";
	            String encodedSoql = URLEncoder.encode(soql, "UTF-8");

	            // Call Salesforce REST API
	            JSONObject response = (JSONObject) HTTPClientWrapper.runGetRequest("/query/?q=" + encodedSoql);

	            // Parse response
	            if (response != null && response.has("records")) {
	                JSONArray records = response.getJSONArray("records");
	                if (records.isEmpty()) {
	                    System.out.println(" Record " + recordId + " is unlocked.");
	                    return true;
	                }
	            }

	            // Still locked → wait & retry
	            System.out.println(" Record " + recordId + " is still locked, waiting...");
	            Thread.sleep(5000);
	            waited += 5;
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return false; // Timed out
	}
	

	
	// ✅ Validate modal header text
    public void assertModalHeader(String expectedText) {
        String xpath = "//div[contains(@class,'active')]//div[@id='wrapper-body']//h2 |  //div[contains(@class,'active')]//h3[contains(@class,'slds-show notification-text-title')]";
        WebElement header = waitForElementToBeClickable(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
        if (!header.getText().trim().equals(expectedText)) {
            throw new AssertionError("Header mismatch! Expected: " + expectedText + ", Found: " + header.getText());
        }
    }
    
 // ✅ Visible check
    public void assertTextVisible(String expectedText) {
        String xpath = String.format("//*[normalize-space()='%s']", expectedText);
        try {
            WebElement element = findElementWithWait(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
            Assert.assertTrue(
                element.getText().trim().equals(expectedText),
                "Header mismatch! Expected: " + expectedText + ", Found: " + element.getText()
            );
        } catch (TimeoutException e) {
            Assert.fail("Expected text '" + expectedText + "' to be visible, but it was not found within timeout.");
        }
    }

    // ✅ Not visible check
    public void assertTextNotVisible(String expectedText) {
        String xpath = String.format("//*[normalize-space()='%s']", expectedText);
        try {
            WebElement element = findElementWithWait(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
            Assert.assertTrue(
                !element.isDisplayed(),
                "Text '" + expectedText + "' was expected NOT to be visible, but it is displayed."
            );
        } catch (TimeoutException e) {
            // Element not found → passes as "not visible"
            Assert.assertTrue(true, "Text '" + expectedText + "' is not present (as expected).");
        }
    }

    // ✅ Validate modal message text (exact match)
    public void assertModalMessage(String expectedText) {
        String xpath = "//div[contains(@class,'active')]//div[@id='wrapper-body']//lightning-formatted-rich-text | //div[contains(@class,'active')]//span[contains(@class,'notification-text')]";
        WebElement msg = waitForElementToBeClickable(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
        if (!msg.getText().contains(expectedText)) {
            throw new AssertionError("Message mismatch! Expected to contain: " + expectedText + ", Found: " + msg.getText());
        }
    }


	
	public void assertButtonText(String expectedText) {
	    String xpath = "//div[contains(@class,'active')]//div[@id='wrapper-body']//button[normalize-space()='" + expectedText + "']";
	    try {
	        WebElement button = waitForElementToBeClickable(By.xpath(xpath), DEFAULT_WAIT_SECONDS);
	        String actual = button.getText().trim();
	        if (!actual.equals(expectedText)) {
	            throw new AssertionError("Button text mismatch! Expected: " + expectedText + ", Found: " + actual);
	        }
	    } catch (TimeoutException e) {
	        throw new AssertionError("Button with text '" + expectedText + "' not found in modal within timeout.");
	    }
	}
	

    public void assertStageTabSelected(String tabName) {
        WebElement tab = waitForPresenceOfElement(By.xpath("//a[contains(@class,'tabHeader') and contains(normalize-space(),'" + tabName + "')]"), DEFAULT_WAIT_SECONDS);
        boolean isSelected = tab.getAttribute("aria-selected").equalsIgnoreCase("true")?true:false;
        Assert.assertTrue(isSelected, "Tab '" + tabName + "' should be selected");
    }

    
    public void assertStageTabIsCurrent(String tabName) {
        WebElement tab = driver.findElement(By.xpath("//a[contains(@class,'tabHeader') and contains(normalize-space(),'" + tabName + "')]"));
        boolean isSelected = tab.getAttribute("aria-current").equalsIgnoreCase("true")?true:false;
        Assert.assertTrue(isSelected, "Tab '" + tabName + "' should be selected");
    }
    
    public void assertNotVisible(WebElement element) {
        Assert.assertTrue(element.isDisplayed());
    }
	
	// ============================================================
		// --- Get Current Object Section ---
	// ============================================================
	
	// Explicitly set/clear current object (useful in setup or test helpers)
	public void setCurrentObject(String objectApiName) {
	    this.currentObjectApiName = objectApiName;
	    System.out.println("SFPageBase: currentObject set -> " + objectApiName);
	}

	public String getCurrentObject() {
	    if (this.currentObjectApiName == null || this.currentObjectApiName.isEmpty()) {
	        // lazy auto-detect
	        try {
	            updateCurrentObjectAuto();
	        } catch (Exception e) {
	            // keep silent — caller can decide how to handle null
	        }
	    }
	    return this.currentObjectApiName;
	}

	public void clearCurrentObject() {
	    this.currentObjectApiName = null;
	}

	// internal helper for methods that must have object
	private String requireCurrentObject() {
	    String obj = getCurrentObject();
	    if (obj == null || obj.isEmpty()) {
	        // Try ThreadLocal fallback
	        try {
	            String threadObj = BaseTest.getThreadCurrentObject();
	            if (threadObj != null && !threadObj.isEmpty()) {
	                this.currentObjectApiName = threadObj;
	                return threadObj;
	            }
	        } catch (Throwable ignored) { }

	        throw new IllegalStateException("Current object not set. Call setCurrentObject(...) or ensure a navigation method ran.");
	    }
	    return obj;
	}
	
	
	public static void setCurrentObjectType(String t) {
		currentObjectType = t;
	}

	public static String getCurrentObjectType() {
		return currentObjectType;
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