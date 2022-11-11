package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
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
    //Note that the status is not take into account here
    return invoice != null && Objects.equals(id, invoice.getId())
        && Objects.equals(title, invoice.getTitle())
        && Objects.equals(comment, invoice.getComment())
        && Objects.equals(ref, invoice.getRef())
        && sendingDate.compareTo(invoice.getSendingDate()) == 0
        && toPayAt.compareTo(invoice.getToPayAt()) == 0
        && totalVat == invoice.getTotalVat()
        && totalPriceWithoutVat == invoice.getTotalPriceWithoutVat()
        //&& Objects.equals(invoiceCustomer, invoice.invoiceCustomer)
        //&& Objects.equals(products, invoice.products)
        && Objects.equals(account, invoice.getAccount())
        && Objects.equals(fileId, invoice.fileId);
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
