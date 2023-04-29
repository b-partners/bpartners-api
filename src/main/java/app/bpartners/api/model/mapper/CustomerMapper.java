package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Customer;
import app.bpartners.api.repository.jpa.model.HCustomer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

  public Customer toDomain(HCustomer entity) {
    if (entity == null) {
      return null;
    }
    return Customer.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .firstName(entity.getFirstName())
        .lastName(entity.getLastName())
        .email(entity.getEmail())
        .phone(entity.getPhone())
        .website(entity.getWebsite())
        .address(entity.getAddress())
        .zipCode(entity.getZipCode())
        .city(entity.getCity())
        .country(entity.getCountry())
        .comment(entity.getComment())
        .status(entity.getStatus())
        .build();
  }

  public HCustomer toEntity(Customer domain) {
    return HCustomer.builder()
        .id(domain.getId())
        .idAccount(domain.getIdAccount())
        .email(domain.getEmail())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .phone(domain.getPhone())
        .website(domain.getWebsite())
        .address(domain.getAddress())
        .zipCode(domain.getZipCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .comment(domain.getComment())
        .status(domain.getStatus())
        .build();
  }
}
