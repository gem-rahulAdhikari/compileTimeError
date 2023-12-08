import com.aventstack.extentreports.Status;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.annotations.Test;

import java.lang.*;
import java.util.*;
import java.awt.*;
import java.math.*;
import java.time.*;

public class App extends driverConfig{
    static String reportName="Report_4a5d2e5676cf34734a533fe17b61b38dba979b836a374798302934e7b32ad777_0";

    @Test

public void demo(){
driver.get("https://www.google.com/");
     WebElement searchInput = driver.findElement(By.xpath("//textarea"));
    searchInput.sendKeys("selenium");
    searchInput.sendKeys(Keys.RETURN);
     WebElement title = driver.findElement(By.xpath("(//h3[text()='Selenium'])[1]")); 
     String fetchedTitle=title.getText();
    System.out.println(fetchedTitle+" start2");
    System.out.println(fetchedTitle+" start2");
    
   
     if("Selenium".equals(fetchedTitle))
     {
         extentTest.log(Status.PASS,"text matched successfully.",captureScreenshot());
     }
    else
     {
         extentTest.log(Status.FAIL,"Failed to match text.",captureScreenshot());
     }
 
     extentTest.log(Status.PASS,driver.getCurrentUrl(),captureScreenshot());
 
}


























}
