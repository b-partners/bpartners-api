package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.repository.jpa.model.HCustomer;
import app.bpartners.api.service.utils.GeoUtils;
import java.time.Instant;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;

@Component
public class CustomerMapper {

  public Customer toDomain(HCustomer entity) {
    if (entity == null) {
      return null;
    }
    Double longitude = entity.getLongitude() == null ? null : entity.getLongitude();
    Double latitude = entity.getLatitude() == null ? null : entity.getLatitude();
    Location customerLocation = Location.builder()
        .address(entity.getAddress())
        .longitude(longitude)
        .latitude(latitude)
        .coordinate(GeoUtils.Coordinate.builder()
            .latitude(latitude)
            .longitude(longitude)
            .build())
        .build();
    return Customer.builder()
        .id(entity.getId())
        .idUser(entity.getIdUser())
        .name(entity.getName())
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
        .customerType(entity.getCustomerType())
        .recentlyAdded(entity.isRecentlyAdded())
        .updatedAt(entity.getUpdatedAt())
        .createdAt(entity.getCreatedAt())
        .latestFullAddress(entity.getLatestFullAddress())
        .build();
  }

  public HCustomer toEntity(Customer domain) {
    return HCustomer.builder()
        .id(domain.getId())
        .name(domain.getName())
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
        //TODO: use coordinate instead
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
        .customerType(domain.getCustomerType())
        .recentlyAdded(domain.isRecentlyAdded())
        .updatedAt(Instant.now())
        .latestFullAddress(domain.getLatestFullAddress())
        .build();
  }
}
