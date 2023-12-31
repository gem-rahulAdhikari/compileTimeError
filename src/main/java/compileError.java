import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import io.restassured.RestAssured;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Properties;

public final class compileError {

    String bucketName="selenium-reportdata";
    String executionName;
    @Test
    public void upload() throws IOException {
        System.out.println("inside upload");
        String directory="./seleniumExecution/test-output";
        String fileExtension=".html";
        Path dir= Paths.get(directory);
        String fileFound=findFileInDirectory(dir,fileExtension);
        System.out.println(fileFound);
        if (fileFound ==null) {
            String token = token();
            Properties reportNameReader = new Properties();
            reportNameReader.load(new FileInputStream("./seleniumExecution/reportName.properties"));
            executionName=reportNameReader.getProperty("reportName");
            String filePath = "./seleniumExecution/test-output/" + executionName+".txt";
            File file = new File(filePath);
        RestAssured.baseURI = "https://storage.googleapis.com/upload/storage/v1/b/"+bucketName+"/o";
        RestAssured.given().header("Authorization", "Bearer " + token).queryParam("uploadType","media").queryParam("name",executionName+".txt").contentType("text/html").body(file).post().then().statusCode(200);
        String reportName="https://storage.googleapis.com/"+bucketName+"/" + executionName + ".txt";
            // mongoTransfer(reportName);
        }
    }
    public String token() throws IOException {
        String serviceAccountKeyPath = "./g-code-editor-417ccbad5803.json";
       GoogleCredentials credentials = ServiceAccountCredentials.fromStream(new FileInputStream(serviceAccountKeyPath))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
        AccessToken accessToken = credentials.refreshAccessToken();
        String token = accessToken.getTokenValue();
        System.out.println("Access Token: " + token);
        return token;
    }
    public static String findFileInDirectory(Path directory, String fileExtension) throws IOException {
        final String[] foundFile = {null};

        Files.walkFileTree(directory, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new java.nio.file.SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(fileExtension)) {
                    foundFile[0] = file.toString();
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return foundFile[0];
    }
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
 public void mongoTransfer(String reportName) throws IOException {
        //uploading bucket report link and user-updated code to db
        System.out.println("in mongo upload function");
        String userId = executionName.split("_")[1];
        // String url = "http://g-codeeditor.el.r.appspot.com/editor?name=" + userId;
     // String url = " http://127.0.0.1:5000/editor?name=" + userId;
        String url=userId;
        String filePath = "./seleniumExecution/src/main/java/App.java";
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
            System.out.println(getStatusCode);
            if (getStatusCode == HttpURLConnection.HTTP_OK) {
                System.out.println("inside get request");
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
