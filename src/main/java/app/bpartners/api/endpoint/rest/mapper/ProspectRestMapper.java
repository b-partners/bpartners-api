package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Area;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.InterventionResult;
import app.bpartners.api.endpoint.rest.model.OldCustomerResult;
import app.bpartners.api.endpoint.rest.model.Prospect;
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
    ProspectEval eval = prospectResult.getProspectEval();
    ProspectEvalInfo info = eval.getProspectEvalInfo();
    Geojson geoJson = new Geojson()
        .type("Point") //default type
        .latitude(info.getCoordinates().getLatitude())
        .longitude(info.getCoordinates().getLongitude());
    ProspectResult.CustomerInterventionResult custRes =
        prospectResult.getCustomerInterventionResult();
    ProspectResult.InterventionResult intRes = prospectResult.getInterventionResult();
    return new EvaluatedProspect()
        .id(String.valueOf(randomUUID()))
        .reference(String.valueOf(info.getReference()))
        .name(info.getName())
        .phone(info.getPhoneNumber())
        .email(info.getEmail())
        .address(info.getAddress())
        .city(info.getCity())
        .townCode(Integer.valueOf(info.getPostalCode()))
        .area(new Area().geojson(geoJson))
        .managerName(info.getManagerName())
        .contactNature(getProspect(info))
        .evaluationDate(prospectResult.getEvaluationDate())
        .interventionResult(intRes == null ? null
            : new InterventionResult()
            .address(intRes.getAddress())
            .distanceFromProspect(
                BigDecimal.valueOf(intRes.getDistance()))
            .value(BigDecimal.valueOf(intRes.getRating())))
        .oldCustomerResult(custRes == null ? null
            : new OldCustomerResult()
            .address(custRes.getAddress())
            .distanceFromProspect(BigDecimal.valueOf(custRes.getDistance()))
            .value(BigDecimal.valueOf(custRes.getRating())));
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

  private ContactNature getProspect(ProspectEvalInfo info) {
    ProspectEvalInfo.ContactNature domainNature = info.getContactNature();
    if (domainNature == ProspectEvalInfo.ContactNature.PROSPECT) {
      return ContactNature.PROSPECT;
    } else if (domainNature == ProspectEvalInfo.ContactNature.OLD_CUSTOMER) {
      return ContactNature.OLD_CUSTOMER;
    }
    return ContactNature.OTHER;
  }
}
