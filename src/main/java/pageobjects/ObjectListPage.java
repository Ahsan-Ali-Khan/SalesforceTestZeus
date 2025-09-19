package pageobjects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;

public class ObjectListPage extends utils.SFPageBase {

	@FindBy(xpath = "//a[@title='New']")
	private WebElement newbutton;

	@FindBy(xpath = "//button[@name='SaveEdit']")
	private WebElement savebutton;
	
	@FindBy(xpath = "//button[normalize-space()='Next']")
	private WebElement nextButton;
	
	@FindBy(xpath = "//button[normalize-space()='Finish']")
	private WebElement finishButton;
	
	@FindBy(xpath = "//div[text()='Advertiser Type']/following-sibling::div//button[@title='Move to Chosen']")
	private WebElement moveSelectiontoChoosen;
	
	@FindBy(xpath = "//div[@class='slds-grid']//span[text() = 'Recently Viewed']/following::lightning-icon[@icon-name='utility:down'][2]")
	private WebElement showmoreactions;
	
	@FindBy(xpath = "(//a[@title='Leads']/span[text()='Leads'])[last()]")
	private WebElement clickOnLeadsTab;
	
	@FindBy(xpath = "//div[contains(@class, 'active')]//a[@title='New']")
	private WebElement clickNewLeads;
	
	@FindBy(xpath = "//div[contains(@class,'active')]//a[text()='Marketing']")
	private WebElement clickMarketingTab;
	
	@FindBy(xpath = "//li[@title='Details' and @class='slds-tabs_default__item slds-is-active'] | //a[@id='detailTab__item']")
	private WebElement clickDetailsTab;
	
	public ObjectListPage(WebDriver webDriver) {
		super(webDriver);
		PageFactory.initElements(driver, this);// Creates instance for all web elements
	}

	/**
	 *
	 * @author Robin 28-9-2021
	 * @return the SF Account List page class instance.
	 * @throws InterruptedException
	 */
	public void clickNew() throws InterruptedException {

		Thread.sleep(5000);
		SFClick(newbutton);

		waitForSFPagetoLoad();

	}
	
	public void clickShowMoreActions() throws InterruptedException {

		try{
			SFClick(showmoreactions);
			}
		catch(Exception e) {
			System.out.println("Show more button not shown on list view");
		}
	

		waitForSFPagetoLoad();

	}
	
	

	public void clickSave() throws InterruptedException {

		SFClick(savebutton);

		Thread.sleep(5000);

	}
	
	public void moveSelectiontoChoosen() throws InterruptedException {

		SFClick(moveSelectiontoChoosen);

		Thread.sleep(5000);

	}
	
	public void clickLeadsTab() throws InterruptedException {

		SFClick(clickOnLeadsTab);

		Thread.sleep(5000);

	}
	
	public void clickNewLeads() throws InterruptedException {

		SFClick(clickNewLeads);

		Thread.sleep(5000);

	}

	public void clickMarketingTab() throws InterruptedException {

		SFClick(clickMarketingTab);

		Thread.sleep(5000);

	}
	
	public void clickDetailsTab() throws InterruptedException {

		SFClick(clickDetailsTab);

		Thread.sleep(5000);

	}
	
}
