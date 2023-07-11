package app.bpartners.api.model.mapper;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.expressif.fact.Robbery;
import app.bpartners.api.repository.jpa.model.HProspectEval;
import app.bpartners.api.repository.jpa.model.HProspectEvalInfo;
import app.bpartners.api.service.utils.GeoUtils;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.jpa.model.HProspectEval.ProspectEvalRule.NEW_INTERVENTION;
import static app.bpartners.api.repository.jpa.model.HProspectEval.ProspectEvalRule.ROBBERY;
import static java.util.UUID.randomUUID;

@Component
public class ProspectEvalMapper {
  public HProspectEvalInfo toInfoEntity(
      ProspectEval evalDomain, Long reference, List<HProspectEval> prospectEvals) {
    ProspectEvalInfo prospect = evalDomain.getProspectEvalInfo();
    return HProspectEvalInfo.builder()
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
        .posLatitude(prospect.getCoordinates().getLatitude())
        .posLongitude(prospect.getCoordinates().getLongitude())
        .companyCreationDate(prospect.getCompanyCreationDate())
        .prospectEvals(prospectEvals)
        .build();
  }

  public HProspectEval toInfoEntity(
      ProspectEval evalDomain, Instant evaluationDate,
      Double prospectRating, Double customerRating) {
    String ruleType = evalDomain.getDepaRule().getClass().getTypeName();

    Boolean declared;
    Double interventionDistance;
    String interventionAddress;
    String oldCustomerAddress;
    Double oldCustomerDistance;
    HProspectEval.ProspectEvalRule evalRule;
    if (ruleType.equals(NewIntervention.class.getTypeName())) {
      NewIntervention rule = (NewIntervention) evalDomain.getDepaRule();
      evalRule = NEW_INTERVENTION;
      declared = rule.getPlanned();
      interventionAddress = rule.getNewIntAddress();
      interventionDistance = rule.getDistNewIntAndProspect();
      NewIntervention.OldCustomer oldCustomer = rule.getOldCustomer();
      oldCustomerAddress = oldCustomer == null ? null : oldCustomer.getOldCustomerAddress();
      oldCustomerDistance = oldCustomer == null ? null : oldCustomer.getDistNewIntAndOldCustomer();
    } else if (ruleType.equals(Robbery.class.getTypeName())) {
      Robbery rule = (Robbery) evalDomain.getDepaRule();
      evalRule = ROBBERY;
      declared = rule.getDeclared();
      interventionAddress = rule.getRobberyAddress();
      interventionDistance = rule.getDistRobberyAndProspect();
      Robbery.OldCustomer oldCustomer = rule.getOldCustomer();
      oldCustomerAddress = oldCustomer == null ? null : oldCustomer.getAddress();
      oldCustomerDistance = oldCustomer == null ? null : oldCustomer.getDistRobberyAndOldCustomer();
    } else {
      throw new ApiException(SERVER_EXCEPTION, "Unknown rule type " + ruleType);
    }
    return HProspectEval.builder()
        .id(String.valueOf(randomUUID()))
        .idProspectEvalInfo(evalDomain.getId())
        .rule(evalRule)
        .individualCustomer(evalDomain.getParticularCustomer())
        .professionalCustomer(evalDomain.getProfessionalCustomer())
        .declared(declared)
        .interventionAddress(interventionAddress)
        .interventionDistance(interventionDistance)
        .prospectRating(prospectRating)
        .oldCustomerAddress(oldCustomerAddress)
        .oldCustomerDistance(oldCustomerDistance)
        .customerRating(customerRating)
        .evaluationDate(evaluationDate)
        .build();
  }

  public ProspectResult toResultDomain(HProspectEvalInfo infoEntity) {
    if (infoEntity == null || infoEntity.getProspectEvals().isEmpty()) {
      return null;
    }
    // Eval Results are ordered by latest evaluation date
    HProspectEval eval = infoEntity.getProspectEvals().get(0);
    return ProspectResult.builder()
        .prospectEval(ProspectEval.builder()
            .id(infoEntity.getId())
            .prospectOwnerId(infoEntity.getIdAccountHolder())
            .prospectEvalInfo(toInfoDomain(infoEntity))
            .particularCustomer(eval.getIndividualCustomer())
            .professionalCustomer(eval.getProfessionalCustomer())
            .build())
        .interventionResult(eval.getInterventionAddress() == null ? null
            : new ProspectResult.InterventionResult(
            eval.getProspectRating(),
            eval.getInterventionDistance(),
            eval.getInterventionAddress()
        ))
        .customerInterventionResult(eval.getOldCustomerAddress() == null ? null
            : new ProspectResult.CustomerInterventionResult(
            eval.getCustomerRating(),
            eval.getOldCustomerDistance(),
            eval.getOldCustomerAddress()
        ))
        .evaluationDate(eval.getEvaluationDate())
        .build();
  }

  private ProspectEvalInfo toInfoDomain(HProspectEvalInfo infoEntity) {
    return ProspectEvalInfo.builder()
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
        .coordinates(GeoUtils.Coordinate.builder()
            .longitude(infoEntity.getPosLongitude())
            .latitude(infoEntity.getPosLatitude())
            .build())
        .build();
  }
}
