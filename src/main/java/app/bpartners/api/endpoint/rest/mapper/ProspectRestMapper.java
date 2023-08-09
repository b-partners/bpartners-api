package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Area;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.InterventionResult;
import app.bpartners.api.endpoint.rest.model.OldCustomerResult;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectRating;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import app.bpartners.api.model.Customer;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.service.utils.GeoUtils;
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
    ProspectResult.InterventionResult interventionResult = prospectResult.getInterventionResult();
    ProspectResult.CustomerInterventionResult customerResult =
        prospectResult.getCustomerInterventionResult();
    Customer customerInfo = customerResult == null ? null
        : customerResult.getOldCustomer();
    return new EvaluatedProspect()
        .id(String.valueOf(randomUUID()))
        .reference(String.valueOf(info.getReference()))
        .name(customerInfo == null ? info.getName()
            : customerInfo.getName())
        .phone(customerInfo == null ? info.getPhoneNumber()
            : customerInfo.getPhone())
        .email(customerInfo == null ? info.getEmail()
            : customerInfo.getEmail())
        .address(customerInfo == null ? info.getAddress()
            : customerInfo.getFullAddress())
        .city(customerInfo == null ? info.getCity()
            : customerInfo.getCity())
        .townCode(customerInfo == null ? Integer.valueOf(info.getPostalCode())
            : customerInfo.getZipCode()
        )
        .area(new Area().geojson(
            customerInfo == null
                ? toGeoJson(info.getCoordinates())
                : toGeoJson(customerInfo.getLocation().getCoordinate())))
        .managerName(customerInfo == null ? info.getManagerName()
            : customerInfo.getName())
        .contactNature(customerInfo == null ? getProspect(info)
            : ContactNature.OLD_CUSTOMER)
        .evaluationDate(prospectResult.getEvaluationDate())
        .interventionResult(interventionResult == null || customerInfo != null ? null
            : new InterventionResult()
            .address(interventionResult.getAddress())
            .distanceFromProspect(
                BigDecimal.valueOf(interventionResult.getDistance()))
            .value(BigDecimal.valueOf(interventionResult.getRating())))
        .oldCustomerResult(customerInfo == null ? null
            : new OldCustomerResult()
            .address(customerResult.getAddress())
            .distanceFromProspect(BigDecimal.valueOf(customerResult.getDistance()))
            .value(BigDecimal.valueOf(customerResult.getRating())));
  }

  public Prospect toRest(app.bpartners.api.model.Prospect domain) {
    app.bpartners.api.model.Prospect.ProspectRating prospectRating = domain.getRating();
    return new Prospect()
        .id(domain.getId())
        .email(domain.getEmail())
        .name(domain.getName())
        .phone(domain.getPhone())
        .address(domain.getAddress())
        .location(domain.getLocation())
        .townCode(domain.getTownCode())
        .status(domain.getStatus())
        .rating(prospectRating == null ? null
            : new ProspectRating()
            .lastEvaluation(prospectRating.getLastEvaluationDate())
            .value(prospectRating.getValue() == null ? BigDecimal.valueOf(-1.0)
                : BigDecimal.valueOf(prospectRating.getValue())));
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

  private Geojson toGeoJson(GeoUtils.Coordinate coordinate) {
    return new Geojson()
        .type("Point") //default type
        .latitude(coordinate.getLatitude())
        .longitude(coordinate.getLongitude());
  }
}
