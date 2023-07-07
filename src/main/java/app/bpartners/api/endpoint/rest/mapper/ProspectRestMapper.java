package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class ProspectRestMapper {
  private final ProspectRestValidator validator;

  public EvaluatedProspect toRest(ProspectResult prospectResult) {
    if (prospectResult == null) {
      return null;
    }
    ProspectEval prospectEval = prospectResult.getProspectEval();
    ProspectEvalInfo prospect = prospectEval.getProspectEvalInfo();
    Geojson geoJson = new Geojson()
        .type("Point") //default type
        .latitude(prospect.getCoordinates().getLatitude())
        .longitude(prospect.getCoordinates().getLongitude());
    return new EvaluatedProspect()
        .prospect(new Prospect()
            .id(String.valueOf(randomUUID()))
            .name(prospect.getName())
            .address(prospect.getAddress())
            .phone(prospect.getPhoneNumber())
            .email(prospect.getEmail())
            .townCode(Integer.valueOf(prospect.getPostalCode()))
            .location(geoJson)
            .status(ProspectStatus.TO_CONTACT) //TODO
        )
        .evaluationDate(prospectResult.getEvaluationDate())
        .rating(BigDecimal.valueOf(prospectResult.getRating()));
  }

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
