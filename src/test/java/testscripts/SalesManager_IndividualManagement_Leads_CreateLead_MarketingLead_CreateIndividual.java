package testscripts;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import base.BaseTest;


public class SalesManager_IndividualManagement_Leads_CreateLead_MarketingLead_CreateIndividual extends BaseTest {
	
	@Test
	public void editAccount() throws Exception {
		
		// Navigation to login page
		lightningloginpage.openHomepage(SFBaseURL);
		lightningloginpage.login(SFUserId, SFPassword);
		//lightningloginpage.applauncher("Account");
		
//		String recordID = objectlistpage.getRecordIdFromUiLabel_Optimized("Lead", "Lead Name", "WebomatesFirst7055");
//		objectlistpage.NavigateToRecord("Lead", recordID);
//		objectlistpage.uiApiParser(recordID);
		
			
			objectlistpage.clickLeadsTab();
			objectlistpage.clickNewLeads();
		
			//	objectlistpage.formValueFiller("First Name", "webo0409");
			//	objectlistpage.formValueFiller("Last Name", "webo0409");
			//	objectlistpage.formValueFiller("Email", "webo@gmail.com");
			//	objectlistpage.formValueFiller("Business Name", "webomates1241");
			
			String firstName = "webo3310t2315";
			String lastName = "webo3310t2315";
			String email = "webo3310t2215@gmail.com";
			String fullName = firstName +" "+lastName;
			String business = "webo10104NB";
			driver.findElement(By.xpath("//div[contains(@class, 'active')]//input[@name='firstName']")).sendKeys(firstName);
			driver.findElement(By.xpath("//div[contains(@class, 'active')]//input[@name='lastName']")).sendKeys(lastName);
			driver.findElement(By.xpath("//div[contains(@class, 'active')]//input[@name='Email']")).sendKeys(email);
			driver.findElement(By.xpath("//input[@name='Company']")).sendKeys(business);
			
			objectlistpage.clickSave();

			//objectlistpage.assertFormValue("Lead Name", "webo0409 webo0409");
			//objectlistpage.assertFormValue("Email", "webo@gmail.com");
			//objectlistpage.marketingTab("Indivisual", "Individual");
			//objectlistpage.assertEquals("Lead Name", "webo0409 webo0409");
			
			Thread.sleep(10000);
			
			Assert.assertEquals(driver.findElement(By.xpath("(//div[@data-target-selection-name='sfdc:RecordField.Lead.Name']//lightning-formatted-name)[last()]")).getText(), fullName);
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
		
		
				
	} 

	
}
