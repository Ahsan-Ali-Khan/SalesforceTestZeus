package testscripts;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
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
	    String MarketingLeadFirstName = "Webomates" + randomLetters + new Random().nextInt(1000000);
	    String MarketingLeadLastName = "Webomates" + randomLetters + new Random().nextInt(1000000);
	    String MarketingLeadEmail = MarketingLeadFirstName + "@"+ randomLetters + new Random().nextInt(10000) +".com";
		String accountName = "Webomates" + randomLetters +" "+ new Random().nextInt(1000000);
		String SalesLeadFirstName = "Webomates" + randomLetters + new Random().nextInt(1000000);
	    String SalesLeadLastName = "Webomates" + randomLetters + new Random().nextInt(1000000);
	    String SalesLeadEmail = SalesLeadFirstName + "@"+ randomLetters + new Random().nextInt(10000) +".com";
	    String website = "www." + randomLetters + new Random().nextInt(10000) + ".com";
	    int firstDigit = new Random().nextInt(9) + 1;
	    StringBuilder phoneNumberBuilder = new StringBuilder(String.valueOf(firstDigit));
        for (int i = 1; i < 10; i++) {
            phoneNumberBuilder.append(new Random().nextInt(10));
        }

        String phone = phoneNumberBuilder.toString();

	    HTTPClientWrapper client = new HTTPClientWrapper();
	    Map<String, Object> leadData = new HashMap<>();

	    
	    leadData.put("Salutation", "Mr."); 
	    leadData.put("FirstName", MarketingLeadFirstName); 
	    leadData.put("LastName", MarketingLeadLastName); 
	    leadData.put("Email", MarketingLeadEmail);                  // ${email}
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
		
		objectlistpage.clickButton("New");
		
		objectlistpage.FillFormValueUsingSalesforceAPIMetadata("First Name", SalesLeadFirstName);
		objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Last Name", SalesLeadLastName);
		objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Email", SalesLeadEmail);
		objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Business Legal Name", accountName);
		
		objectlistpage.clickButton("Save");

		String LeadName = SalesLeadFirstName+ " " + SalesLeadLastName;
		objectlistpage.assertFieldLabelAndValue("Name", LeadName);
		objectlistpage.assertFieldLabelAndValue("Email", SalesLeadEmail.toLowerCase());
		
		objectlistpage.clickTab("Marketing");
		objectlistpage.assertFieldLabelAndValue("Individual", LeadName);
		
		objectlistpage.clickTab("Details");
		
		objectlistpage.clickEditByFieldLabel("Phone");
		
		objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Phone", "abc");

		
		objectlistpage.clickButton("Save");
		
		objectlistpage.assertFormErrorValueByLabel("Phone", "Enter 10 digit Phone number without any special characters. The field will be auto-formatted to US format (xxx)-xxx-xxx once saved");
		objectlistpage.assertFormValueSnags("Phone", "Phone");

		objectlistpage.clickEditByFieldLabel("Phone");
		
		objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Phone", "$%&/");
		objectlistpage.clickButton("Save");
		
		objectlistpage.assertFormErrorValueByLabel("Phone", "Enter 10 digit Phone number without any special characters. The field will be auto-formatted to US format (xxx)-xxx-xxx once saved");
		objectlistpage.assertFormValueSnags("Phone", "Phone");
		
		objectlistpage.clickEditByFieldLabel("Phone");
		
		objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Phone", "708451215");
		objectlistpage.clickButton("Save");
		
		objectlistpage.assertFormErrorValueByLabel("Phone", "Enter 10 digit Phone number without any special characters. The field will be auto-formatted to US format (xxx)-xxx-xxx once saved");
		objectlistpage.assertFormValueSnags("Phone", "Phone");
		
		objectlistpage.assertFieldLabelAndValue("Phone", "(708) 451-2150");
		
		objectlistpage.clickTab("Marketing");
			
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
