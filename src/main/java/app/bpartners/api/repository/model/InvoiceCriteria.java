package app.bpartners.api.repository.model;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.Builder;

@Builder(toBuilder = true)
public record InvoiceCriteria(
    String idUser,
    List<InvoiceStatus> statusList,
    ArchiveStatus archiveStatus,
    List<String> keywords,
    String customerEmail,
    String exactTitle,
    LocalDate sendingDateFrom,
    LocalDate sendingDateTo,
    Integer page,
    Integer pageSize) {

  public InvoiceCriteria {
    keywords = keywords == null ? List.of() : List.copyOf(keywords);
  }

  public static class InvoiceCriteriaBuilder {
    public InvoiceCriteriaBuilder sendingDateIn(YearMonth yearMonth) {
      this.sendingDateFrom = yearMonth == null ? null : yearMonth.atDay(1);
      this.sendingDateTo = yearMonth == null ? null : yearMonth.atEndOfMonth();
      return this;
    }
  }
}
