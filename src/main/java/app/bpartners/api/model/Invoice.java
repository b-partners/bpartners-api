package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Invoice {
  private String id;
  private String title;
  private String ref;
  private LocalDate sendingDate;
  private LocalDate toPayAt;
  private int totalVat;
  private int totalPriceWithoutVat;
  private int totalPriceWithVat;
  private String paymentUrl;
  private InvoiceCustomer invoiceCustomer;
  private Account account;
  private List<Product> products;
  private InvoiceStatus status;

  public String getFileId() {
    return this.getRef() + PDF_EXTENSION;
  }

  public Date getFormattedSendingDate() {
    return Date.from(sendingDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  public Date getFormattedPayingDate() {
    return Date.from(toPayAt.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

}
