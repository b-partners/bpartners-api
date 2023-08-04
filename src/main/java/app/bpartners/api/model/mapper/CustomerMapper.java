package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.repository.jpa.model.HCustomer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;

@Component
@Slf4j
public class CustomerMapper {

  public Customer toDomain(HCustomer entity) {
    if (entity == null) {
      return null;
    }
    Location customerLocation = Location.builder()
        .longitude(entity.getLongitude() == null ? null : entity.getLongitude())
        .latitude(entity.getLatitude() == null ? null : entity.getLatitude())
        .build();
    log.info("{} latitude", entity.getLatitude());
    log.info("{} longitude", entity.getLongitude());
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
        .location(customerLocation)
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
        .latitude(
            domain.getLocation().getLatitude() == null
                ? null
                : domain.getLocation().getLatitude())
        .longitude(
            domain.getLocation().getLongitude() == null
                ? null
                : domain.getLocation().getLongitude())
        .status(domain.getStatus() == null ? ENABLED
            : domain.getStatus())
        .recentlyAdded(domain.isRecentlyAdded())
        .build();
  }
}
