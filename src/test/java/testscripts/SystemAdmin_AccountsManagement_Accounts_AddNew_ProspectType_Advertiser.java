package testscripts;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import base.BaseTest;

public class SystemAdmin_AccountsManagement_Accounts_AddNew_ProspectType_Advertiser extends BaseTest {
	
	@Test
	public void editAccount() throws Exception {
		
		// Navigation to login page
				lightningloginpage.openHomepage(SFBaseURL);
				// Submitting user id, password and logging in
				lightningloginpage.login(SFUserId, SFPassword);
				// Navigating directly to Account app
				lightningloginpage.applauncher("Account");
				objectlistpage.clickOnGlobalSerachTextbox("Search");
				objectlistpage.clickSelectAllDownArrow("Search: All");
				objectlistpage.clickListboxOption("Accounts");
				WebElement searchElement= driver.findElement(By.xpath("//input[@placeholder='Search...']"));
				objectlistpage.enterValue(searchElement, "webo0409");
				System.out.println(objectlistpage.getTextOfElement(searchElement));
				Thread.sleep(2000);
				objectlistpage.selectFirstSuggestedValue();
				
				String recordid = "001VZ00000Ub9PfYAJ";
				objectlistpage.uiApiParser(recordid);
				
				objectlistpage.clickEditByFieldLabel("Account Name");
				objectlistpage.implicitWait(5);
				objectlistpage.formValueFiller("Status","Active");
				objectlistpage.clickSave();
				System.out.println("Thank you :) ");
		
	} 

}
