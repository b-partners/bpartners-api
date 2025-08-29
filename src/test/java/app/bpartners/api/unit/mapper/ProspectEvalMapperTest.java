package app.bpartners.api.unit.mapper;

import static app.bpartners.api.repository.expressif.ProspectEvalInfo.ContactNature.PROSPECT;
import static app.bpartners.api.repository.jpa.model.HProspectEval.ProspectEvalRule.NEW_INTERVENTION;
import static app.bpartners.api.repository.jpa.model.HProspectEval.ProspectEvalRule.ROBBERY;
import static java.time.LocalDate.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.mapper.ProspectEvalMapper;
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
import org.junit.jupiter.api.Test;

class ProspectEvalMapperTest {
  ProspectEvalMapper subject = new ProspectEvalMapper();

  @Test
  void to_info_entity_with_eval_domain_reference_prospect_eval() {
    var prospect = prospect();
    var evalDomain = evalDomain().toBuilder().prospectEvalInfo(prospect).build();
    var reference = 123L;
    var prospectEvals = HProspectEval.builder().build();

    var actual = subject.toInfoEntity(evalDomain, reference, List.of(prospectEvals));

    var expected =
        HProspectEvalInfo.builder()
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
            .prospectEvals(List.of(prospectEvals))
            .defaultComment(prospect.getDefaultComment())
            .build();
    assertEquals(expected, actual);
  }

  ProspectEval evalDomain() {
    return ProspectEval.builder().id("id").prospectOwnerId("prospectOwnerId").build();
  }

  ProspectEvalInfo prospect() {
    var coordinates = GeoUtils.Coordinate.builder().latitude(10.10).latitude(20.20).build();
    return ProspectEvalInfo.builder()
        .coordinates(coordinates)
        .name("name")
        .phoneNumber("phoneNumber")
        .email("email")
        .website("website")
        .address("address")
        .managerName("managerName")
        .postalCode("postalCode")
        .city("city")
        .category("category")
        .subcategory("subcategory")
        .companyCreationDate(now())
        .defaultComment("defaultComment")
        .contactNature(PROSPECT)
        .build();
  }

