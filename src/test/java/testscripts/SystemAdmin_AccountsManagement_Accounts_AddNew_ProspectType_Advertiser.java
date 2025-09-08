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
				lightningloginpage.login(SFUserId, SFPassword);
				lightningloginpage.applauncher("Account");
				
				String recordID = objectlistpage.getRecordIdFromUiLabel_Optimized("Account", "Account Name", "webo0409");
				objectlistpage.NavigateToRecord("Account", recordID);
				objectlistpage.uiApiParser(recordID);
				
				objectlistpage.clickEditByFieldLabel("Status");
				objectlistpage.formValueFiller("Status","Active");
				objectlistpage.clickSave();
				
				objectlistpage.assertFieldLabelAndValue("Prospect Type", "Prospect");
				
//				objectlistpage.a
				
	} 

}
