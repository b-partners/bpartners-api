package app.bpartners.api.model.mapper;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.expressif.fact.Robbery;
import app.bpartners.api.repository.jpa.model.HProspectEvaluation;
import app.bpartners.api.repository.jpa.model.HProspectEvaluationInfo;
import app.bpartners.api.service.utils.GeoUtils;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.jpa.model.HProspectEvaluation.ProspectEvalRule.NEW_INTERVENTION;
import static app.bpartners.api.repository.jpa.model.HProspectEvaluation.ProspectEvalRule.ROBBERY;
import static java.util.UUID.randomUUID;

@Component
public class ProspectEvalMapper {
  public HProspectEvaluationInfo toInfoEntity(
      ProspectEvaluation evalDomain, Long reference, List<HProspectEvaluation> prospectEvals) {
    ProspectEvaluationInfo prospect = evalDomain.getProspectEvaluationInfo();
    GeoUtils.Coordinate coordinates = prospect.getCoordinates();
    return HProspectEvaluationInfo.builder()
        .id(evalDomain.getId())
        .idAccountHolder(evalDomain.getProspectOwnerId())
        .reference(reference)
        .name(prospect.getName())
        .phoneNumber(prospect.getPhoneNumber())
        .email(prospect.getEmail())
        .website(prospect.getWebsite())
        .address(prospect.getAddress())
        .managerName(prospect.getManagerName())
        .postalCode(prospect.getPostalCode())
        .city(prospect.getCity())
        .category(prospect.getCategory())
        .subcategory(prospect.getSubcategory())
        .contactNature(prospect.getContactNature())
        .posLatitude(coordinates == null ? null : coordinates.getLatitude())
        .posLongitude(coordinates == null ? null : coordinates.getLongitude())
        .companyCreationDate(prospect.getCompanyCreationDate())
        .prospectEvals(prospectEvals)
        .defaultComment(prospect.getDefaultComment())
        .build();
  }

  public HProspectEvaluation toInfoEntity(
      ProspectEvaluation evalDomain, Instant evaluationDate,
      Double prospectRating, Double customerRating) {
    Boolean declared;
    Double interventionDistance;
    String interventionAddress;
    String oldCustomerAddress;
    Double oldCustomerDistance;
    HProspectEvaluation.ProspectEvalRule evalRule;
    String idCustomer;
    if (evalDomain.isNewIntervention()) {
      NewIntervention rule = (NewIntervention) evalDomain.getDepaRule();
      evalRule = NEW_INTERVENTION;
      declared = rule.getPlanned();
      interventionAddress = rule.getNewIntAddress();
      interventionDistance = rule.getDistNewIntAndProspect();
      NewIntervention.OldCustomer oldCustomer = rule.getOldCustomer();
      idCustomer = oldCustomer.getIdCustomer();
      oldCustomerAddress = oldCustomer == null ? null : oldCustomer.getOldCustomerAddress();
      oldCustomerDistance = oldCustomer == null ? null : oldCustomer.getDistNewIntAndOldCustomer();
    } else if (evalDomain.isRobbery()) {
      Robbery rule = (Robbery) evalDomain.getDepaRule();
      evalRule = ROBBERY;
      declared = rule.getDeclared();
      interventionAddress = rule.getRobberyAddress();
      interventionDistance = rule.getDistRobberyAndProspect();
      Robbery.OldCustomer oldCustomer = rule.getOldCustomer();
      idCustomer = oldCustomer == null ? null : oldCustomer.getIdCustomer();
      oldCustomerAddress = oldCustomer == null ? null : oldCustomer.getAddress();
      oldCustomerDistance = oldCustomer == null ? null : oldCustomer.getDistRobberyAndOldCustomer();
    } else {
      throw new ApiException(SERVER_EXCEPTION,
          "Unknown rule type " + evalDomain.getDepaRule().getClass().getTypeName());
    }
    return HProspectEvaluation.builder()
        .id(String.valueOf(randomUUID()))
        .idProspectEvalInfo(evalDomain.getId())
        .rule(evalRule)
        .individualCustomer(evalDomain.getParticularCustomer())
        .professionalCustomer(evalDomain.getProfessionalCustomer())
        .declared(declared)
        .interventionAddress(interventionAddress)
        .interventionDistance(interventionDistance)
        .prospectRating(prospectRating)
        .idCustomer(idCustomer)
        .oldCustomerAddress(oldCustomerAddress)
        .oldCustomerDistance(oldCustomerDistance)
        .customerRating(customerRating)
        .evaluationDate(evaluationDate)
        .build();
  }

  public ProspectResult toResultDomain(HProspectEvaluationInfo infoEntity) {
    if (infoEntity == null || infoEntity.getProspectEvals().isEmpty()) {
      return null;
    }
    // Eval Results are ordered by latest evaluation date
    HProspectEvaluation eval = infoEntity.getProspectEvals().get(0);
    return ProspectResult.builder()
        .prospectEvaluation(ProspectEvaluation.builder()
            .id(infoEntity.getId())
            .prospectOwnerId(infoEntity.getIdAccountHolder())
            .prospectEvaluationInfo(toInfoDomain(infoEntity))
            .particularCustomer(eval.getIndividualCustomer())
            .professionalCustomer(eval.getProfessionalCustomer())
            .build())
        .interventionResult(eval.getInterventionAddress() == null ? null
            : new ProspectResult.InterventionResult(
            eval.getProspectRating(),
            eval.getInterventionDistance(),
            eval.getInterventionAddress()
        ))
        .customerInterventionResult(
            eval.getIdCustomer() == null || eval.getOldCustomerAddress() == null ? null
                : new ProspectResult.CustomerInterventionResult(
                eval.getCustomerRating(),
                eval.getOldCustomerDistance(),
                eval.getOldCustomerAddress(),
                eval.getIdCustomer()
            ))
        .evaluationDate(eval.getEvaluationDate())
        .build();
  }

  private ProspectEvaluationInfo toInfoDomain(HProspectEvaluationInfo infoEntity) {
    Double posLongitude = infoEntity.getPosLongitude();
    Double posLatitude = infoEntity.getPosLatitude();
    return ProspectEvaluationInfo.builder()
        .reference(infoEntity.getReference())
        .name(infoEntity.getName())
        .phoneNumber(infoEntity.getPhoneNumber())
        .email(infoEntity.getEmail())
        .website(infoEntity.getWebsite())
        .address(infoEntity.getAddress())
        .managerName(infoEntity.getManagerName())
        .mailSent(infoEntity.getMailSent())
        .postalCode(infoEntity.getPostalCode())
        .city(infoEntity.getCity())
        .category(infoEntity.getCategory())
        .subcategory(infoEntity.getSubcategory())
        .companyCreationDate(infoEntity.getCompanyCreationDate())
        .contactNature(infoEntity.getContactNature())
        .coordinates(posLatitude == null && posLongitude == null ? null
            : GeoUtils.Coordinate.builder()
            .longitude(posLongitude)
            .latitude(posLatitude)
            .build())
        .defaultComment(infoEntity.getDefaultComment())
        .build();
  }
}
