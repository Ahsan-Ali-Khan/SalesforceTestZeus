package base;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
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

    public static WebDriver createInstance(URL hubUrl, String browserName) throws IOException {
        WebDriver driver = null;

        switch (browserName.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().clearDriverCache().setup();
                FirefoxOptions ffOptions = new FirefoxOptions();
                driver = new FirefoxDriver(ffOptions);
                break;

            case "chrome":
                WebDriverManager.chromedriver().clearDriverCache().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--disable-notifications",
                                           "--start-maximized",
                                           "--enable-automation",
                                           "--no-sandbox",
                                           "--disable-infobars",
                                           "--disable-dev-shm-usage",
                                           "--disable-browser-side-navigation",
                                           "--disable-gpu");
                chromeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                driver = new ChromeDriver(chromeOptions);
                break;

            case "edge":
                WebDriverManager.edgedriver().clearDriverCache().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                driver = new EdgeDriver(edgeOptions);
                break;

            case "ie":
                WebDriverManager.iedriver().clearDriverCache().setup();
                InternetExplorerOptions ieOptions = new InternetExplorerOptions();
                ieOptions.ignoreZoomSettings();
                ieOptions.introduceFlakinessByIgnoringSecurityDomains();
                ieOptions.destructivelyEnsureCleanSession();
                driver = new InternetExplorerDriver(ieOptions);
                break;

            case "safari":
                if (isSafariSupportedPlatform()) {
                    driver = new SafariDriver();
                } else {
                    throw new UnsupportedOperationException("Safari is only supported on macOS");
                }
                break;

            case "remote-chrome":
                WebDriverManager.chromedriver().clearDriverCache().setup();
                driver = new RemoteWebDriver(hubUrl, new ChromeOptions());
                break;

            case "remote-firefox":
                WebDriverManager.firefoxdriver().clearDriverCache().setup();
                driver = new RemoteWebDriver(hubUrl, new FirefoxOptions());
                break;

            case "remote-edge":
                WebDriverManager.edgedriver().clearDriverCache().setup();
                driver = new RemoteWebDriver(hubUrl, new EdgeOptions());
                break;

            default:
                throw new IllegalArgumentException("Unsupported browser: " + browserName);
        }

        System.out.println(" WebDriverFactory created driver for: " + browserName +
                           " | Browser version: " +
                           ((HasCapabilities) driver).getCapabilities().getBrowserVersion());

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
