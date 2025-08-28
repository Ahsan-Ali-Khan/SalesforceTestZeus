package pageobjects;

import java.time.Duration;
import java.util.List;

import org.openqa.selenium.Alert;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class LightningLoginPage extends testzeus.base.SFPageBase {

    @FindBy(id = "username")
    @CacheLookup
    private WebElement username;

    @FindBy(id = "password")
    @CacheLookup
    private WebElement password;

    @FindBy(id = "Login")
    @CacheLookup
    private WebElement login_button;

    // TODO: Add correct xpath for error messages if needed
    @FindBy(xpath = "//*[contains(@class,'error')]")
    private List<WebElement> sessionErrorMessage;

    public LightningLoginPage(WebDriver webDriver) {
        super(webDriver);
        PageFactory.initElements(webDriver, this); // use the parameter instead of raw driver
    }

    /**
     * Perform login into Salesforce Lightning.
     */
    public void login(String userid, String passwordtext) throws InterruptedException {
        // Use Selenium 4 wait instead of raw sleep
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(40));
        wait.until(ExpectedConditions.visibilityOf(username));

        username.clear();
        username.sendKeys(userid);

        password.clear();
        password.sendKeys(passwordtext);

        safeClick(login_button);

        try {
            // Handle alert popup if appears
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            System.out.println("Alert data: " + alertText);
            alert.accept();
        } catch (NoAlertPresentException e) {
            // ignore if no alert present
        }

        waitForSFPagetoLoad();
    }

    /**
     * Launch specific Salesforce app from App Launcher.
     */
    public void applauncher(String appname) throws InterruptedException {
        String accountappurl = getURL(appname);
        System.out.println("App URL is " + accountappurl);

        String cleanurl = accountappurl.replace("[\"", "").replace("\"]", "");
        System.out.println("Navigating to App URL as: " + cleanurl);

        openHomepage(cleanurl + "?eptVisible=1");
        waitForSFPagetoLoad();
    }
}
