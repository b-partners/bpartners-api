package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.exception.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Invoice {
  private String id;
  private String title;
  private String ref;
  private LocalDate sendingDate;
  private LocalDate toPayAt;
  private Fraction totalVat;
  private Fraction totalPriceWithoutVat;
  private Fraction totalPriceWithVat;
  private String paymentUrl;
  private InvoiceCustomer invoiceCustomer;
  private Account account;
  private List<Product> products;
  private InvoiceStatus status;
  private String comment;
  private Instant updatedAt;
  private String fileId;
  private boolean toBeRelaunched;

  public String getRealReference() {
    if (getRef() == null) {
      return null;
    }
    if (getRef().contains(DRAFT_REF_PREFIX)) {
      return getRef().replace(DRAFT_REF_PREFIX, "");
    }
    return getRef();
  }

  public Date getFormattedSendingDate() {
    if (sendingDate == null) {
      return null;
    }
    return Date.from(sendingDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  public Date getFormattedPayingDate() {
    if (toPayAt == null) {
      return null;
    }
    return Date.from(toPayAt.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    Invoice invoice = (Invoice) o;
    return invoice != null && Objects.equals(id, invoice.getId())
        && Objects.equals(title, invoice.getTitle())
        && Objects.equals(comment, invoice.getComment())
        && Objects.equals(this.getRealReference(), invoice.getRealReference())
        && sendingDate.compareTo(invoice.getSendingDate()) == 0
        && toPayAt.compareTo(invoice.getToPayAt()) == 0
        && Objects.equals(products, invoice.products)
        //&& Objects.equals(invoiceCustomer, invoice.invoiceCustomer)
        && Objects.equals(account, invoice.getAccount());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  //TODO : test equals and hashcode
  //TODO add accepted in database.
  public InvoiceStatus getPreviousStatus() {
    switch (status) {
      case DRAFT:
      case PROPOSAL:
      case ACCEPTED:
        return DRAFT;
      case CONFIRMED:
        return PROPOSAL;
      case PAID:
        return CONFIRMED;
      default:
        throw new ApiException(SERVER_EXCEPTION, "Unexpected status " + status);
    }
  }
}
