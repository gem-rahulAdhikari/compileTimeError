import com.aventstack.extentreports.Status;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

public class App extends driverConfig {
    @Test
    public void demo() {
        driver.get("https://www.google.com/")
        WebElement searchInput = driver.findElement(By.xpath("//textarea"))
        searchInput.sendKeys("selenium");
        searchInput.sendKeys(Keys.RETURN);
        WebElement title = driver.findElement(By.xpath("(//h3[text()='Selenium'])[1]"));
        String fetchedTitle = title.getText();
        System.out.println(fetchedTitle + " start2");
        System.out.println(fetchedTitle + " start2");
        if ("Selenium".equals(fetchedTitle)) {
            extentTest.log(Status.PASS, "text matched successfully.", captureScreenshot());
        } else {
            extentTest.log(Status.FAIL, "Failed to match text.", captureScreenshot());
        }

        extentTest.log(Status.PASS, driver.getCurrentUrl(), captureScreenshot());

    }


}
