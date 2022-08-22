[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.testzeus/Test_Zeus/badge.svg?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.testzeus/Test_Zeus)

## Hello 👋
⚡Automation for Salesforce is tough (no , seriously). And every platform release brings the lightning and thunder for UI Automation tests (Winter21 caused even our tests to shiver). So we thought, what if we could find a solution to this madness and create a simple (but robust) framework for Salesforce automation tests.
And solve the problem of flaky tests, while accelerating the development of automation tests. 

Presenting (drum roll 🥁) TestZeus -> An open source automation framework built specifically for Salesforce. 
So what ? Well, this framework has boiler plate code to get you started with test automation for your Salesforce org. 
And just to name a few features : 
* a.Advance utilities like Autolocators 🧙‍♂️ (https://github.com/TestZeus/TestZeus/blob/main/README.md#autolocator-strategy-)
* b.API integrations for REST APIs 🔌
* c.Intelligent Waiting mechanism for Salesforce pages ⌚
* d.Contextual actions for Salesforce UI elements🏹
* e.Basic integrations like Email , Selenium, TESTNG, MAVEN and page objects.✔

The core of the framework works on top of Salesforce's UI API to achieve UI automation.  Dont know what is UI API? 
No worries at all , here is a resource to get you going : https://developer.salesforce.com/docs/atlas.en-us.uiapi.meta/uiapi/ui_api_get_started.htm
 
 And here is a nice diagram from our friends at Salesforce: 
 
![uiAPiimage](https://user-images.githubusercontent.com/7482112/152789742-b0bedc53-7d6e-4565-872e-77150766b43f.png)


## AutoLocator Strategy 🤖:   
This is where the magic happens. We parse the UI API and process the jsonresponse for labels, datatypes and sections to create the actions and locators for the UI elements on the fly. 
Ask me how? See this now : 

![AutoLocators drawio](https://user-images.githubusercontent.com/7482112/180414381-39b54280-1ea2-47d7-b4c2-af7ce65fb80c.png)



## High Level Framework diagram 🏛:
Here is a high level diagram for the framework. The tests can be run from maven or TestNG. 

![TestZeusArchitectureSimple drawio (1)](https://user-images.githubusercontent.com/7482112/180376812-63564207-f255-4143-9b40-e59966c208cb.png)


**Note** - Kindly open the above images in new tab and you can see the full image displayed correctly. 

## Podcast around TestZeus : 
Here's a neat podcast around test automation for Salesforce using TestZeus : https://youtu.be/iQk0cZuR-ko


## Pre-requisites 🔗:
  * Technical requirements : JAVA, Maven, TestNG, ChromeDriver on the local. 
 * And Non-technical requirements : A beverage of your choice (coffee/tea) and some good music to automate the toughest of test cases. 
  
## Instructions for usage 💽:

There are 2 ways of using TestZeus: 

### A. Using the TestZeus framework as a starting point
  
  1. Clone this repository
  2. Import into your favourite IDE as a "Maven" project
  3. As this repository already contains Maven dependencies for TestZeus, Selenium, TestNG, Emails, APIs etc; therefore you need not add these separately
  
 #### Start Creating UI Test cases ✒:
 As easy as 1-2-3:
 1. Add the Page object class for which the flow has to be modeled
 2. The class variable for the same needs to be added to the BaseTest class for instantiation
 3. Create the actual test class with references to the web elements and corresponding actions from the page object class 

Each test class is extended from BaseTest, thereby inheriting the wrapped methods for @BeforeClass and @BeforeSuite.

BaseTest class also triggers the below 2 things:

 - Page Object Model: The page objects are instantiated at run time via [Reflection]([https://www.oracle.com/technical-resources/articles/java/javareflection.html](https://www.oracle.com/technical-resources/articles/java/javareflection.html))
 - Data setup and post execution clean up of webdriver

Wrapper methods for abstracting the webdriver internals are written in the **PageBase** class.
Methods to interact with UI API and create locators/interactions are setup in the **SFPageBase** class.

 **Note.1** : There are sample tests included in the location ```src/test/java/com/AT/testscripts/``` to help you get started.
  **Note.2** : The demo test cases under ```src/test/java/com/AT/testscripts/``` require credentials from config.json file for authentication and authorization, so dont forget to put in the credentials before trying to run the tests .
 
 #### To run these tests 🥈 :
   - Option 1 - Both the UI and API test can be run as standalone TESTNG tests
    OR
   - Option 2- Perform a maven build with ```mvn clean install test``` goals on the pom.xml
 
#### Debug tests 🐜:
  Detailed option: Run as -Dtest=SmokeTest -Dmaven.surefire.debug test
  And then: 
  Debug config->set up 5005 port and then continue debugging

Quick Option: As always, adding break points and debug as TestNG test
     
### B. Using Testzeus as a dependency in your existing test automation framework
  1. Add the Testzeus maven dependency in your pom.xml file. 
 Latest build here: https://search.maven.org/artifact/com.testzeus/Test_Zeus

 
  2. TestZeus will need the below to perform the requisite automated operations :
  * A webdriver instance ```WebDriver driver = new ChromeDriver();``` to perform the UI interactions (clicks, smart waits, selects) from the SFPageBase class
  * User and API credentials to perform CRUD operations and fetch UI API details for building the web elements on the fly. 
 
 **Sample method call and Test Class :**
 ```
 public class AccountCreationViaUI {

	@Test(priority = 1)
	public void createAccount() throws Exception {
		// Credentials for using the Connected app and accessing data via REST API
		final String SFAPIUSERNAME_UAT = "gmail@rajnikanth.com";

		final String SFAPITOKEN_UAT = "yourAPItoken";

		final String SFAPIPASSWORDSTRING_UAT = "passwordstring";

		// password needs to be appended with token as per : //
		// https://stackoverflow.com/questions/38334027/salesforce-oauth-authentication-bad-request-error

		final String SFAPIPASSWORD_UAT = SFAPIPASSWORDSTRING_UAT + SFAPITOKEN_UAT;

		final String SFAPILOGINURL_UAT = "https://testzeus.my.salesforce.com";

		final String SFAPIGRANTSERVICE = "/services/oauth2/token?grant_type=password";
		// Client id is the consumerkey for the connected app
		final String SFAPICLIENTID_UAT = "clientID";

		// Client secret is the consumer secret protected static final String
		final String SFAPICLIENTSECRET_UAT = "clientsecret";

		// Setting up Login for SF API requests
		HTTPClientWrapper.SFLogin_API(SFAPILOGINURL_UAT, SFAPIGRANTSERVICE, SFAPICLIENTID_UAT, SFAPICLIENTSECRET_UAT,
				SFAPIUSERNAME_UAT, SFAPIPASSWORD_UAT);
		
		//Sample usage of BoniGarcia's webdriver manager
		WebDriverManager.chromedriver().setup();

		ChromeDriver driver = new ChromeDriver();
		
		//Create a new instance of the SFPageBase class
		SFPageBase pb = new SFPageBase(driver);

		// Use methods from TestZeus as below for Navigation to login page
		pb.openHomepage("https://testzeus2-dev-ed.my.salesforce.com");
		pb.maximize();

		// Or Use the webdriver implementations: Example for Submitting user id,
		// password and logging in
		driver.findElement(By.id("username")).sendKeys("UIusername@gmail.com");
		driver.findElement(By.id("password")).sendKeys("UIpassword");
		WebElement loginbutton = driver.findElement(By.id("Login"));

		pb.safeClick(loginbutton);
		pb.appLauncher("Account");

		WebElement newbutton = driver.findElement(By.xpath("//a[@title='New']"));

		pb.safeClick(newbutton);

		// We fetch all the labels and datatype from UI API here for a certain record
		String recordid = "0015g00000S9lfUAAR";
		pb.uiApiParser(recordid);
		// Form data can be passed directly on the new sObject creation screen
		pb.formValueFiller("Account Name", "AccountCreatedOn : " + pb.getCurrentDateTimeStamp());
		WebElement savebutton = driver.findElement(By.xpath("//button[@name='SaveEdit']"));

		pb.safeClick(savebutton);
		HTTPClientWrapper.SFLogout_API();
		// Dont forget to say thanks
		System.out.println("Thank you :) ");
		driver.close();
		driver.quit();
		// Setting driver to null for stopping persistent use of driver
		// session across browsers
		driver = null;

	}
}
 ```


## Video Demo
Under 5 minute video to show you the highlights of the framework and a demo of the execution :


https://user-images.githubusercontent.com/7482112/152846537-db5ee79d-ce29-436f-b57f-2c4e9405b275.mp4

## Selenium Conference 2022 :
* Link to the [video recording](https://youtu.be/KLN4bHND0nM)
* Link to the [slides](https://docs.google.com/presentation/d/1tBkivZRPb17KTSYw1FBYoqMb_muqqNVerdRCk6cZRF4/edit?usp=sharing)


## Why is the name TestZeus?
Zeus is the God of lightning and thunder and we want this framework to be the same with Lightning platform. 
(plus we got the domain name for a cheap price 🤗)

## Summary 🙏
- Stop writing flaky locators and cryptic xpaths for Webelements on the Salesforce UI. We  source the locator values for your scripts from the Salesforce UI API , so that you can focus more on building tests and less on maintenance
- The world of testing for Salesforce can be fast-paced and scary. That's why we are together in this mission and need your help to spread the word, try the framework or contribute to our codebase. And we have a long road ahead of us, so lets join forces to automate Salesforce

## Support ☎
You can find a happy and helping community of Test Automation/QA folks at the below link: 

[Test Automation Trailblazers](https://trailhead.salesforce.com/trailblazer-community/groups/0F93A000000DQPd?tab=discussion&sort=LAST_MODIFIED_DATE_DESC)


And if you would like to technically contribute/raise an issue, then feel free to open a ticket on this Github Repo.

Kindly note , that we have observed a few users mentioning about an issue with project imports and getting errors related to Testzeus base file missing from the project. This is an IDE related issue and can be easily resolved with the solution mentioned [here](https://github.com/TestZeus/TestZeus/commit/d35f1ec5c52594aab23c6e765ddb01a89a01e565#commitcomment-78946563)

Note : We are in no way directly or indirectly associated with Salesforce (yet). 

Made with ♥ in India.



![testzeusanimated](https://user-images.githubusercontent.com/7482112/152791284-53556ac2-ccd4-419d-a8c7-1e8036aafea8.gif)


<a href="https://www.buymeacoffee.com/robin" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy Me A Coffee" height="20" width="87"></a>
