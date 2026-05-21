package app.bpartners.api.service;

import static app.bpartners.api.service.utils.WorkbookComparator.isEqual;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.service.customer.CustomerExportPayload;
import app.bpartners.api.service.file.CustomerExportFunction;
import java.util.List;
import lombok.SneakyThrows;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class CustomerExportFunctionTest {

  CustomerExportFunction subject = new CustomerExportFunction();

  @SneakyThrows
  @Test
  void export_customer_to_xls() {
    try (XSSFWorkbook excpectedWorkbook =
        new XSSFWorkbook(
            new ClassPathResource("/files/expected_customers_export.xlsx").getFile())) {
      var johnDoe =
          new CustomerExportPayload("John Doe", "john@example.com", "john-uuid-gen-v4", true);
      var janeDoe =
          new CustomerExportPayload("Jane Doe", "jane@example.com", "jane-uuid-gen-v4", false);

      var file = subject.apply(List.of(johnDoe, janeDoe));

      try (var excelWorkbook = new XSSFWorkbook(file)) {

        assertTrue(isEqual(excelWorkbook, excpectedWorkbook));
      }
    }
  }
}
