package testscripts;
//TODO
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import base.BaseTest;
import pageobjects.ObjectListPage;
import utils.HTTPClientWrapper;
import utils.MetadataCache.QuickActionContext;

public class AgencySeller_ContactsManagement_ContactDetails_Chatter_Access extends BaseTest {

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
		String randomWord = randomLetters(5);
	    String FirstName = "WebomatesFirst" + new Random().nextInt(10000);
		String accountName = "Webomates " + randomLetters +" "+ new Random().nextInt(1000000);
		String accountName2 = "Webomates " + randomLetters +" "+ new Random().nextInt(1000000);
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
        String opportunityName = "Webomates " + randomLetters +" "+ new Random().nextInt(1000000);
        String closeDate = objectlistpage.getCurrentDateWithCustomFormat("M/dd/yyyy",5,"IST");
        String requestedDueDate = objectlistpage.getCurrentDateWithCustomFormat("M/dd/yyyy",2,"IST");
        String clientChallengesAndNeeds = randomWithRange("Webomates", 100, 9999); 
        		
	    HTTPClientWrapper client = new HTTPClientWrapper();

        // -----------------------------
        // 2️⃣ Create Account (label-driven)
        // -----------------------------
	    Map<String, Object> accountData = new HashMap<>();

	 // Required / Unique fields
	    accountData.put("Name", accountName); 
	    accountData.put("Record Type ID", HTTPClientWrapper.getRecordTypeId("Account", "Advertiser"));
	    accountData.put("OwnerId", HTTPClientWrapper.getUserIdByRole("EnterpriseSeller"));

	    // Standard fields
	    accountData.put("Annual Potential Spend", 1);
	    accountData.put("Billing City", "Stamford");
	    accountData.put("Billing Country", "United States");
	    accountData.put("Billing Country Code", "US");
	    accountData.put("Billing Zip/Postal Code", "06905");
	    accountData.put("Billing State/Province", "Connecticut");
	    accountData.put("Billing State/Province Code", "CT");
	    accountData.put("Billing Street", street);
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
	    accountData.put("Total Media Spend", 65000);
	    accountData.put("Website", website);

	    // Custom fields
//	    accountData.put("Billing Detail", "Co-Op");
//	    accountData.put("Billing Type", "Broadcast");
	    accountData.put("Client Segment", "Multiscreen");
	    accountData.put("Customer Threshold", "SMB");
	    accountData.put("Finance Approval Status", "");
	    accountData.put("Prospect Type", "");
	    accountData.put("vlocity_cmt__BillCycle__c", 1);
	    accountData.put("vlocity_cmt__BillDeliveryMethod__c", "Paper Billing");
	    accountData.put("vlocity_cmt__BillFormat__c", "Detail");
	    accountData.put("vlocity_cmt__BillFrequency__c", "Weekly");
	    accountData.put("vlocity_cmt__BillingEmailAddress__c", email);
	    accountData.put("vlocity_cmt__CreditRating__c", "Good");
	    accountData.put("vlocity_cmt__CreditScore__c", 5);
	    accountData.put("vlocity_cmt__TaxID__c", "");
	    accountData.put("CAM_ID__c", camId);
	    accountData.put("vlocity_cmt__Status__c", "Active");

	 // Create the account
	 JSONObject createdAccount = client.createByLabels("Account", accountData);

