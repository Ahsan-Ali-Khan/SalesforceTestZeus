package testscripts;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import utils.HTTPClientWrapper;


public class SalesManager_IndividualManagement_Leads_CreateLead_MarketingLead_CreateIndividual extends BaseTest {
	
	public String randomLetters(int length) {
        StringBuilder result = new StringBuilder(length);
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            char randomChar = (char) ('A' + random.nextInt(26));
            result.append(randomChar);
        }
        return result.toString();
    }
	
	 public String randomWithRange( String prefix, long min, long max) {
	        String randomString = prefix + (long) (new Random().nextDouble()*(max - min) + min);
	        return randomString;
	    }
	
	@Test
	public void editAccount() throws Exception {
		String randomLetters = randomLetters(5);
	    String FirstName = "WebomatesFirst" + new Random().nextInt(10000);
	    String LastName = "WebomatesFirst" + new Random().nextInt(10000);
		String accountName = "Webomates " + randomLetters +" "+ new Random().nextInt(1000000);
		String leadName = "Webomates " + randomLetters +" "+ new Random().nextInt(1000000);
	    String street = new Random().nextInt(10000) + " High Ridge Rd";
	    String email = FirstName + "@"+ randomLetters + new Random().nextInt(10000) +".com";
	    String website = "www." + randomLetters + new Random().nextInt(10000) + ".com";
	    int firstDigit = new Random().nextInt(9) + 1;
	    StringBuilder phoneNumberBuilder = new StringBuilder(String.valueOf(firstDigit));
        for (int i = 1; i < 10; i++) {
            phoneNumberBuilder.append(new Random().nextInt(10));
        }

        String phone = phoneNumberBuilder.toString();
        String phone2 = phoneNumberBuilder.toString();
        String camId = randomWithRange( "32", 1000, 9999);

	    HTTPClientWrapper client = new HTTPClientWrapper();
	    Map<String, Object> leadData = new HashMap<>();

	 // Additional fields from the provided steps

	    // Fields under CreateLeadRequestSchema.fields
//	    leadData.put("Salutation", "Mr.");                 // CONSTANT_BLANK
//	    leadData.put("MiddleName", "");                // CONSTANT_BLANK
//	    leadData.put("Title", "POC");
//	    leadData.put("MobilePhone", phone);            // ${phone}
//	    leadData.put("Preferred_Contact_Method__c", "All");
//	    leadData.put("Industry", "ALCO29828");
//	    leadData.put("Website", website);              // ${website}
//	    leadData.put("Description", "Webomates Lead");
//	    leadData.put("Country", "United States");
//	    leadData.put("GeocodeAccuracy", "");          // CONSTANT_BLANK
//	    leadData.put("Latitude", "");                 // CONSTANT_BLANK
//	    leadData.put("Longitude", "");                // CONSTANT_BLANK
//	    leadData.put("StateCode", "CT");
//	    leadData.put("Street", street);                // ${street}
//	    leadData.put("NYI__c", false);                // FALSE
//	    leadData.put("Budget__c", 10000);
//	    leadData.put("Flight_Start_Date__c", "2025-12-31");
//	    leadData.put("Flight_End_Date__c", "2026-12-31");
//	    leadData.put("Contact_Record_Type__c", "Advertiser");
//	    leadData.put("LeadSource", "General Sales Source");
//	    leadData.put("Status", "New");
//	    leadData.put("Lead_Status__c", "New Lead");
//	    leadData.put("Date_of_Subscribe_Request__c", "2025-03-28");

	    
	    leadData.put("FirstName", FirstName); 
	    leadData.put("LastName", LastName); 
	    leadData.put("Email", email);                  // ${email}
	    leadData.put("Phone", phone);                  // ${phone}
	    leadData.put("Company", accountName);         // ${accountName}
	    leadData.put("Website", website);              // ${website}
	    leadData.put("City", "Stamford");
	    leadData.put("PostalCode", "06905");
	    leadData.put("State", "Connecticut");
	    leadData.put("Status", "New");
	    leadData.put("OwnerId", HTTPClientWrapper.getUserIdByRole("MarketSeller"));  // ${marketSellerUserId}
	    leadData.put("RecordTypeId", HTTPClientWrapper.getRecordTypeId("Lead", "Marketing_Lead")); // ${marketingLeadRecordType}
	 JSONObject createdLead = client.createByLabels("Lead", leadData);

	 String leadId = createdLead.getString("id");
	 System.out.println("Created Lead Id: " + leadId);
		try {
		// Navigation to login page
		lightningloginpage.openHomepage(appUrl);
		lightningloginpage.loginWithRole("SalesManager");
		lightningloginpage.applauncher("Lead");
		
		String recordID = objectlistpage.getRecordIdByUiLabelAndValue("Lead", "Lead Name", "WebomatesFirst7055");
		objectlistpage.NavigateToRecord("Lead", recordID);
		objectlistpage.uiApiParser(recordID);
		
			
			objectlistpage.clickNew();
		
			//	objectlistpage.formValueFiller("First Name", "webo0409");
			//	objectlistpage.formValueFiller("Last Name", "webo0409");
			//	objectlistpage.formValueFiller("Email", "webo@gmail.com");
			//	objectlistpage.formValueFiller("Business Name", "webomates1241");
			
//			driver.findElement(By.xpath("//div[contains(@class, 'active')]//input[@name='firstName']")).sendKeys(firstName);
//			driver.findElement(By.xpath("//div[contains(@class, 'active')]//input[@name='lastName']")).sendKeys(lastName);
//			driver.findElement(By.xpath("//div[contains(@class, 'active')]//input[@name='Email']")).sendKeys(email);
//			driver.findElement(By.xpath("//input[@name='Company']")).sendKeys(business);
			
			objectlistpage.clickSave();

			//objectlistpage.assertFormValue("Lead Name", "webo0409 webo0409");
			//objectlistpage.assertFormValue("Email", "webo@gmail.com");
			//objectlistpage.marketingTab("Indivisual", "Individual");
			//objectlistpage.assertEquals("Lead Name", "webo0409 webo0409");
			
			Thread.sleep(10000);
			
//			Assert.assertEquals(driver.findElement(By.xpath("(//div[@data-target-selection-name='sfdc:RecordField.Lead.Name']//lightning-formatted-name)[last()]")).getText(), fullName);
			Assert.assertEquals(driver.findElement(By.xpath("//div[@data-target-selection-name='sfdc:RecordField.Lead.Email']//a")).getText(), email);
			
			objectlistpage.clickMarketingTab();
			
			Thread.sleep(10000);
				//not working 
			//Assert.assertEquals(driver.findElement(By.xpath("//span[text()='Individual']/ancestor::dt/following-sibling::dd/div/span//a |//span[text()='Individual']/parent::div/following-sibling::div/span//a")).getText(), fullName);
			
			objectlistpage.clickDetailsTab();
			objectlistpage.clickEditByFieldLabel("Phone");

			//objectlistpage.formValueFiller("Phone", "ABC");
			
			driver.findElement(By.xpath("//input[@name='Phone']")).sendKeys("ABC");
			
			//objectlistpage.clickSalutationDropdown();
			objectlistpage.clickSave();
			
			objectlistpage.assertFormValueSnags("Phone", "Phone");
			
			driver.findElement(By.xpath("//input[@name='Phone']")).clear();
			
			driver.findElement(By.xpath("//input[@name='Phone']")).sendKeys("%%");
			
			//objectlistpage.clickSalutationDropdown();
			objectlistpage.clickSave();
			
			objectlistpage.assertFormValueSnags("Phone", "Phone");
			
			driver.findElement(By.xpath("//input[@name='Phone']")).sendKeys("9424312");
			
			objectlistpage.clickSave();
			
			driver.findElement(By.xpath("//input[@name='Phone']")).clear();
			
			driver.findElement(By.xpath("//input[@name='Phone']")).sendKeys("9424312579");
			
			driver.findElement(By.xpath("//input[@name='Phone']")).clear();
			
			objectlistpage.clickSave();
			//objectlistpage.assertError("Phone", "Enter 10 digit Phone number without any special characters. The field will be auto-formatted to US format (xxx)-xxx-xxx once saved");
			//objectlistpage.assertSnag("Phone", "Phone");
			
			//objectlistpage.();
			//objectlistpage.clickSave();
			
//--------------------------------------------------------
		} catch (Exception e) {

			client.deleteRecord("Lead", leadId);
			System.out.println("\nDeleted Lead: " + leadId);

			JSONObject deletedCheck = (JSONObject) HTTPClientWrapper.runGetRequest("/sobjects/Lead/" + leadId);
			if (deletedCheck == null) {
				System.out.println("Verified: Lead successfully deleted.");
			} else {
				System.out.println("Account still exists!");
			}

			throw e;
		}
		
		
				
	} 

	
}
