package testscripts;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import utils.HTTPClientWrapper;

public class SystemAdmin_AccountsManagement_Accounts_AddNew_ProspectType_Advertiser extends BaseTest {

	public String randomLetters(int length) {
        // Initialize a StringBuilder to construct the result
        StringBuilder result = new StringBuilder(length);

        // Create a Random instance for random number generation
        Random random = new Random();

        // Generate random uppercase letters
        for (int i = 0; i < length; i++) {
            // Randomly select a letter between 'A' and 'Z'
            char randomChar = (char) ('A' + random.nextInt(26));
            result.append(randomChar);
        }

        // Return the generated string
        return result.toString();
    }
	
	 public String randomWithRange( String prefix, long min, long max) {
	        String randomString = prefix + (long) (new Random().nextDouble()*(max - min) + min);
	        return randomString;
	    }
	
	@Test
	public void editAccount() throws Exception {
	    // Create account via API

		String randomLetters = randomLetters(5);
	    String FirstName = "WebomatesFirst" + new Random().nextInt(10000);
		String accountName = "Webomates " + randomLetters +" "+ new Random().nextInt(1000000);
	    String street = new Random().nextInt(10000) + " High Ridge Rd";
	    String email = FirstName + "@"+ randomLetters + new Random().nextInt(10000) +".com";
	    String website = "www." + randomLetters + new Random().nextInt(10000) + ".com";
	    int firstDigit = new Random().nextInt(9) + 1;
	    StringBuilder phoneNumberBuilder = new StringBuilder(String.valueOf(firstDigit));
        for (int i = 1; i < 10; i++) {
            phoneNumberBuilder.append(new Random().nextInt(10));
        }

        String phone = phoneNumberBuilder.toString();
        String camId = randomWithRange( "32", 1000, 9999);

	    HTTPClientWrapper client = new HTTPClientWrapper();

        // -----------------------------
        // 2️⃣ Create Account (label-driven)
        // -----------------------------
	    Map<String, Object> accountData = new HashMap<>();

	 // Required / Unique fields
	    accountData.put("Name", accountName); 
	    accountData.put("Record Type ID", HTTPClientWrapper.getRecordTypeId("Account", "Prospect"));
	    accountData.put("OwnerId", HTTPClientWrapper.getUserIdByRole("AgencySeller"));

	    // Standard fields
	    accountData.put("Annual Potential Spend", 1);
	    accountData.put("Billing City", "Stamford");
	    accountData.put("Billing Country", "United States");
	    accountData.put("Billing Country Code", "US");
	    accountData.put("Billing Zip/Postal Code", "06905");
	    accountData.put("Billing State/Province", "Connecticut");
	    accountData.put("Billing State/Province Code", "CT");
	    accountData.put("Billing Street", "1177 High Ridge Road");
	    accountData.put("Account Description", "WebomatesTest");
	    accountData.put("Industry", "ALCO29828");
	    accountData.put("Employees", 15);
	    accountData.put("Account Phone", phone);
	    accountData.put("Potential Spend", 100000);
	    accountData.put("Shipping City", "Stamford");
	    accountData.put("Shipping Country", "United States");
	    accountData.put("Shipping Country Code", "US");
	    accountData.put("Shipping Zip/Postal Code", "06905");
	    accountData.put("Shipping State/Province", "Connecticut");
	    accountData.put("Shipping State/Province Code", "CT");
	    accountData.put("Shipping Street", street);
	    accountData.put("Total Media Budget", 65000);
	    accountData.put("Website", website);

	    // Custom fields
	    accountData.put("Billing Detail", "Co-Op");
	    accountData.put("Billing Type", "Broadcast");
	    accountData.put("Client Segment", "Multiscreen");
	    accountData.put("Customer Threshold", "SMB");
	    accountData.put("Finance Approval Status", "");
	    accountData.put("Prospect Type", "Advertiser");
	    accountData.put("vlocity_cmt__BillCycle__c", 1);
	    accountData.put("vlocity_cmt__BillDeliveryMethod__c", "Paper Billing");
	    accountData.put("vlocity_cmt__BillFormat__c", "Detail");
	    accountData.put("vlocity_cmt__BillFrequency__c", "Weekly");
	    accountData.put("vlocity_cmt__BillingEmailAddress__c", email);
	    accountData.put("vlocity_cmt__CreditRating__c", "Good");
	    accountData.put("vlocity_cmt__CreditScore__c", 5);
	    accountData.put("vlocity_cmt__TaxID__c", "");
	    accountData.put("CAM_ID__c", camId);

	 // Create the account
	 JSONObject createdAccount = client.createByLabels("Account", accountData);

	 // Get the Id back
	 String accountId = createdAccount.getString("id");
	 System.out.println("Created Account Id: " + accountId);
	    try {
	    // UI login + navigate
	    lightningloginpage.openHomepage(appUrl);
	    lightningloginpage.loginWithRole("SystemAdmin");
	    lightningloginpage.applauncher("Account");

		String recordID = objectlistpage.getRecordIdFromUiLabel_Optimized("Account", "Account Name", accountName);
		objectlistpage.NavigateToRecord("Account", recordID);
		objectlistpage.uiApiParser(recordID);

		objectlistpage.clickEditByFieldLabel("Status");
		objectlistpage.formValueFiller("Status", "Active");
		objectlistpage.clickSave();

		objectlistpage.assertFieldLabelAndValue("Prospect Type", "Advertiser");
		objectlistpage.assertFieldLabelAndValue("Account Record Type", "Prospect");
		objectlistpage.assertFieldLabelAndValue("Status", "Active");
		objectlistpage.assertFieldLabelAndValue("Industry", "ALCOH BEVS-BEER");
		objectlistpage.assertFieldLabelAndValue("Shipping Address",
				street + "\nStamford, Connecticut 06905\nUnited States");
		objectlistpage.assertFieldLabelAndMap("Shipping Address", 30);
		objectlistpage.assertFieldLabelAndValue("Website", website);

		objectlistpage.clickEditByFieldLabel("Account Name");

		objectlistpage.assertFormValue("Prospect Type", "Advertiser");
		objectlistpage.assertFormValue("Shipping Street", street);
		// locator making problem
		 objectlistpage.assertFormValue("Shipping State/Province Code", "Stamford");
		 objectlistpage.assertFormValue("Shipping Zip/Postal Code", "06905");
		// expected is below and actual is blank showing
		 objectlistpage.assertFormValue("Account Name", accountName);
		 objectlistpage.assertFormValue("Website", website);

		// filling blank but it is not filling blank
		objectlistpage.formValueFillerClearInput("Account Name");

		objectlistpage.clickSave();

		// objectlistpage.assertFormValue("Account Name", "Complete this field");
		objectlistpage.assertFormValueSnags("Account Name", "Account Name");

		objectlistpage.formValueFiller("Account Name", "WeboTest01");
		// not working
		// objectlistpage.formValueFiller("Industry","--None--");
		// objectlistpage.formValueFiller("Phone","9424312578");

		objectlistpage.clickSave();

		objectlistpage.assertFieldLabelAndValue("Account Name", "WeboTest01");
		objectlistpage.assertFieldLabelAndValue("Advertiser Type", "");

		objectlistpage.clickEditByFieldLabel("Account Name");

		// not working this form filler
		// objectlistpage.formValueFiller("Advertiser Type","Local");

		driver.findElement(By.xpath(
				"//div[contains(@class, 'active')]//div[normalize-space()='Advertiser Type']/parent::div//span[normalize-space()='Available']/following-sibling::div//li[@role='presentation']//span/span[normalize-space()='Local']"))
				.click();
		objectlistpage.moveSelectiontoChoosen();
		objectlistpage.clickSave();

		objectlistpage.assertFieldLabelAndValue("Advertiser Type", "Local");
		// website locator not working
		// objectlistpage.assertFieldLabelAndValue("Website", "www.FRARK3285.com");

		Assert.assertEquals(driver.findElement(By.xpath("//lightning-formatted-url")).getText(), "www.FRARK3285.com");
		objectlistpage.assertFieldLabelAndValue("Industry", "ALCOH BEVS-BEER");

		objectlistpage.clickEditByFieldLabel("Status");

		objectlistpage.formValueFiller("Status", "New");
		objectlistpage.clickSave();

		objectlistpage.assertFieldLabelAndValue("Status", "New");
	    }catch (Exception e) {
			
		client.deleteRecord("Account", accountId);
        System.out.println("\nDeleted Account: " + accountId);

        // -----------------------------
        // 5️⃣ Verify deletion
        // -----------------------------
        JSONObject deletedCheck = HTTPClientWrapper.runGetRequest("/sobjects/Account/" + accountId);
        if (deletedCheck == null) {
            System.out.println("Verified: Account successfully deleted.");
        } else {
            System.out.println("Account still exists!");
        }

        // -----------------------------
        // Logout
        // -----------------------------
//        HTTPClientWrapper.SFLogout_API();
        throw e;
	    }

	}
	
	

}
