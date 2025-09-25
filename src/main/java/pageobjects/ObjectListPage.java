package pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class ObjectListPage extends utils.SFPageBase {

	
	@FindBy(xpath = "//span[@class=\"photoContainer forceSocialPhoto\"]")
	private WebElement userProfileImage;
	
	@FindBy(xpath = "//lightning-icon[@icon-name='utility:notification']")
	private WebElement notificationIcon;
	
	
	public ObjectListPage(WebDriver webDriver) {
		super(webDriver);
		PageFactory.initElements(driver, this);// Creates instance for all web elements
	}

	
	public void clickUserProfileImage() throws InterruptedException {

        clickButton(userProfileImage);

	}
	
	public void clickNotificationIcon() throws InterruptedException {
		
		SFClick(notificationIcon);
		
		Thread.sleep(3000);
		
	}
	
}
