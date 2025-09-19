package testscripts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONObject;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import base.BaseTest;
import utils.HTTPClientWrapper;

public class AgencySeller_OpportunityManagement_OpportunityDetails_Workflow_ClosedWon extends BaseTest {

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
        String phone2 = phoneNumberBuilder.toString();
        String camId = randomWithRange( "32", 1000, 9999);
		String opportunityName = "Webomates " + randomLetters +" "+ new Random().nextInt(1000000);
		String flightEndDate = objectlistpage.getCurrentDateWithCustomFormat("M/dd/yyyy",5,"IST");
		String flightStartDate = objectlistpage.getCurrentDateWithCustomFormat("M/dd/yyyy",0,"IST");
		String closeDate = objectlistpage.getCurrentDateWithCustomFormat("M/dd/yyyy",5,"IST");

	    HTTPClientWrapper client = new HTTPClientWrapper();

        // -----------------------------
        // 2️⃣ Create Account (label-driven)
        // -----------------------------
	    Map<String, Object> accountData = new HashMap<>();

	 // Required / Unique fields
	    accountData.put("Name", accountName); 
	    accountData.put("Record Type ID", HTTPClientWrapper.getRecordTypeId("Account", "Advertiser"));
	    accountData.put("OwnerId", HTTPClientWrapper.getUserIdByRole("AgencySeller"));

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
	    accountData.put("Billing Detail", "Co-Op");
	    accountData.put("Billing Type", "Broadcast");
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
			// UI login + navigate
			lightningloginpage.openHomepage(appUrl);
			lightningloginpage.loginWithRole("AgencySeller");
			lightningloginpage.applauncher("Account");

			String recordID = objectlistpage.getRecordIdByUiLabelAndValue("Account", "Account Name", accountName);
			objectlistpage.NavigateToRecord("Account", recordID);
			objectlistpage.assertFieldLabelAndValue("Status", "Active");
			objectlistpage.assertFieldLabelAndValue("Account Record Type", "Advertiser");
			objectlistpage.clickQuickAction("New Opportunity");
			objectlistpage.setCurrentObject("Opportunity");
			objectlistpage.formValueFiller("Opportunity Name", opportunityName);
			objectlistpage.formValueFiller("Amount", "2000");
			objectlistpage.formValueFiller("Close Date", closeDate);
			objectlistpage.formValueFiller("Flight Start Date", flightStartDate);
			objectlistpage.formValueFiller("Flight End Date", flightEndDate);
			objectlistpage.clickButton("Next");
			objectlistpage.assertModalMessage("Opportunity " + opportunityName + " created successfully !!");
			objectlistpage.clickButton("Finish");
			objectlistpage.waitForSFPagetoLoad();
			objectlistpage.assertStageTabSelected("Discovery");
			objectlistpage.assertFieldLabelAndValue("Opportunity Name", opportunityName);
			objectlistpage.assertFieldLabelAndValue("Opportunity Owner", HTTPClientWrapper.getUserNameByRole("AgencySeller"));
			objectlistpage.assertFieldLabelAndValue("Amount", "$2,000.00");
			objectlistpage.assertFieldLabelAndValue("Close Date", closeDate);
			objectlistpage.assertFieldLabelAndValue("Flight Start Date", flightStartDate);
			objectlistpage.assertFieldLabelAndValue("Flight End Date", flightEndDate);
			objectlistpage.scrollEachSection( "[Opportunity Summary,Order Details,Makegood Parameters,Additional Details,System Information]" );
			objectlistpage.assertFieldLabelAndValue("Opportunity Record Type", "Tactic");
			objectlistpage.hardwait(2*60);
			objectlistpage.clickButton("Mark Stage as Complete");
			objectlistpage.assertToastMessageContains("Stage changed successfully.");
			
			//------- working till here -----

			// -------------------------
	        // 🔹 Stage: Building Solution
	        // -------------------------
	        objectlistpage.assertStageTabSelected("Building Solution");
	        
//	        objectlistpage.assertElementNotVisible("Media Planning Engaged");
//	        objectlistpage.assertElementNotVisible("Sales Development Engaged");
//	        objectlistpage.assertElementNotVisible("Pacesetting");
	        objectlistpage.assertFieldLabelAndValue("ROI", "");

	        objectlistpage.clickButton("Mark Stage as Complete");
	        objectlistpage.assertToastMessageContains("Please fill those fields in order to move to next stage : Amount, Flight Start, Flight End, Contact, Market and Opportunity Name.");

	        driver.navigate().refresh();
	        objectlistpage.waitForSFPagetoLoad();

