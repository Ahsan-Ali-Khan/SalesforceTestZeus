package base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.MessagingException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.testng.ITestResult;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import pageobjects.ObjectListPage;
import pageobjects.AccountListPage_new_example;
import pageobjects.LightningLoginPage;
import utils.GetSFApps;
import utils.HTTPClientWrapper;
import utils.PageBase;

/*@author: Robin Gupta
@Date: 29 September 2021
@Purpose: All the test classes extend this base test , so as to carry forward the abstraction for page objects , webdriver setup and TEstNG level methods

*/

public class BaseTest implements ExcelReader, PropertyReader {

	public static final Logger logger = LogManager.getLogger(BaseTest.class);
	protected static WebDriver driver;

	private static final String InstalledVersionDetailPage = null;

	protected static Actions action;
	protected LightningLoginPage lightningloginpage;
	protected ObjectListPage objectlistpage;
	protected AccountListPage_new_example acne;

	public static String SFBaseURL; // This is the base URL like https://test-ea.lightning.force.com/

	protected static PageFactory pageFactory = null;
	protected Properties staticData = getStaticData();
	protected URL huburl = null;// Setup GRID hub URL here or from properties file
	protected static EmailUtils emailUtils;

	public static String env;
	public static String SFUserId;
	public static String SFPassword;
	public static String SFAPIGRANTSERVICE = "/services/oauth2/token?grant_type=password";
	public static String environmentName;
	public static String grantType;
	public static String SFAPIUSERNAME;
	public static String SFAPIPASSWORD;
	public static String appUrl;
	public static String apiUrl;
	public static String SFAPICLIENTID;
	public static String SFAPICLIENTSECRET;
	public static List<?> roles;

	@BeforeSuite(alwaysRun = true)
	@Parameters({ "browserType" })
	public void setupWebDriver(@Optional("chrome") String browserType) throws IOException {
		// Fetch all the test data like URL, UserID and Passwords from config.json file

		if ((driver == null)) {
			logger.info("setupWebDriver()");
			driver = WebDriverFactory.createInstance(huburl, browserType);
			action = new Actions(driver);
			pageFactory = new PageFactory(driver);

			driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
			driver.manage().window().maximize();

			System.out.println("Window width: " + driver.manage().window().getSize().getWidth());
			System.out.println("Window height: " + driver.manage().window().getSize().getHeight());
		}
	}

	@BeforeTest(alwaysRun = true)
	public void cleanTestSetup() {
		driver.manage().deleteAllCookies();
	}

	@BeforeClass(alwaysRun = true)
	@Parameters({ "environmentName", "role" })   // <-- pass from testng.xml
	protected void setUp(@Optional("NewQA") String environmentName, 
	                     @Optional("SystemAdmin") String role) throws MessagingException {
	    // Load env + role creds
	    readEnvironmentConfigJsonFile(environmentName, role);

	    // API login
	    HTTPClientWrapper.SFLogin_API(environmentName, role);

	    // Page objects
	    lightningloginpage = (LightningLoginPage) pageFactory.getPageObject(LightningLoginPage.class.getName());
	    objectlistpage = (ObjectListPage) pageFactory.getPageObject(ObjectListPage.class.getName());
	    acne = (AccountListPage_new_example) pageFactory.getPageObject(AccountListPage_new_example.class.getName());
	}

	@AfterMethod(alwaysRun = true)
	public void tearDownandCaptureScreenShot(Method method, ITestResult result) { // Method for taking screenshots on
																					// failure of the test case
		if (ITestResult.FAILURE == result.getStatus()) {
			try {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
				String currentdatetime = simpleDateFormat.format(new Date());
				File source = captureScreenShot();
				FileUtils.copyFile(source, new File(System.getProperty("user.dir")
						+ "/target/surefire-reports/FailedScreenShots/" + result.getName() + currentdatetime + ".png"));
				Reporter.log("Screenshot taken");
			} catch (Exception e) {

				Reporter.log("Exception while taking screenshot " + e.getMessage());
			}
		}
		logger.info("*************");
		logger.info("Ending Test  ---->" + method.getName());

	}

	@AfterClass(alwaysRun = true)
	public void deleteAllCookies() {
		// Logging out of the Salesforce APIs
		HTTPClientWrapper.SFLogout_API();

		// Handling windows after executing each class from Suite
		try {

			String originalHandle = driver.getWindowHandle();

			for (String handle : driver.getWindowHandles()) {
				if (!handle.equals(originalHandle)) {
					driver.switchTo().window(handle);
					driver.close();
				}
			}

			driver.switchTo().window(originalHandle);

		} catch (Exception e) {

			Reporter.log("Error while closing child windows" + e.getMessage());

		}

		logger.info("Clearing all browser cookies...");
		driver.manage().deleteAllCookies();

	}

