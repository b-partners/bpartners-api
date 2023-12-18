package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Area;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.ExtendedProspectStatus;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.InterventionResult;
import app.bpartners.api.endpoint.rest.model.OldCustomerResult;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobDetails;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobInfo;
import app.bpartners.api.endpoint.rest.model.ProspectRating;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.validator.ExtendedProspectUpdateValidator;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.service.utils.GeoUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
public class ProspectRestMapper {
  private final ProspectRestValidator validator;
  private final ExtendedProspectUpdateValidator prospectUpdateValidator;

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
    Integer postalCode;
    try {
      postalCode = Integer.valueOf(info.getPostalCode());
    } catch (NumberFormatException e) {
      postalCode = null;
    }
    return new EvaluatedProspect()
        .id(String.valueOf(randomUUID())) //TODO: return persisted ID
        .reference(String.valueOf(info.getReference()))
        .name(customerInfo == null ? info.getName()
            : customerInfo.getFullName())
        .phone(customerInfo == null ? info.getPhoneNumber()
            : customerInfo.getPhone())
        .email(customerInfo == null ? info.getEmail()
            : customerInfo.getEmail())
        .address(customerInfo == null ? info.getAddress()
            : customerInfo.getFullAddress())
        .city(customerInfo == null ? info.getCity()
            : customerInfo.getCity())
        .townCode(customerInfo == null ? postalCode
            : customerInfo.getZipCode()
        )
        .area(new Area().geojson(
            customerInfo == null
                ? toGeoJson(info.getCoordinates())
                : toGeoJson(customerInfo.getLocation().getCoordinate())))
        .managerName(customerInfo == null ? info.getManagerName()
            : customerInfo.getFullName())
        .contactNature(customerResult != null ? ContactNature.OLD_CUSTOMER
            : getProspect(info))
        .evaluationDate(prospectResult.getEvaluationDate())
        .interventionResult(interventionResult == null || customerResult != null ? null
            : new InterventionResult()
            .address(interventionResult.getAddress())
            .distanceFromProspect(
                BigDecimal.valueOf(interventionResult.getDistance()))
            .value(BigDecimal.valueOf(interventionResult.getRating())))
        .oldCustomerResult(customerResult == null ? null
            : new OldCustomerResult()
            .address(customerResult.getAddress())
            .distanceFromProspect(BigDecimal.valueOf(customerResult.getDistance()))
            .value(BigDecimal.valueOf(customerResult.getRating())));
  }

  public Prospect toRest(app.bpartners.api.model.prospect.Prospect domain) {
    app.bpartners.api.model.prospect.Prospect.ProspectRating prospectRating = domain.getRating();
    return new Prospect()
        .id(domain.getId())
        .email(domain.getEmail())
        .name(domain.getName())
        .managerName(domain.getManagerName())
        .phone(domain.getPhone())
        .address(domain.getAddress())
        .location(domain.getLocation())
        .townCode(domain.getTownCode())
        .status(domain.getActualStatus())
        .statusHistory(domain.getStatusHistories().stream()
            .map(this::toRest)
            .collect(Collectors.toList()))
        .rating(prospectRating == null ? null
            : new ProspectRating()
            .lastEvaluation(prospectRating.getLastEvaluationDate())
            .value(prospectRating.getValue() == null ? BigDecimal.valueOf(-1.0)
                : BigDecimal.valueOf(prospectRating.getValue())))
        .comment(domain.getComment())
        .defaultComment(domain.getDefaultComment())
        .contractAmount(domain.getContractAmount() == null
            ? null
            : domain.getContractAmount().getCentsRoundUp())
        .invoiceID(domain.getIdInvoice())
        .prospectFeedback(domain.getProspectFeedback())
        .contactNature(domain.getContactNature());
  }

  private app.bpartners.api.endpoint.rest.model.ProspectStatusHistory toRest(
      ProspectStatusHistory statusHistory) {
    return new app.bpartners.api.endpoint.rest.model.ProspectStatusHistory()
        .status(statusHistory.getStatus())
        .updatedAt(statusHistory.getUpdatedAt());
  }

  public ProspectEvaluationJobInfo toRest(ProspectEvaluationJob domain) {
    return new ProspectEvaluationJobInfo()
        .id(domain.getId())
        .type(domain.getType())
        .status(domain.getJobStatus())
        .startedAt(domain.getStartedAt())
        .endedAt(domain.getEndedAt())
        .metadata(domain.getMetadata());
  }

  public ProspectEvaluationJobDetails toRestResult(ProspectEvaluationJob domain) {
    return new ProspectEvaluationJobDetails()
        .id(domain.getId())
        .type(domain.getType())
        .status(domain.getJobStatus())
        .startedAt(domain.getStartedAt())
        .endedAt(domain.getEndedAt())
        .results(domain.getResults().stream()
            .map(this::toRest)
            .collect(Collectors.toList()))
        .metadata(domain.getMetadata());
  }

  public app.bpartners.api.model.prospect.Prospect toDomain(String ownerId,
                                                            UpdateProspect rest) {
    validator.accept(rest);
    return app.bpartners.api.model.prospect.Prospect.builder()
        .id(rest.getId())
        .idHolderOwner(ownerId)
        .email(rest.getEmail())
        .name(rest.getName())
        .managerName(rest.getManagerName())
        .phone(rest.getPhone())
        .address(rest.getAddress())
        .statusHistories(List.of(ProspectStatusHistory.builder()
            .status(rest.getStatus())
            .updatedAt(Instant.now())
            .build()))
        .townCode(rest.getTownCode())
        .build();
  }

  public app.bpartners.api.model.prospect.Prospect toDomain(String ownerId,
                                                            ExtendedProspectStatus rest) {
    prospectUpdateValidator.accept(rest);
    return app.bpartners.api.model.prospect.Prospect.builder()
        .id(rest.getId())
        .idHolderOwner(ownerId)
        .email(rest.getEmail())
        .name(rest.getName())
        .managerName(rest.getManagerName())
        .phone(rest.getPhone())
        .address(rest.getAddress())
        .statusHistories(List.of(ProspectStatusHistory.builder()
            .status(rest.getStatus())
            .updatedAt(Instant.now())
            .build()))
        .townCode(rest.getTownCode())
        .comment(rest.getComment())
        .contractAmount(parseFraction(rest.getContractAmount()))
        .idInvoice(rest.getInvoiceID())
        .prospectFeedback(rest.getProspectFeedback())
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
