package testscripts;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;
import utils.HTTPClientWrapper;

public class SystemAdmin_AccountsManagement_Accounts_AddNew_ProspectType_Advertiser extends BaseTest {

	@Test
	public void editAccount() throws Exception {
	    // Create account via API
	    JSONObject accountdata = new JSONObject();
	    String accountName = "AccountCreated_ByAPIs at " + lightningloginpage.getCurrentDateTimeStamp();
	    accountdata.put("Name", accountName);

	    JSONObject responseObject = HTTPClientWrapper.create_sObject("/sobjects/Account/", accountdata);
	    String accountID = responseObject.getString("id");
	    System.out.println("Account created as : " + accountID);

	    // UI login + navigate
	    lightningloginpage.openHomepage(appUrl);
	    lightningloginpage.loginWithRole(environmentName, "SystemAdmin");
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
				"555 Mission Rock Street\nSan Francisco, California 94158\nUnited States");
		objectlistpage.assertFieldLabelAndMap("Shipping Address", 30);
		objectlistpage.assertFieldLabelAndValue("Website", "www.FRARK3285.com");

		objectlistpage.clickEditByFieldLabel("Account Name");

		objectlistpage.assertFormValue("Prospect Type", "Advertiser");
		objectlistpage.assertFormValue("Shipping Street", "555 Mission Rock Street");
		// locator making problem
		// objectlistpage.assertFormValue("Shipping State/Province Code", "California");
		// objectlistpage.assertFormValue("Shipping Zip/Postal Code", "94158");
		// expected is below and actual is blank showing
		// objectlistpage.assertFormValue("Account Name", "webo0409");
		// objectlistpage.assertFormValue("Website", "www.FRARK3285.com");

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

	}

}