  @Test
  void
      to_info_entity_with_eval_domain_is_new_intervention_evaluation_date_prospect_rating_customer_rating() {
    var oldCustomer =
        NewIntervention.OldCustomer.builder()
            .idCustomer("idCustomer")
            .oldCustomerAddress("oldCustomerAddress")
            .distNewIntAndOldCustomer(10.1)
            .build();
    var rule =
        NewIntervention.builder()
            .planned(true)
            .newIntAddress("newIntAddress")
            .distNewIntAndProspect(10.1)
            .oldCustomer(oldCustomer)
            .build();
    var evalDomain = evalDomain().toBuilder().depaRule(rule).build();
    var evaluationDate = Instant.now();
    var prospectRating = 1.1;
    var customerRating = 1.2;

    var actual = subject.toInfoEntity(evalDomain, evaluationDate, prospectRating, customerRating);

    var expected =
        HProspectEval.builder()
            .id(actual.getId())
            .idProspectEvalInfo(evalDomain.getId())
            .rule(NEW_INTERVENTION)
            .individualCustomer(evalDomain.getParticularCustomer())
            .professionalCustomer(evalDomain.getProfessionalCustomer())
            .declared(rule.getPlanned())
            .interventionAddress(rule.getNewIntAddress())
            .interventionDistance(rule.getDistNewIntAndProspect())
            .prospectRating(prospectRating)
            .idCustomer(oldCustomer.getIdCustomer())
            .oldCustomerAddress(oldCustomer.getOldCustomerAddress())
            .oldCustomerDistance(oldCustomer.getDistNewIntAndOldCustomer())
            .customerRating(customerRating)
            .evaluationDate(evaluationDate)
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void
      to_info_entity_with_eval_domain_is_robbery_evaluation_date_prospect_rating_customer_rating() {
    var oldCustomer =
        Robbery.OldCustomer.builder()
            .idCustomer("idCustomer")
            .address("oldCustomerAddress")
            .distRobberyAndOldCustomer(10.1)
            .build();
    var rule =
        Robbery.builder()
            .declared(true)
            .robberyAddress("robberyAddress")
            .distRobberyAndProspect(10.1)
            .oldCustomer(oldCustomer)
            .build();
    var evalDomain = evalDomain().toBuilder().depaRule(rule).build();
    var evaluationDate = Instant.now();
    var prospectRating = 1.1;
    var customerRating = 1.2;

    var actual = subject.toInfoEntity(evalDomain, evaluationDate, prospectRating, customerRating);

    var expected =
        HProspectEval.builder()
            .id(actual.getId())
            .idProspectEvalInfo(evalDomain.getId())
            .rule(ROBBERY)
            .individualCustomer(evalDomain.getParticularCustomer())
            .professionalCustomer(evalDomain.getProfessionalCustomer())
            .declared(rule.getDeclared())
            .interventionAddress(rule.getRobberyAddress())
            .interventionDistance(rule.getDistRobberyAndProspect())
            .prospectRating(prospectRating)
            .idCustomer(oldCustomer.getIdCustomer())
            .oldCustomerAddress(oldCustomer.getAddress())
            .oldCustomerDistance(oldCustomer.getDistRobberyAndOldCustomer())
            .customerRating(customerRating)
            .evaluationDate(evaluationDate)
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void to_result_domain() {
    var eval =
        HProspectEval.builder()
            .individualCustomer(true)
            .professionalCustomer(false)
            .interventionAddress("interventionAddress")
            .prospectRating(5.5)
            .interventionDistance(10.10)
            .interventionAddress("interventionAddress")
            .idCustomer("idCustomer")
            .oldCustomerAddress("oldCustomerAddress")
            .customerRating(10.10)
            .oldCustomerDistance(10.10)
            .oldCustomerAddress("oldCustomerAddress")
            .idCustomer("idCustomer")
            .evaluationDate(Instant.now())
            .build();
    var infoEntity =
        HProspectEvalInfo.builder()
            .prospectEvals(List.of(eval))
            .id("id")
            .idAccountHolder("idAccountHolder")
            .posLongitude(10.10)
            .posLatitude(20.20)
            .reference(123L)
            .name("name")
            .phoneNumber("phoneNumber")
            .email("email")
            .website("website")
            .address("address")
            .managerName("managerName")
            .mailSent("mailSent")
            .postalCode("postalCode")
            .city("city")
            .category("category")
            .subcategory("subcategory")
            .companyCreationDate(now())
            .contactNature(PROSPECT)
            .defaultComment("defaultComment")
            .build();
    var toInfoDomain =
        ProspectEvalInfo.builder()
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
            .coordinates(
                GeoUtils.Coordinate.builder()
                    .longitude(infoEntity.getPosLongitude())
                    .latitude(infoEntity.getPosLatitude())
                    .build())
            .defaultComment(infoEntity.getDefaultComment())
            .build();

    var actual = subject.toResultDomain(infoEntity);

    var expected =
        ProspectResult.builder()
            .prospectEval(
                ProspectEval.builder()
                    .id(infoEntity.getId())
                    .prospectOwnerId(infoEntity.getIdAccountHolder())
                    .prospectEvalInfo(toInfoDomain)
                    .particularCustomer(eval.getIndividualCustomer())
                    .professionalCustomer(eval.getProfessionalCustomer())
                    .build())
            .interventionResult(
                eval.getInterventionAddress() == null
                    ? null
                    : new ProspectResult.InterventionResult(
                        eval.getProspectRating(),
                        eval.getInterventionDistance(),
                        eval.getInterventionAddress()))
            .customerInterventionResult(
                eval.getIdCustomer() == null || eval.getOldCustomerAddress() == null
                    ? null
                    : new ProspectResult.CustomerInterventionResult(
                        eval.getCustomerRating(),
                        eval.getOldCustomerDistance(),
                        eval.getOldCustomerAddress(),
                        eval.getIdCustomer()))
            .evaluationDate(eval.getEvaluationDate())
            .build();
    assertEquals(expected, actual);
  }
}
