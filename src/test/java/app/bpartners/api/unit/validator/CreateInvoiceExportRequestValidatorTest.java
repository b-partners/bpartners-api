package app.bpartners.api.unit.validator;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceExportOutputFormat.ZIP;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceExportRequest;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceExportRequestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateInvoiceExportRequestValidatorTest {
  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 1, 31);

  CreateInvoiceExportRequestValidator subject = new CreateInvoiceExportRequestValidator();

  @Test
  void valid_request_ok() {
    assertDoesNotThrow(() -> subject.accept(validRequest()));
  }

  @Test
  void optional_export_options_are_not_mandatory_ok() {
    assertDoesNotThrow(
        () -> subject.accept(validRequest().id(null).outputFormat(null).batchSize(null)));
  }

  @Test
  void single_day_interval_ok() {
    assertDoesNotThrow(() -> subject.accept(validRequest().from(FROM).to(FROM)));
  }

  @Test
  void archived_invoices_export_ok() {
    assertDoesNotThrow(() -> subject.accept(validRequest().archiveStatus(DISABLED)));
  }

  @Test
  void missing_from_ko() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(validRequest().from(null)));

    assertEquals("CreateInvoiceExportRequest.from is mandatory. ", actual.getMessage());
  }

  @Test
  void missing_to_ko() {
    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(validRequest().to(null)));

    assertEquals("CreateInvoiceExportRequest.to is mandatory. ", actual.getMessage());
  }

  @Test
  void from_after_to_ko() {
    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.accept(validRequest().from(TO).to(FROM)));

    assertEquals(
        "CreateInvoiceExportRequest.from can not be after CreateInvoiceExportRequest.to.",
        actual.getMessage());
  }

  @Test
  void missing_archive_status_ko() {
    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.accept(validRequest().archiveStatus(null)));

    assertEquals("CreateInvoiceExportRequest.archiveStatus is mandatory. ", actual.getMessage());
  }

  @Test
  void missing_status_list_ko() {
    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.accept(validRequest().statusList(null)));

    assertEquals(
        "CreateInvoiceExportRequest.statusList is mandatory and can not be empty. ",
        actual.getMessage());
  }

  @Test
  void empty_status_list_ko() {
    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.accept(validRequest().statusList(List.of())));

    assertEquals(
        "CreateInvoiceExportRequest.statusList is mandatory and can not be empty. ",
        actual.getMessage());
  }

  @Test
  void empty_request_reports_every_missing_field_at_once_ko() {
    var actual =
        assertThrows(
            BadRequestException.class, () -> subject.accept(new CreateInvoiceExportRequest()));

    // no date is provided, so the from-after-to check has nothing to report
    assertEquals(
        "CreateInvoiceExportRequest.from is mandatory. "
            + "CreateInvoiceExportRequest.to is mandatory. "
            + "CreateInvoiceExportRequest.archiveStatus is mandatory. "
            + "CreateInvoiceExportRequest.statusList is mandatory and can not be empty. ",
        actual.getMessage());
  }

  @Test
  void from_after_to_and_missing_archive_status_are_both_reported_ko() {
    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.accept(validRequest().from(TO).to(FROM).archiveStatus(null)));

    assertEquals(
        "CreateInvoiceExportRequest.from can not be after CreateInvoiceExportRequest.to."
            + "CreateInvoiceExportRequest.archiveStatus is mandatory. ",
        actual.getMessage());
  }

  private CreateInvoiceExportRequest validRequest() {
    return new CreateInvoiceExportRequest()
        .id("invoice_export_request_id")
        .from(FROM)
        .to(TO)
        .archiveStatus(ENABLED)
        .statusList(List.of(CONFIRMED, PAID))
        .outputFormat(ZIP)
        .batchSize(500);
  }
}
