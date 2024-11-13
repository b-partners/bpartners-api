package app.bpartners.api.unit.repository;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.repository.InvoiceRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InvoiceRepositoryTest extends MockedThirdParties {
  @Autowired private InvoiceRepository subject;

  @Test
  void should_return_invoices_in_date_range_and_pagination_when_valid_dates_and_page_given() {
    int page = 0;
    int pageSize = 3;
    var startDate = LocalDate.of(2022, 9, 1);
    var endDate = LocalDate.of(2022, 10, 12);

    List<Invoice> result =
        subject.findAllByIdUserAndSendingDateBetweenAndPaginate(
            JOE_DOE_ID, startDate, endDate, page, pageSize);

    assertEquals(pageSize, result.size());
    for (int i = 1; i < result.size(); i++) {
      assertTrue(
          result.get(i - 1).getSendingDate().isBefore(result.get(i).getSendingDate())
              || result.get(i - 1).getSendingDate().isEqual(result.get(i).getSendingDate()));
    }

    assertTrue(result.get(0).getSendingDate().isAfter(startDate.minusDays(1)));
    assertTrue(result.get(0).getSendingDate().isBefore(endDate.plusDays(1)));
    assertTrue(result.get(1).getSendingDate().isAfter(startDate.minusDays(1)));
    assertTrue(result.get(1).getSendingDate().isBefore(endDate.plusDays(1)));
  }

  @Test
  void should_return_empty_list_when_no_invoices_found_in_date_range() {
    LocalDate nonMatchingStartDate = LocalDate.of(2025, 1, 1);
    LocalDate nonMatchingEndDate = LocalDate.of(2025, 12, 31);

    List<Invoice> result =
        subject.findAllByIdUserAndSendingDateBetweenAndPaginate(
            JOE_DOE_ID, nonMatchingStartDate, nonMatchingEndDate, 0, 5);

    assertTrue(result.isEmpty());
  }

  @Test
  void should_return_empty_list_when_no_invoices_found_for_user() {
    var startDate = LocalDate.of(2022, 9, 1);
    var endDate = LocalDate.of(2022, 9, 10);

    List<Invoice> result =
        subject.findAllByIdUserAndSendingDateBetweenAndPaginate(
            "non_existent_user", startDate, endDate, 0, 5);

    assertTrue(result.isEmpty());
  }
}
