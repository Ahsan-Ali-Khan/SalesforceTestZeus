package testscripts;

import org.testng.annotations.Test;

import base.BaseTest;
import utils.SFPageBase;

/**
 * @author Robin
 * @date: 28/09/2021
 * @purpose: This test covers the login to UI, fetches the UI API details and
 *           prints them to console 👼
 */

public class LeadCreationViaUI extends BaseTest {

	@Test(priority = 1)
	public void createLead() throws Exception {

		// Navigation to login page
		lightningloginpage.openHomepage(SFBaseURL);
		// Submitting user id, password and logging in
		lightningloginpage.login(SFUserId, SFPassword);
		// Navigating directly to Lead list page
		lightningloginpage.applauncher("Lead");
		objectlistpage.clickShowMoreActions();
		objectlistpage.clickNew();

		String recordid = "00QVZ00000F0uuP2AR";
		objectlistpage.uiApiParser(recordid);
		
		// Form data can be passed directly on the new sObject creation screen
		objectlistpage.formValueFiller("Salutation",
				"Mr.");
		objectlistpage.formValueFiller("First Name",
				"Webo");
        objectlistpage.formValueFiller("Email",
				"Test1234235@gmail.com");
        objectlistpage.formValueFiller("Business Legal Name",
        		"Test Business Legal Name");

        objectlistpage.formValueFiller("Last Name",
        		"Test");

		objectlistpage.clickSave();
		System.out.println("Thank you :) ");

	}
}
