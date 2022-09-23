package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Customer;
import app.bpartners.api.repository.jpa.model.HCustomer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

  public Customer toDomain(HCustomer entity) {
    return Customer.builder()
        .id(entity.getId())
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

  public HCustomer toEntity(Customer domain) {
    return HCustomer.builder()
        .id(domain.getId())
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