	        // -------------------------
	        // 🔹 Add Contact Roles (Steps 53–65)
	        // -------------------------
	        objectlistpage.clickTab("Related");
	        objectlistpage.assertSectionHeaders("Contact Roles");
	        objectlistpage.clickButton("Add Contact Roles");
	        objectlistpage.enterSearchTextField("Webomates"); 
	        
//	        objectlistpage.selectFirstOption("Search Contacts");
	        objectlistpage.clickButton("Next");
//	        objectlistpage.clickField("Primary Contact");
//	        objectlistpage.selectSuggestedOption("Primary Contact");
//	        objectlistpage.clickDropdown("Role");
//	        objectlistpage.selectDropdownOption("Role", "Influencer");
	        objectlistpage.clickButton("Save");

	        // -------------------------
	        // 🔹 ROI Edit Flow (Steps 66–76)
	        // -------------------------
	        objectlistpage.clickEditByFieldLabel("ROI");
//	        objectlistpage.assertPicklistOptionsEqual("ROI", Arrays.asList("Instant Impact", "Multi-screen Impact"));
//	        objectlistpage.clickAvailableOption("ROI", "Instant Impact");
	        objectlistpage.clickButton("Move to Chosen");
//	        String ROI2 = objectlistpage.getChosenValue("ROI");
//	        objectlistpage.clickCheckbox("Pacesetting");
	        objectlistpage.clickButton("Save");
//	        objectlistpage.assertFieldLabelAndValue("ROI", ROI2);

	        // -------------------------
	        // 🔹 Verify Dates + Additional Details (77–79)
	        // -------------------------
	        objectlistpage.assertFieldLabelAndValue("Flight Start Date", flightStartDate);
	        objectlistpage.assertFieldLabelAndValue("Flight End Date", flightEndDate);
//	        objectlistpage.assertLabelsPresent("Additional Details",
//	                Arrays.asList("Media Planning Engaged", "Sales Development Engaged", "Pacesetting"));

	        // -------------------------
	        // 🔹 Add Market (80–83)
	        // -------------------------
	        objectlistpage.clickButton("Add Market");
	        objectlistpage.formValueFiller("Search Market", "New York");
//	        objectlistpage.selectOption("Market", "New York");
	        objectlistpage.clickButton("Next");

	        // -------------------------
	        // 🔹 Stage: Presenting Solution (84–87)
	        // -------------------------
	        objectlistpage.clickButton("Mark Stage as Complete");
	        objectlistpage.assertToastMessageContains("Stage changed successfully.");
	        objectlistpage.assertStageTabSelected("Presenting Solution");

	        driver.navigate().refresh();
	        objectlistpage.clickButton("Mark Stage as Complete");
	        objectlistpage.assertStageTabSelected("Negotiating Solution");

	        // -------------------------
	        // 🔹 Verify Empty Fields (91–98)
	        // -------------------------
	        objectlistpage.assertFieldLabelAndValue("Revenue Type", "");
	        objectlistpage.assertFieldLabelAndValue("Billing Type", "");
	        objectlistpage.assertFieldLabelAndValue("Makegood Approval", "");
	        objectlistpage.assertFieldLabelAndValue("Makegood Parameters - Linear", "");
	        objectlistpage.assertFieldLabelAndValue("Makegood Parameters - Digital", "");
	        objectlistpage.assertFieldLabelAndValue("Makegood Currency", "");

	        // -------------------------
	        // 🔹 Stage: Order Fulfillment (99–101)
	        // -------------------------
	        objectlistpage.clickButton("Mark Stage as Complete");
	        objectlistpage.assertStageTabSelected("Order Fulfillment");

	        // -------------------------
	        // 🔹 Edit Revenue / Billing / Makegood (102–129)
	        // -------------------------
	        objectlistpage.clickEditByFieldLabel("Revenue Type");
	        objectlistpage.formValueFiller("Revenue Type", "SomeOption");
	        objectlistpage.formValueFiller("Campaign Status", "In Progress");
	        objectlistpage.formValueFiller("Billing Type", "Co-Op");
	        objectlistpage.formValueFiller("Makegood Approval", "Co-Op");
//	        objectlistpage.assertPicklistOptionsEquals("Makegood Approval", "Not Allowed,Requires Seller Approval,Requires Client approval,Requires Agency Approval,Requires Rep Firm Approval,No Approval Needed within Campaign Flight");
	        objectlistpage.formValueFiller("Makegood Approval", "Not Allowed,Requires Seller Approval,Requires Client approval,Requires Agency Approval,Requires Rep Firm Approval,No Approval Needed within Campaign Flight");

//	        objectlistpage.assertPicklistOptionsEquals("Makegood Parameters - Linear",
//	                "Flexible,Same networks ordered/flexible daypart,Same networks ordered/same dayparts,Same Market/zones,Same Demo/Audience");
	        objectlistpage.formValueFiller("Makegood Parameters - Linear", "Flexible");

//	        objectlistpage.assertPicklistOptionsEqual("Makegood Parameters - Digital",
//	                Arrays.asList("Flexible", "Same Product Ordered", "Same Market/zones", "Same Demo/Audience"));
//	        objectlistpage.moveAvailableOptionToChosen("Makegood Parameters - Digital", "Flexible");
//	        objectlistpage.moveAvailableOptionToChosen("Makegood Parameters - Digital", "Same Product Ordered");

//	        objectlistpage.selectDropdownOption("Makegood Currency", "USD");

//	        objectlistpage.assertPicklistOptionsEqual("Won Reason",
//	                Arrays.asList("Competitive Pricing", "Strong Relationship", "Unique Value Proposition",
//	                        "Strong ROI Projections", "Product Differentiation"));
//	        objectlistpage.selectDropdownOption("Won Reason", "Competitive Pricing");
	        objectlistpage.clickButton("Save");

