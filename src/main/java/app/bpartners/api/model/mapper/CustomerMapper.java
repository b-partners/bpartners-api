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
        .address(entity.getAddress())
        .build();
  }

  public HCustomer toEntity(Customer domain) {
    return HCustomer.builder()
        .id(domain.getId())
        .idAccount(domain.getIdAccount())
        .email(domain.getEmail())
        .name(domain.getName())
        .phone(domain.getPhone())
        .address(domain.getAddress())
        .build();
  }
}
