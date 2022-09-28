package app.bpartners.api.model.mapper;

import app.bpartners.api.model.CustomerTemplate;
import app.bpartners.api.model.InvoiceCustomer;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HCustomerTemplate;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceCustomer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceCustomerMapper {
  private final CustomerRepository customerRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final CustomerMapper customerMapper;

  public InvoiceCustomer toDomain(HInvoiceCustomer entity) {
    HCustomerTemplate customerTemplate = entity.getCustomerTemplate();
    InvoiceCustomer invoiceCustomer = InvoiceCustomer.customerTemplateBuilder()
        .customerId(customerTemplate.getId())
        .idAccount(customerTemplate.getIdAccount())
        .name(customerTemplate.getName())
        .email(customerTemplate.getEmail())
        .phone(customerTemplate.getPhone())
        .website(customerTemplate.getWebsite())
        .address(customerTemplate.getAddress())
        .zipCode(customerTemplate.getZipCode())
        .city(customerTemplate.getCity())
        .country(customerTemplate.getCountry())
        .build();
    invoiceCustomer.setId(entity.getId());
    invoiceCustomer.setIdInvoice(entity.getInvoice().getId());
    return refreshValues(invoiceCustomer, entity);
  }

  public HInvoiceCustomer toEntity(InvoiceCustomer domain) {
    CustomerTemplate customerTemplate = customerRepository.findById(domain.getCustomerId());
    HInvoice invoice = invoiceJpaRepository.getById(domain.getIdInvoice());
    return HInvoiceCustomer.builder()
        .id(domain.getId())
        .customerTemplate(customerMapper.toEntity(customerTemplate))
        .invoice(invoice)
        .email(domain.getEmail())
        .phone(domain.getPhone())
        .website(domain.getWebsite())
        .address(domain.getAddress())
        .zipCode(domain.getZipCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .build();
  }

  private InvoiceCustomer refreshValues(InvoiceCustomer invoiceCustomer, HInvoiceCustomer entity) {
    if (entity.getEmail() != null) {
      invoiceCustomer.setEmail(entity.getEmail());
    }
    if (entity.getPhone() != null) {
      invoiceCustomer.setPhone(entity.getPhone());
    }
    if (entity.getWebsite() != null) {
      invoiceCustomer.setWebsite(entity.getWebsite());
    }
    if (entity.getAddress() != null) {
      invoiceCustomer.setAddress(entity.getAddress());
    }
    if (entity.getZipCode() != null) {
      invoiceCustomer.setZipCode(entity.getZipCode());
    }
    if (entity.getCity() != null) {
      invoiceCustomer.setCity(entity.getCity());
    }
    if (entity.getCountry() != null) {
      invoiceCustomer.setCountry(entity.getCountry());
    }
    return invoiceCustomer;
  }
}
