package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.validator.CreateCustomerValidator;
import app.bpartners.api.endpoint.rest.validator.CustomerRestValidator;
import app.bpartners.api.model.CustomerTemplate;
import app.bpartners.api.model.InvoiceCustomer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CustomerRestMapper {
  private final CreateCustomerValidator validator;
  private final CustomerRestValidator restValidator;

  public Customer toRest(CustomerTemplate domain) {
    if (domain == null) {
      return null;
    }
    return new Customer()
        .id(domain.getCustomerId())
        .name(domain.getName())
        .phone(domain.getPhone())
        .website(domain.getWebsite())
        .email(domain.getEmail())
        .address(domain.getAddress())
        .zipCode(domain.getZipCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .city(domain.getCity());
  }

  public CustomerTemplate toDomain(String accountId, CreateCustomer rest) {
    validator.accept(rest);
    return InvoiceCustomer.customerTemplateBuilder()
        .customerId(null)
        .idAccount(accountId)
        .name(rest.getName())
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .build();
  }

  public InvoiceCustomer toDomain(String accountId, String idInvoice, Customer rest) {
    if (rest == null) {
      return null;
    }
    restValidator.accept(rest);
    InvoiceCustomer invoiceCustomer = InvoiceCustomer.customerTemplateBuilder()
        .customerId(rest.getId())
        .idAccount(accountId)
        .name(rest.getName())
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .build();
    invoiceCustomer.setIdInvoice(idInvoice);
    return invoiceCustomer;
  }
}
