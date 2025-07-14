package slack;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service // Marks this as a Spring service component
public class SlackScraperService {

    public List<UserProfile> scrapeSlackProfiles(String emailId) throws InterruptedException, IOException {
        WebDriver driver = null;
        List<UserProfile> userProfiles = new ArrayList<>();
        String tempDir = "";

        try {
            WebDriverManager.chromedriver().setup();
            ChromeOptions co = new ChromeOptions();
            co.addArguments("--remote-allow-origins=*");
            co.addArguments("--incognito");
            co.addArguments("--disable-blink-features=AutomationControlled");
            co.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
            co.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
           
            tempDir = System.getProperty("user.home") + File.separator + "seleniumTempUserDir" + File.separator + "Profile_" + System.currentTimeMillis();
            Path path = Paths.get(tempDir);
            co.addArguments("--user-data-dir=" + path.toAbsolutePath());
            System.out.println("Starting Chrome driver for scraping...");

            driver = new ChromeDriver(co);
            driver.manage().window().maximize();
            driver.get("https://slack.com/signin#/signin");

            // Input email and click submit
            driver.findElement(By.id("signup_email")).sendKeys(emailId);
            driver.findElement(By.id("submit_btn")).click();

            // Handle ReCAPTCHA (THIS IS A MAJOR CHALLENGE FOR AUTOMATION)
            // You are relying on a manual click here. For truly automated systems,
            // you'd need a CAPTCHA solving service or a different login strategy.
            System.out.println("Waiting for ReCAPTCHA to be present (manual intervention might be needed for CAPTCHA if it appears)...");
            WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(20)); // Increased wait time for potential manual CAPTCHA
            try {
                wait1.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.xpath("//iframe[contains(@title, 'reCAPTCHA')]")));
                WebElement recaptchaCheckbox = wait1.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='recaptcha-anchor']/div")));
                recaptchaCheckbox.click();
                System.out.println("ReCAPTCHA checkbox clicked. Waiting for verification...");
                driver.switchTo().defaultContent(); // Switch back after clicking checkbox
                // Wait for the reCAPTCHA to disappear or for the next element to become clickable
                WebDriverWait waitAfterRecaptcha = new WebDriverWait(driver, Duration.ofSeconds(60)); // Wait for CAPTCHA to resolve
                waitAfterRecaptcha.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//iframe[contains(@title, 'reCAPTCHA')]")));
                System.out.println("ReCAPTCHA frame disappeared.");

            } catch (Exception e) {
                System.out.println("ReCAPTCHA frame not found or not interactable within timeout. Proceeding...");
                // This might happen if CAPTCHA isn't triggered or if it's auto-solved.
                // Or if it presents a challenge you can't click automatically.
            }

            // Click the final submit button after reCAPTCHA (if present)
            WebDriverWait wait2 = new WebDriverWait(driver, Duration.ofSeconds(30)); // Adjusted wait
            WebElement finalSubmitBtn = wait2.until(ExpectedConditions.elementToBeClickable(By.id("submit_btn")));
            finalSubmitBtn.click();
            System.out.println("Final submit button clicked.");

            // Wait for the email verification link or direct login
            WebDriverWait wait3 = new WebDriverWait(driver, Duration.ofSeconds(80)); // Long wait for Slack to send email/redirect
            WebElement emailVerificationLink = wait3.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"page_contents\"]/div/div/div[2]/p/a")));
            emailVerificationLink.click();
            System.out.println("Email verification link clicked.");


            WebDriverWait wait4 = new WebDriverWait(driver, Duration.ofSeconds(25));
            WebElement membersButton = wait4.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='p-autoclog__hook']//button[@class='c-button-unstyled p-avatar_stack--details']")));
            membersButton.click();
            System.out.println("Clicked on members button.");

            WebDriverWait wait5 = new WebDriverWait(driver, Duration.ofSeconds(10));
            WebElement parentElement = wait5.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("body > div.c-sk-modal_portal > div > div > div.p-about_modal__tabs > div.p-about_modal__tab_panel.c-tabs__tab_panel.c-tabs__tab_panel--active > div > div.p-ia_details_popover__members_list > div:nth-child(1) > div > div")));
            List<WebElement> allDescendantElements = parentElement.findElements(By.tagName("strong"));

            int numberOfProfiles = allDescendantElements.size();
            System.out.println("Found " + numberOfProfiles + " profiles.");

            // Close the initial modal (if it pops up directly after membersButton click and covers the members list)
            // This might be the "Close" button for the general "About" modal
            try {
                WebElement closeInitialModal = driver.findElement(By.xpath("/html/body/div[12]/div/div/button"));
                closeInitialModal.click();
                System.out.println("Initial modal closed.");
            } catch (Exception e) {
                System.out.println("No initial modal to close or element not found.");
            }


            for (int i = 0; i < numberOfProfiles; i++) {
                WebDriverWait profileWait = new WebDriverWait(driver, Duration.ofSeconds(25));

                // Re-click the "members" button to reopen the list if it closed
                WebElement initialClickButton = profileWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@class='p-autoclog__hook']//button[@class='c-button-unstyled p-avatar_stack--details']")));
                initialClickButton.click();
                System.out.println("Re-opened members list.");

                // Re-locate the parent element and strong elements within the *newly opened* modal
                WebElement currentParentElement = profileWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("body > div.c-sk-modal_portal > div > div > div.p-about_modal__tabs > div.p-about_modal__tab_panel.c-tabs__tab_panel.c-tabs__tab_panel--active > div > div.p-ia_details_popover__members_list > div:nth-child(1) > div > div")));
                List<WebElement> currentStrongElements = currentParentElement.findElements(By.tagName("strong"));

                // Ensure we click the correct profile from the refreshed list
                if (i < currentStrongElements.size()) {
                    WebElement currentProfileToClick = currentStrongElements.get(i);
                    currentProfileToClick.click();
                    System.out.println("Clicked profile " + (i + 1));

                    profileWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.p-r_member_profile__name__text")));
                    
                  
                    WebElement usernameElement = profileWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span.p-r_member_profile__name__text")));
                    String username = usernameElement.getText();
                    System.out.println("Username found: " + username);

                    // Wait specifically for the email link to be present and visible
                    // This is the element that was failing previously. Make sure it's fully rendered.
                    WebElement emailElement = profileWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(@class, 'p-rimeto_member_profile_field__value')]/a[contains(@class, 'c-link')]")));
                    String email = emailElement.getText(); // Extract the href attribute for the email link
                    System.out.println("Email found: " + email); 
                    
                    UserProfile u = new UserProfile(username, email);
                  userProfiles.add(u);
                  System.out.println("Scraped: " + u);
                    

                    // Close the profile detail pane
                  WebElement closeProfileDetailButton = profileWait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.p-flexpane_header.p-flexpane_header--no-bottom-border > div > button")));
                  closeProfileDetailButton.click();
                  System.out.println("Profile detail pane closed.");
                  Thread.sleep(1000);
                } else {
                    System.err.println("Error: Profile index " + i + " out of bounds. Expected " + numberOfProfiles + " profiles but found " + currentStrongElements.size());
                    
                }
            }

            System.out.println("Scraping complete. Total profiles: " + userProfiles.size());
            return userProfiles;

        } catch (Exception e) {
            System.err.println("An error occurred during scraping: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to be handled by the controller
        } finally {
            if (driver != null) {
                driver.quit();
                System.out.println("Chrome driver quit.");
            }
            // Clean up the temporary user data directory
            if (!tempDir.isEmpty()) {
                try {
                    // This is for demonstration. In production, be careful with recursive deletes.
                     Files.walk(Paths.get(tempDir)).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                    // System.out.println("Cleaned up temp user data directory: " + tempDir);
                } catch (Exception e) {
                    System.err.println("Failed to delete temp directory: " + tempDir + " - " + e.getMessage());
                }
            }
        }
    }
}