	 // Get the Id back
	 String accountId = createdAccount.getString("id");
	 System.out.println("Created Account Id: " + accountId);
		try {
			
			// --------------- Testcase begins from here ------------------------
			
			// Login as Enterprise Seller
			
			lightningloginpage.openHomepage(appUrl);
			lightningloginpage.loginWithRole("EnterpriseSeller");
			lightningloginpage.applauncher("Account");
		    

		    // Open Account record directly
			objectlistpage.NavigateToRecord("Account", accountId);

		    // Create Opportunity
		    objectlistpage.clickQuickAction("New Opportunity");
		    QuickActionContext.setCurrentSObject("Opportunity");
			objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Opportunity Name", opportunityName);
			objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Amount", "2000");
			objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Close Date", closeDate);
			objectlistpage.clickButton("Next");
			objectlistpage.clickButton("Finish");

			QuickActionContext.setCurrentSObject("Opportunity");
		    
//		    // Submit Sales Dev Request
		    objectlistpage.clickQuickAction("Sales Dev Request");
		    QuickActionContext.setCurrentSObject("Case");
		    objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Requested Due Date", requestedDueDate);
		    objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Client Challenges & Needs", clientChallengesAndNeeds);
		    objectlistpage.FillFormValueUsingSalesforceAPIMetadata("Deliverables Needed", "1:1 Collateral");
		    objectlistpage.clickButton("Next");
		    objectlistpage.clickButton("Submit");

		    // Verify Opportunity and Chatter
		    objectlistpage.assertFieldLabelAndValue("Opportunity", opportunityName);
		    objectlistpage.assertTabEnabled("Chatter");
		    objectlistpage.clickTab("Chatter");
		    objectlistpage.clickButton("Share an update...");
		    objectlistpage.assertButtonDisabled("Share");
		    objectlistpage.performChatterAction("Post", "@Sales Development Webomates");
		    objectlistpage.assertToastMessageContains("Your update was shared.");
		    objectlistpage.assertChatterPostValueContains("EnterpriseSeller Webomates");
		    objectlistpage.assertChatterPostValueContains("@Sales Development Webomates");
//
//		    // Chatter dropdown actions
		    objectlistpage.clickChatterPostShowActionButton("EnterpriseSeller Webomates");
		    objectlistpage.assertShowActionDropdownEquals("Like on Chatter,Edit on Chatter,Delete on Chatter,Bookmark");
		    objectlistpage.clickButton("Like on Chatter");
		    objectlistpage.assertToastMessageContains("Post was liked.");
		    objectlistpage.assertButtonEnabled("Comment");
		    objectlistpage.clickButton("Comment");
		    objectlistpage.enterValueUsingScript("ChatterComment", randomWord);
		    objectlistpage.assertButtonEnabled("Comment");
		    objectlistpage.clickButton("Comment");
//
//		    // Refresh and delete post
		    objectlistpage.refreshPage();
		    objectlistpage.clickTab("Chatter");
		    objectlistpage.clickChatterPostShowActionButton("EnterpriseSeller Webomates");
		    objectlistpage.assertShowActionDropdownContains("Unlike on Chatter");
		    objectlistpage.clickButton("Delete on Chatter");
		    objectlistpage.assertModalHeader("Delete post and comments?");
		    objectlistpage.assertModalMessage("Deleting this item permanently removes it. We're just making sure that's what you want.");
		    objectlistpage.assertButtonEnabled("Delete on Chatter");
		    objectlistpage.clickButton("Delete on Chatter");
		    objectlistpage.assertNotVisible(driver.findElement(By.xpath(objectlistpage.getChatterPost("EnterpriseSeller Webomates"))) );
//
//		    // Logout
		    objectlistpage.clickUserProfileImage();
		    objectlistpage.clickButton("Log Out");
//
//		    // Login as Sales Development
			lightningloginpage.loginWithRole("SalesDevelopment");
			lightningloginpage.applauncher("Home");
			
//		    // Verify notification

		    objectlistpage.clickNotificationIcon();
		    objectlistpage.assertModalHeader("EnterpriseSeller Webomates mentioned you in a post");
		    objectlistpage.assertModalMessage("@Sales Development Webomates");

		} catch (Exception e) {

			client.deleteRecord("Account", accountId);
			System.out.println("\nDeleted Account: " + accountId);

			JSONObject deletedCheck = (JSONObject) HTTPClientWrapper.runGetRequest("/sobjects/Account/" + accountId);
			if (deletedCheck == null) {
				System.out.println("Verified: Account successfully deleted.");
			} else {
				System.out.println("Account still exists!");
			}

			throw e;
		}

	}
	
	

}
