package eu.nimble.selenium;

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class Test4_ChangeDescriptionOfProduct implements SeleniumInterface {

    @Override
    public void execute() throws Exception {
        //Launch the website
        driver.get("http://localhost:9092/#/user-mgmt/login");

        // Get email and password input elements
        WebElement email = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"email\"]")));
        WebElement password = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"password\"]")));

        email.clear();
        email.sendKeys(emailAddress);

        password.clear();
        password.sendKeys(userPassword);

        // Submit
        driver.findElement(By.xpath("/html/body/div[1]/nimble-app/nimble-login/credentials-form/form/button[1]")).click();

        // Wait until submit button is gone.In other words,wait until dashboard page is available.
        wait.until(ExpectedConditions.not(ExpectedConditions.presenceOfAllElementsLocatedBy(By.xpath("/html/body/div/nimble-app/nimble-login/credentials-form/form/button[1]"))));

        // Go to catalogue
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/nav/button"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[3]/li/div/a[2]"))).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/catalogue-view/div[2]/div/div[2]/div/div/div/div[2]/div[2]/button[1]"))).click();

        // Edit description
        WebElement description = driver.findElement(By.xpath("/html/body/div/nimble-app/product-publish/div/form/catalogue-line-view/catalogue-line-header/div/div[2]/value-view[3]/div/textarea"));
        description.clear();
        description.sendKeys("Done");

        // Save
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div/nimble-app/product-publish/div/div/button"))).click();

        // Check whether it is saved
        String desc = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("/html/body/div/nimble-app/catalogue-view/div[2]/div/div[2]/div/div/div/div[2]/div[1]/p[2]"))).getText();
        Assert.assertEquals("Done",desc);

        // Logout
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"dropdownMenuUser\"]"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"navbarNavAltMarkup\"]/ul[3]/li/div/a[3]"))).click();
    }


}
