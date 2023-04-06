package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectRestMapper {
  private final ProspectRestValidator validator;

  public Prospect toRest(app.bpartners.api.model.Prospect domain) {
    return new Prospect()
        .id(domain.getId())
        .email(domain.getEmail())
        .name(domain.getName())
        .phone(domain.getPhone())
        .address(domain.getAddress())
        .location(domain.getLocation())
        .townCode(domain.getTownCode())
        .status(domain.getStatus());
  }

  public app.bpartners.api.model.Prospect toDomain(UpdateProspect rest) {
    validator.accept(rest);
    return app.bpartners.api.model.Prospect.builder()
        .id(rest.getId())
        .email(rest.getEmail())
        .name(rest.getName())
        .phone(rest.getPhone())
        .address(rest.getAddress())
        .status(rest.getStatus())
        .townCode(rest.getTownCode())
        .build();
  }
}
