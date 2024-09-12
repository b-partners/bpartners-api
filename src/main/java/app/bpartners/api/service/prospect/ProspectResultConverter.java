package app.bpartners.api.service.prospect;

import static app.bpartners.api.service.ProspectService.defaultStatusHistory;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.service.utils.GeoUtils;
import org.springframework.stereotype.Component;

@Component
public class ProspectResultConverter {
  public Prospect fromEvaluationAttributes(
      ProspectResult result,
      ProspectEvaluation evaluation,
      ProspectEvaluationInfo evaluationInfo,
      GeoUtils.Coordinate coordinates) {
    Integer townCode;
    try {
      townCode = evaluationInfo == null ? null : Integer.valueOf(evaluationInfo.getPostalCode());
    } catch (NumberFormatException e) {
      townCode = null;
    }
    return Prospect.builder()
        .id(String.valueOf(randomUUID())) // TODO: change when prospect evaluation can be override
        .idHolderOwner(evaluation == null ? null : evaluation.getProspectOwnerId())
        .name(evaluationInfo == null ? null : evaluationInfo.getName())
        .managerName(evaluationInfo == null ? null : evaluationInfo.getManagerName())
        .email(evaluationInfo == null ? null : evaluationInfo.getEmail())
        .phone(evaluationInfo == null ? null : evaluationInfo.getPhoneNumber())
        .address(evaluationInfo == null ? null : evaluationInfo.getAddress())
        .statusHistories(defaultStatusHistory())
        .townCode(evaluationInfo == null ? null : townCode)
        .defaultComment(evaluationInfo == null ? null : evaluationInfo.getDefaultComment())
        .townCode(evaluationInfo == null ? null : Integer.valueOf(evaluationInfo.getPostalCode()))
        .location(
            new Geojson()
                .latitude(coordinates == null ? null : coordinates.getLatitude())
                .longitude(coordinates == null ? null : coordinates.getLongitude()))
        .rating(
            Prospect.ProspectRating.builder()
                .value(result == null ? null : result.getInterventionResult().getRating())
                .lastEvaluationDate(result == null ? null : result.getEvaluationDate())
                .build())
        .build();
  }

  public Prospect fromResultOnly(ProspectResult result) {
    ProspectEvaluation prospectEvaluation = result.getProspectEval();
    ProspectEvaluationInfo evaluationInfo = result.getProspectEval().getEvaluationInfo();
    GeoUtils.Coordinate coordinates = evaluationInfo.getCoordinates();
    return fromEvaluationAttributes(result, prospectEvaluation, evaluationInfo, coordinates);
  }

  public Prospect fromOldCustomer(ProspectResult result, Customer customer) {
    ProspectEvaluation prospectEvaluation = result.getProspectEval();
    result.getCustomerInterventionResult().setOldCustomer(customer);
    return Prospect.builder()
        .id(
            String.valueOf(
                randomUUID())) // TODO: change when prospect prospectEvaluation can be override
        .idHolderOwner(prospectEvaluation.getProspectOwnerId())
        .name(customer.getFullName())
        .managerName(customer.getFullName())
        .email(customer.getEmail())
        .phone(customer.getPhone())
        .address(customer.getFullAddress())
        .statusHistories(defaultStatusHistory())
        .townCode(customer.getZipCode())
        .location(
            new Geojson()
                .latitude(customer.getLocation().getCoordinate().getLatitude())
                .longitude(customer.getLocation().getCoordinate().getLongitude()))
        .rating(
            Prospect.ProspectRating.builder()
                .value(result.getInterventionResult().getRating())
                .lastEvaluationDate(result.getEvaluationDate())
                .build())
        .build();
  }
}
