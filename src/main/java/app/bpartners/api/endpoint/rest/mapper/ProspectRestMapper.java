package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Prospect;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectRestMapper {
  public Prospect toRest(app.bpartners.api.model.Prospect domain) {
    return new Prospect()
        .id(domain.getId())
        .email(domain.getEmail())
        .name(domain.getName())
        .phone(domain.getPhone())
        .location(domain.getLocation())
        .status(domain.getStatus());
  }

  public app.bpartners.api.model.Prospect toDomain(Prospect rest) {
    return app.bpartners.api.model.Prospect.builder()
        .id(rest.getId())
        .email(rest.getEmail())
        .name(rest.getName())
        .phone(rest.getPhone())
        .location(rest.getLocation())
        .status(rest.getStatus())
        .build();
  }
}
