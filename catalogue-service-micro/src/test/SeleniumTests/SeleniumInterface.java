import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

public interface SeleniumInterface {
    // Existing user information
    String emailAddress = "can@gmail.com";
    String userPassword = "123456";

    // Create a new instance of the Chrome driver
    WebDriver driver = new ChromeDriver();

    WebDriverWait wait = new WebDriverWait(driver,60);

    void execute() throws Exception;
}
