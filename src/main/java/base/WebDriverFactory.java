package base;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.ProfilesIni;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import io.github.bonigarcia.wdm.WebDriverManager;

public class WebDriverFactory {

    static Logger log = LogManager.getLogger(WebDriverFactory.class);
    public final static String windowXPositionKey = "xpos";
    public final static String windowYPositionKey = "ypos";

    public static WebDriver startInstance(String browserName) {
        WebDriver driver = null;
        try {
            URL hubUrl = null; // set hubURL if using Selenium Grid
            driver = WebDriverFactory.createInstance(hubUrl, browserName);

            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
            driver.manage().window().maximize();

            int xPosition = Integer.parseInt(System.getProperty(windowXPositionKey, "0"));
            int yPosition = Integer.parseInt(System.getProperty(windowYPositionKey, "0"));
            driver.manage().window().setPosition(new Point(xPosition, yPosition));
        } catch (Exception e) {
            log.error("Exception creating driver instance", e);
        }
        return driver;
    }

    static WebDriver createInstance(URL hubUrl, String browserName) throws IOException {
        WebDriver driver = null;

        if (browserName.equalsIgnoreCase("firefox")) {
            WebDriverManager.firefoxdriver().setup();
            driver = new FirefoxDriver(createFirefoxProfile());

        } else if (browserName.equalsIgnoreCase("chrome")) {
            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();
            options.addArguments("--disable-notifications");
            options.addArguments("--start-maximized");
            options.addArguments("--enable-automation");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-infobars");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-browser-side-navigation");
            options.addArguments("--disable-gpu");
            options.setPageLoadStrategy(PageLoadStrategy.NORMAL);

            driver = new org.openqa.selenium.chrome.ChromeDriver(options);

        } else if (browserName.equalsIgnoreCase("ie")) {
            WebDriverManager.iedriver().setup();

            InternetExplorerOptions options = new InternetExplorerOptions();
            options.ignoreZoomSettings();
            options.introduceFlakinessByIgnoringSecurityDomains();
            options.destructivelyEnsureCleanSession();
            options.withInitialBrowserUrl("http://www.bing.com/");

            driver = new InternetExplorerDriver(options);

        } else if (browserName.equalsIgnoreCase("safari") && isSafariSupportedPlatform()) {
            driver = new SafariDriver();

        } else if (browserName.equalsIgnoreCase("remote-chrome")) {
            ChromeOptions options = new ChromeOptions();
            driver = new RemoteWebDriver(hubUrl, options);

        } else if (browserName.equalsIgnoreCase("remote-firefox")) {
            FirefoxOptions options = new FirefoxOptions();
            driver = new RemoteWebDriver(hubUrl, options);

        } else if (browserName.equalsIgnoreCase("remote-ie")) {
            InternetExplorerOptions options = new InternetExplorerOptions();
            driver = new RemoteWebDriver(hubUrl, options);

        } else if (browserName.equalsIgnoreCase("remote-safari")) {
            driver = new RemoteWebDriver(hubUrl, new SafariDriver().getCapabilities());
        }

        log.info("WebDriverFactory created an instance of WebDriver for: " + browserName);
        return driver;
    }

    static boolean isSafariSupportedPlatform() {
        Platform current = Platform.getCurrent();
        return Platform.MAC.is(current) || Platform.WINDOWS.is(current);
    }

    static FirefoxOptions createFirefoxProfile() {
        ProfilesIni profileIni = new ProfilesIni();
        FirefoxProfile profile = profileIni.getProfile("default");
        FirefoxOptions options = new FirefoxOptions();

        if (profile != null) {
            profile.setPreference("dom.max_chrome_script_run_time", 60);
            profile.setPreference("setTimeoutInSeconds", 60);
            profile.setPreference("dom.max_script_run_time", 60);
            profile.setPreference("dom.popup_maximum", 0);
            profile.setPreference("privacy.popups.disable_from_plugins", 3);
            profile.setPreference("browser.xul.error_pages.enabled", false);
            profile.setPreference("general.useragent.extra.firefox", "Firefox");
            profile.setAcceptUntrustedCertificates(true);
            options.setProfile(profile);
        }

        return options;
    }
}