	@AfterSuite(alwaysRun = true)
	public void quitWebDrivers() {
		logger.info("terminateWebDrivers()");
		try {
			driver.close();
			driver.quit();
			// Setting driver to null for stopping persistent use of driver
			// session across browsers
			driver = null;
		} catch (Exception e) {
			// Sometime driver.quit() causes exception and not nullifying the
			// driver obj. Which stops next successful browser launch
			driver = null;
			logger.error("Error quitting driver");
			e.printStackTrace();
		}
	}

	private void readEnvironmentConfigJsonFile(String envName, String roleName) {
	    try {
	        String sPath = new java.io.File(".").getCanonicalPath();
	        File jsonFile = new File(sPath + File.separator + "src" + File.separator + "main" + File.separator
	                + "resources" + File.separator + "environmentConfig.json");
	        environmentName = envName;
	        appUrl = JsonPath.read(jsonFile, "$.environments." + envName + ".appUrl");
	        apiUrl = JsonPath.read(jsonFile, "$.environments." + envName + ".apiUrl");
	        grantType = JsonPath.read(jsonFile, "$.environments." + envName + ".grantType");
	        SFAPICLIENTID = JsonPath.read(jsonFile, "$.environments." + envName + ".clientId");
	        SFAPICLIENTSECRET = JsonPath.read(jsonFile, "$.environments." + envName + ".clientSecret");

	        // pick role-based creds
	        SFAPIUSERNAME = JsonPath.read(jsonFile, "$.environments." + envName 
	                + ".roles[?(@.role=='" + roleName + "')].username").toString().replace("[", "").replace("]", "");
	        SFAPIPASSWORD = JsonPath.read(jsonFile, "$.environments." + envName 
	                + ".roles[?(@.role=='" + roleName + "')].password").toString().replace("[", "").replace("]", "");

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}

	

	protected void writeDynamicJsonFile(String path, String value) {
		try {// As we are using the dynamic json file as a local data store, we can write
				// data to it using this method

			String sPath = new java.io.File(".").getCanonicalPath();
			Log.info("Path: " + sPath);
			File jsonFile = new File(sPath + File.separator + "src" + File.separator + "main" + File.separator
					+ "resources" + File.separator + "dynamicdata.json");

			Log.info("Writing URL variables to json file");

			DocumentContext doc = JsonPath.parse(jsonFile).

					set(path, value);

			JsonObject jsonObj = new GsonBuilder().create().toJsonTree(doc.json()).getAsJsonObject();
			FileWriter file = new FileWriter(jsonFile);
			String a = jsonObj.toString();
			file.write(a);
			file.flush();
			file.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String readDynamicJsonFile(String path) {
		try { // As we are using the dynamic json file as a local data store, we can read data
				// from it using this method

			String sPath = new java.io.File(".").getCanonicalPath();
			Log.info("Path: " + sPath);
			File jsonFile = new File(sPath + File.separator + "src" + File.separator + "main" + File.separator
					+ "resources" + File.separator + "dynamicdata.json");

			Log.info("Reading variables from json file");
			return (String) JsonPath.read(jsonFile, path);

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public File captureScreenShot() {
		return new PageBase(driver).takeScreenshot();
	}

	@Override
	public Properties getStaticData() { // Method to read data from static data properties file
		if (staticData == null) {
			staticData = new Properties();
			InputStream input = null;

			try {
				String filename = "staticdata.properties";
				input = BaseTest.class.getClassLoader().getResourceAsStream(filename);
				if (input != null) {
					// load a properties file from class path, inside static
					// method
					staticData.load(input);
				}
			} catch (IOException ex) {
				TestNGCustomReporter.logbr("error loading staticdata.properties" + ex.getMessage());
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						TestNGCustomReporter.logbr(("error loading staticdata.properties") + e.getMessage());
					}
				}
			}
		}
		return staticData;
	}

	// Stub methods below for reference
	@Override
	public String excelValueReader(int row, int column) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void excelValueWriter(int row, int column, String value) {
		// TODO Auto-generated method stub

	}

	public String getURL(String appname) { // Method to get SF Apps URL and simulate 9 dot navigation
		GetSFApps getSfApps = new GetSFApps();
		return getSfApps.getAppNavURL(appname);

	}

}
