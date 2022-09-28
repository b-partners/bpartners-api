package app.bpartners.api.model.mapper;

import app.bpartners.api.model.CustomerTemplate;
import app.bpartners.api.model.InvoiceCustomer;
import app.bpartners.api.repository.jpa.model.HCustomerTemplate;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

  public CustomerTemplate toDomain(HCustomerTemplate entity) {
    return InvoiceCustomer.customerTemplateBuilder()
        .customerId(entity.getId())
        .name(entity.getName())
        .email(entity.getEmail())
        .phone(entity.getPhone())
        .website(entity.getWebsite())
        .address(entity.getAddress())
        .zipCode(entity.getZipCode())
        .city(entity.getCity())
        .country(entity.getCountry())
        .build();
  }

  public HCustomerTemplate toEntity(CustomerTemplate domain) {
    return HCustomerTemplate.builder()
        .id(domain.getCustomerId())
        .idAccount(domain.getIdAccount())
        .email(domain.getEmail())
        .name(domain.getName())
        .phone(domain.getPhone())
        .website(domain.getWebsite())
        .address(domain.getAddress())
        .zipCode(domain.getZipCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .build();
  }
}
