import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import io.restassured.RestAssured;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;


public abstract class driverConfig extends WebdriverEventListener {
    public static WebDriver driver;
    static ThreadLocal<WebDriver> wDriver = new ThreadLocal<WebDriver>();
    public String bucketName = "selenium-reporting";
    String executionName;

    public static String readClassFileAsString(String filePath) throws IOException {
        //Reading user-updated code
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }

        return content.toString();
    }

    @BeforeSuite
    public void reporter() throws IOException {
        Properties reportNameReader = new Properties();
        reportNameReader.load(new FileInputStream("./reportName.properties"));
        executionName=reportNameReader.getProperty("reportName");
        //Extent report initialization
        ExtentSparkReporter htmlReporter = new ExtentSparkReporter("test-output/" + executionName + ".html");
        extentReports = new ExtentReports();
        extentReports.attachReporter(htmlReporter);
        extentTest = extentReports.createTest(getClass().getSimpleName());
        extentReports.setSystemInfo("OS Info", System.getProperty("os.name"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.specification.version"));
    }

    @BeforeMethod
    public void setWebDriver() {
        //Chromedriver setup
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("start-maximized");
        options.addArguments("disable-infobars");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        options.addArguments("--remote-allow-origins=*");
        WebDriverListener listener = new WebdriverEventListener();
        wDriver.set(new ChromeDriver(options));
        WebDriver decorated = new EventFiringDecorator(listener).decorate(wDriver.get());
        wDriver.set(decorated);
        driver = wDriver.get();
        System.out.println(driver);
    }

    @AfterMethod
    public void tearDown() {
        //terminating execution
        driver.quit();
        extentReports.flush();
    }

    @AfterSuite
    public void reportMover() throws IOException {
        //Uploading report to gcloud bucket storage
        System.out.println("Execution complete, report manipulation started");
        String serviceAccountKeyPath = "./g-code-editor-417ccbad5803.json";
        GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(serviceAccountKeyPath))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
        AccessToken accessToken = credentials.refreshAccessToken();
        String token = accessToken.getTokenValue();
        System.out.println("Access Token: " + token);
       uploadReport(token);
        String reportName = "https://storage.googleapis.com/"+bucketName+"/" + executionName + ".html";
        mongoTransfer(reportName);
    }

   public void uploadReport(String token) {
       try {
           System.out.println("in upload function");
           String filePath = "./test-output/"+executionName+".html";
           File file=new File(filePath);
           RestAssured.baseURI = "https://storage.googleapis.com/upload/storage/v1/b/"+bucketName+"/o";
           RestAssured.given().header("Authorization", "Bearer " + token).queryParam("uploadType","media").queryParam("name",executionName+".html").contentType("text/html").body(file).post().then().statusCode(200);
       } catch (Exception e) {
           throw new RuntimeException(e);
       }
   }

    public void mongoTransfer(String reportName) throws IOException {
        //uploading bucket report link and user-updated code to db
        System.out.println("in mongo upload function");
        String userId = executionName.split("_")[1];
        // String url = "http://g-codeeditor.el.r.appspot.com/editor?name=" + userId;
        // String filePath = "./src/main/java/App.java";
        String url=userId;
        String filePath = "./src/main/java/App.java";
        String classContent = readClassFileAsString(filePath);
        System.out.print("class content"+classContent);
        String escapedClassContent = classContent.replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
        try {
            URL getUrl = new URL("https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/getSeleniumOutput"); // Replace with your actual GET API URL
            HttpURLConnection getConnection = (HttpURLConnection) getUrl.openConnection();
            getConnection.setRequestMethod("GET");

            int getStatusCode = getConnection.getResponseCode();
            if (getStatusCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(getConnection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    String responseData = response.toString();
                    JSONArray dataArray = new JSONArray(responseData); // Assuming the response is a JSON array

                    String currentUrl = url; // Replace with the URL you want to compare

                    boolean foundMatch = false;
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);
                        String entryUrl = item.getString("url");

                        if (entryUrl.equals(currentUrl)) {
                            foundMatch = true;

                            String apiUrl = "https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/updateSeleniumSubmission";
                            URL url1 = new URL(apiUrl);
                            HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
                            connection.setRequestMethod("PUT");
                            connection.setRequestProperty("Content-Type", "application/json");
                            connection.setDoOutput(true);

                            String putData = "{\n" +
                                    "    \"filter\": {\n" +
                                    "        \"url\": \"" + url + "\"\n" +
                                    "    },\n" +
                                    "    \"SubmittedCode\":\"" + escapedClassContent + "\",\n" +
                                    "    \"Output\":\"" + reportName + "\"\n" +
                                    "}";

                            try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                                outputStream.writeBytes(putData);
                                outputStream.flush();
                            }
                            int statusCode = connection.getResponseCode();
                            String statusMessage = connection.getResponseMessage();
                            System.out.println("Status Code: " + statusCode);
                            System.out.println("Status Message: " + statusMessage);
                            break; // No need to continue the loop once a match is found
                        }
                    }

                    if (!foundMatch) {
                        String apiUrl = "https://us-east-1.aws.data.mongodb-api.com/app/application-0-awqqz/endpoint/addSeleniumResult";
                        URL url2 = new URL(apiUrl);
                        HttpURLConnection connection = (HttpURLConnection) url2.openConnection();
                        connection.setRequestMethod("PUT");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);
                        String putData1 = "{\n" +
                                "    \"filter\": {\n" +
                                "        \"url\": \"" + url + "\"\n" +
                                "    },\n" +
                                "    \"code\":\"" + escapedClassContent + "\",\n" +
                                "    \"output\":\"" + reportName + "\"\n" +
                                "}";


                        try (DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream())) {
                            outputStream.writeBytes(putData1);
                            outputStream.flush();
                        }
                        int statusCode = connection.getResponseCode();
                        String statusMessage = connection.getResponseMessage();
                        System.out.println("Status Code: " + statusCode);
                        System.out.println("Status Message: " + statusMessage);
                    }
                }
            } else {
                System.out.println("GET Request failed with status code " + getStatusCode);
            }

            getConnection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
