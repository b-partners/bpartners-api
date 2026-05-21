package app.bpartners.api.service.file;

import app.bpartners.api.service.customer.CustomerExportPayload;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

@Component
public class CustomerExportFunction implements Function<List<CustomerExportPayload>, File> {

  private static final String[] HEADERS = {"Name", "Email", "Stripe Customer ID", "Unknown"};

  @Override
  public File apply(List<CustomerExportPayload> payloads) {
    try {
      File tempFile = File.createTempFile("customers_export_", ".xlsx");

      try (XSSFWorkbook workbook = new XSSFWorkbook();
          FileOutputStream fos = new FileOutputStream(tempFile)) {

        XSSFSheet sheet = workbook.createSheet("Customers");

        XSSFCellStyle headerStyle = workbook.createCellStyle();
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
          Cell cell = headerRow.createCell(i);
          cell.setCellValue(HEADERS[i]);
          cell.setCellStyle(headerStyle);
        }

        int rowIndex = 1;
        for (CustomerExportPayload payload : payloads) {
          Row row = sheet.createRow(rowIndex++);
          row.createCell(0).setCellValue(payload.name());
          row.createCell(1).setCellValue(payload.email());
          row.createCell(2).setCellValue(payload.stripeCustomerId());
          row.createCell(3).setCellValue(payload.unknown());
        }

        for (int i = 0; i < HEADERS.length; i++) {
          sheet.autoSizeColumn(i);
        }

        workbook.write(fos);
      }

      return tempFile;

    } catch (IOException e) {
      throw new RuntimeException("Unable to export xlsx file", e);
    }
  }
}
