package app.bpartners.api.service.file;

import app.bpartners.api.service.customer.CustomerExportPayload;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerExportFunction implements Function<List<CustomerExportPayload>, File> {

  private static final String[] HEADERS = {
    "Stripe Customer ID",
    "Email",
    "Stripe Name",
    "Dashboard Customer Name (Invoice)",
    "Creation Datetime",
    "Facture générée"
  };

  @Override
  public File apply(List<CustomerExportPayload> payloads) {
    var uniqueCustomerToExports = new HashSet<>(payloads);
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
        for (CustomerExportPayload payload : uniqueCustomerToExports) {
          Row row = sheet.createRow(rowIndex++);
          row.createCell(0).setCellValue(payload.stripeCustomerId());
          row.createCell(1).setCellValue(payload.email());
          row.createCell(2).setCellValue(payload.stripeCustomerName());
          row.createCell(3).setCellValue(payload.internalCustomerName());
          row.createCell(4)
              .setCellValue(
                  String.valueOf(
                      LocalDateTime.from(
                          payload
                              .stripeCreationDatetime()
                              .atZone(ZoneOffset.UTC)
                              .withZoneSameInstant(ZoneId.of("Europe/Paris")))));
          row.createCell(5).setCellValue(!payload.unknown());
        }

        for (int i = 0; i < HEADERS.length; i++) {
          sheet.autoSizeColumn(i);
        }

        workbook.write(fos);
      }

      log.info("Customers export file created: {}", tempFile.getAbsolutePath());
      return tempFile;

    } catch (IOException e) {
      throw new RuntimeException("Unable to export xlsx file", e);
    }
  }
}
