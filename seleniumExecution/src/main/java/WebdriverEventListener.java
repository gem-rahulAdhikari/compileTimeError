import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.model.Media;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.events.WebDriverListener;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Collection;

public class WebdriverEventListener implements WebDriverListener {
    public static ExtentTest extentTest;
    public static Boolean isSeleniumException = false;
    ExtentReports extentReports;
    private String tag, value, getText, placeHolder, id, ariaLabel;

    public Media captureScreenshot() {
        //creating screenshots for reporting with
        TakesScreenshot screenshotDriver = (TakesScreenshot) driverConfig.driver;
        byte[] screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);
        return MediaEntityBuilder.createScreenCaptureFromBase64String(Base64.getEncoder().encodeToString(screenshotBytes)).build();
    }

    //functions for overriding default methods for adding default selenium commands to report
    @Override
    public void afterGet(WebDriver driver, String url) {
        extentTest.log(Status.PASS, "Launched Url Successfully : " + url, captureScreenshot());
    }

    @Override
    public void afterAnyNavigationCall(WebDriver.Navigation navigation, Method method, Object[] args, Object result) {
        extentTest.log(Status.PASS, "Browser Navigation Call : " + method.getName(), captureScreenshot());
    }

    @Override
    public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
        String description = "Successfully: Entered Text " + keysToSend[0] + " In TextBox";
        extentTest.log(Status.PASS, description, captureScreenshot());
    }

    @Override
    public void afterClear(WebElement element) {
        String description = "Text Cleared ";
        String elementGetText = element.getAttribute("placeholder");
        if (elementGetText != null && !elementGetText.isEmpty()) {
            description = "Successfully: Cleared Text Of  " + elementGetText + " TextBox ";
        }
        extentTest.log(Status.PASS, description, captureScreenshot());
    }

    @Override
    public void beforeClick(WebElement element) {
        value = "";
        getText = "";
        tag = "";
        placeHolder = "";
        id = "";
        ariaLabel = "";

        try {

            if (element.getText() != null && !element.getText().isEmpty()) {
                getText += element.getText();
            }
            if (element.getTagName() != null && !element.getTagName().isEmpty()) {
                tag += element.getTagName();
            }
            if (element.getAttribute("value") != null && !element.getAttribute("value").isEmpty()) {
                value = element.getAttribute("value");
            }
            if (element.getAttribute("placeholder") != null && !element.getAttribute("placeholder").isEmpty()) {
                placeHolder = element.getAttribute("placeholder");
            }
            if (element.getAttribute("id") != null && !element.getAttribute("id").isEmpty()) {
                id = element.getAttribute("id");
            }
            if (element.getAttribute("aria-label") != null && !element.getAttribute("aria-label").isEmpty()) {
                id = element.getAttribute("aria-label");
            }

        } catch (Exception e) {

            String log = e.getMessage().split("Session info")[0];
            log = log.substring(0, log.length() - 1);
            extentTest.log(Status.FAIL, "Error in button", captureScreenshot());
        }
    }

    @Override
    public void afterClick(WebElement element) {

        String description = "Clicked On Element Successfully";

        if (!placeHolder.isEmpty()) {
            description = "Successfully: Clicked on " + placeHolder;
        } else if (!getText.isEmpty()) {
            description = "Successfully: Clicked on " + getText;
        } else if (!value.isEmpty()) {
            description = "Successfully: Clicked on " + value;
        } else if (!id.isEmpty()) {
            description = "Successfully: Clicked on " + id;
        } else if (!ariaLabel.isEmpty()) {
            description = "Successfully: Clicked on " + tag;
        } else if (!tag.isEmpty()) {
            description = "Successfully: Clicked on " + tag;
        }
        extentTest.log(Status.PASS, description, captureScreenshot());
    }

    @Override
    public void afterPerform(WebDriver driver, Collection<Sequence> actions) {
        String mouseAction = "";
        int size;
        try {
            Sequence sequence = ((Sequence) actions.toArray()[0]);
            Class<?> sequenceRefAPIClass = sequence.getClass();
            Field priVarActions = sequenceRefAPIClass.getDeclaredField("actions");
            priVarActions.setAccessible(true);
            Method priMethodSize = sequenceRefAPIClass.getDeclaredMethod("size");
            priMethodSize.setAccessible(true);
            size = (int) priMethodSize.invoke(sequence);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        if (size == 1)
            mouseAction = "Hover Over/Move To Element";
        else if (size == 5)
            mouseAction = "Double Click";
        else if (size == 3)
            mouseAction = "Right Click";
        extentTest.log(Status.PASS, "Successfully: Performed Action " + mouseAction, captureScreenshot());
    }

    @Override
    public void afterExecuteScript(WebDriver driver, String script, Object[] args, Object result) {
        extentTest.log(Status.PASS, "JavaScriptExecuter : " + script, captureScreenshot());
    }

    @Override
    public void afterAnyAlertCall(Alert alert, Method method, Object[] args, Object result) {
        extentTest.log(Status.PASS, "Alert Operation : " + method.getName().toUpperCase(), captureScreenshot());
    }

    @Override
    public void afterAnyWindowCall(WebDriver.Window window, Method method, Object[] args, Object result) {
        extentTest.log(Status.PASS, "Window Operation : " + method.getName().toUpperCase(), captureScreenshot());
    }

    @Override
    public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
        isSeleniumException = true;
        String log = e.getTargetException().getMessage().split("Session info")[0];
        log = log.substring(0, log.length() - 1);
        extentTest.log(Status.FAIL, "Error in : " + args[0] + "<br></br>" + log, captureScreenshot());
    }

    @Override
    public void afterClose(WebDriver driver) {
        extentTest.log(Status.PASS, "Close Driver");
    }

    @Override
    public void afterQuit(WebDriver driver) {
        extentTest.log(Status.PASS, "Quit Driver");
    }

}
