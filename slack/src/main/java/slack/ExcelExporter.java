package slack;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {

    public static byte[] exportUsersToExcel(List<UserProfile> userProfiles) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Slack Users");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Username", "Email"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            sheet.createFreezePane(0, 1);
            // Create data rows
            int rowNum = 1;
            for (UserProfile user : userProfiles) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(user.getUsername());
                row.createCell(1).setCellValue(user.getEmail());
            }

            for (int i = 0; i < headers.length; i++) { 
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
}