package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerRestMapper {
  public Customer toRest(app.bpartners.api.model.Customer domain) {
    return new Customer()
        .id(domain.getId())
        .name(domain.getName())
        .phone(domain.getPhone())
        .email(domain.getEmail())
        .address(domain.getAddress());
  }

  public app.bpartners.api.model.Customer toDomain(String accountId, CreateCustomer rest) {
    return app.bpartners.api.model.Customer.builder()
        .id(null)
        .idAccount(accountId)
        .name(rest.getName())
        .phone(rest.getPhone())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .build();
  }
}
