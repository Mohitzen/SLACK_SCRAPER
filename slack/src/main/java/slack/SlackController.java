package slack;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
public class SlackController {

    @Autowired
    private SlackScraperService slackScraperService;

    // Handles the GET request to the root URL, showing the input form
    @GetMapping("/")
    public String showForm() {
        return "index"; // Renders src/main/resources/templates/index.html
    }

    // Handles the POST request when the form is submitted
    @PostMapping("/scrape")
    public Object scrapeAndDownload( // Changed return type to Object
            @RequestParam("emailId") String emailId,
            RedirectAttributes redirectAttributes) { // Removed Model as it's not used directly for redirect

        if (emailId == null || emailId.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email ID cannot be empty. Please provide a valid email.");
            // Redirect back to the form page
            return "redirect:/"; // <-- IMPORTANT CHANGE: Redirect to the home page
        }

        try {
            System.out.println("Received request to scrape for email: " + emailId);
            List<UserProfile> userProfiles = slackScraperService.scrapeSlackProfiles(emailId);
            System.out.println("Scraping completed. Found " + userProfiles.size() + " profiles.");

            if (userProfiles.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No profiles found for the provided email ID. Please check the email and try again.");
                return "redirect:/"; // <-- Redirect if no profiles found
            }

            byte[] excelBytes = ExcelExporter.exportUsersToExcel(userProfiles);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=slack_profiles.xlsx");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE); // or MediaType.APPLICATION_EXCEL_VALUE
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            System.out.println("Returning Excel file.");
            return ResponseEntity.ok() // <-- Return ResponseEntity for successful download
                    .headers(headers)
                    .contentLength(excelBytes.length)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new ByteArrayResource(excelBytes));

        } catch (Exception e) {
            System.err.println("Error during scraping or Excel generation: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "An error occurred during scraping: " + e.getMessage() + ". Please check the server logs.");
            return "redirect:/"; // <-- Redirect on any other error
        }
    }
}