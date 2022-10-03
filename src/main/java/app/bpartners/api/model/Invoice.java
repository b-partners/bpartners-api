package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Invoice {
  private String id;
  private String fileId;
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
}