	        objectlistpage.assertFieldLabelAndValue("Makegood Parameters - Digital", "Flexible;Same Product Ordered");

	        // -------------------------
	        // 🔹 Campaign Status & Fulfillment (135–151)
	        // -------------------------
	        objectlistpage.clickEditByFieldLabel("Campaign Status");
//	        objectlistpage.assertPicklistOptionsEqual("Campaign Status",
//	                Arrays.asList("OneConnect – Submitted to CM", "Submitted to CM", "Revised to CM",
//	                        "Pending – Action Required", "In Progress", "Complete"));
//	        objectlistpage.selectDropdownOption("Campaign Status", "Revised to CM");
	        objectlistpage.clickButton("Save");

	        objectlistpage.clickEditByFieldLabel("Fulfillment Progress");
//	        objectlistpage.assertPicklistOptionsEqual("Fulfillment Progress",
//	                Arrays.asList("Ready for CIOC", "Assigned to CIOC", "CIOC rejected back to CM", "CIOC complete"));
//	        objectlistpage.selectDropdownOption("Fulfillment Progress", "Ready for CIOC");
	        objectlistpage.clickButton("Save");

	        objectlistpage.assertFieldLabelAndValue("Campaign Status", "Revised to CM");
	        objectlistpage.assertFieldLabelAndValue("Fulfillment Progress", "Ready for CIOC");

	        driver.navigate().refresh();
	        objectlistpage.clickEditByFieldLabel("Fulfillment Progress");
//	        objectlistpage.selectDropdownOption("Fulfillment Progress", "CIOC complete");
//	        objectlistpage.selectDropdownOption("Campaign Status", "Submitted to CM");
	        objectlistpage.clickButton("Save");
	        objectlistpage.assertFieldLabelAndValue("Campaign Status", "Complete");
	        objectlistpage.assertFieldLabelAndValue("Fulfillment Progress", "CIOC complete");

	        // -------------------------
	        // 🔹 Global Search Opportunity (161–168)
	        // -------------------------
//	        objectlistpage.navigateToTab("Opportunities");
//	        objectlistpage.globalSearchAndOpenRecord("Opportunity", opportunityName);

	        // -------------------------
	        // 🔹 Closed Won Negative Test (169–171)
	        // -------------------------
	        objectlistpage.clickButton("Mark Stage as Complete");
	        objectlistpage.clickButton("Done");
	        objectlistpage.assertToastMessageContains("You cannot mark Closed Won manually");

	        // -------------------------
	        // 🔹 ROI Second Edit (172–180)
	        // -------------------------
	        objectlistpage.scrollToBottom();
	        objectlistpage.clickEditByFieldLabel("ROI");
//	        objectlistpage.moveLastAvailableOptionToChosen("ROI");
//	        String NewROI = objectlistpage.getLastChosenValue("ROI");
	        objectlistpage.clickButton("Save");

	        driver.navigate().refresh();
//	        objectlistpage.assertFieldLabelContains("ROI", NewROI);

	        // -------------------------
	        // 🔹 Final Validations in List View (185–192)
	        // -------------------------
	        String CloseDate = objectlistpage.getRecordViewValue("Close Date");

//	        objectlistpage.navigateToList("Opportunities");
//	        objectlistpage.searchList("Opportunities", opportunityName);
//	        objectlistpage.assertTableCellValue("Opportunity Name", opportunityName);
//	        objectlistpage.assertTableCellValue("Stage", "Order Fulfillment");
//	        objectlistpage.assertTableCellValue("Closed Date", CloseDate);
//	        objectlistpage.assertTableCellValue("Owner", ownerName);

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
