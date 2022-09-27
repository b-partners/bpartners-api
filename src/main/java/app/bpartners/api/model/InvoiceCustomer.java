package app.bpartners.api.model;

import lombok.Builder;
import lombok.Data;

@Data
public class InvoiceCustomer extends CustomerTemplate {
  private String id;
  private String idInvoice;

  @Builder(builderMethodName = "customerTemplateBuilder")
  public InvoiceCustomer(
      String customerId, String idAccount, String name, String email,
      String phone, String website, String address, int zipCode,
      String city, String country, String id, String idInvoice) {
    super(customerId, idAccount, name, email, phone, website, address, zipCode, city, country);
    this.id = id;
    this.idInvoice = idInvoice;
  }
}
