package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Customer;
import app.bpartners.api.repository.jpa.model.HCustomer;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;

@Component
public class CustomerMapper {

  public Customer toDomain(HCustomer entity) {
    if (entity == null) {
      return null;
    }
    return Customer.builder()
        .id(entity.getId())
        .idUser(entity.getIdUser())
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
        .latitude(entity.getLatitude())
        .longitude(entity.getLongitude())
        .status(entity.getStatus())
        .recentlyAdded(entity.isRecentlyAdded())
        .build();
  }

  public HCustomer toEntity(Customer domain) {
    return HCustomer.builder()
        .id(domain.getId())
        .idUser(domain.getIdUser())
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
        .latitude(domain.getLatitude())
        .longitude(domain.getLongitude())
        .status(domain.getStatus() == null ? ENABLED
            : domain.getStatus())
        .recentlyAdded(domain.isRecentlyAdded())
        .build();
  }
}
