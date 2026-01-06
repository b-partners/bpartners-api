package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType.ADDRESS_CONVERSION;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.endpoint.rest.validator.CreateProspectValidator;
import app.bpartners.api.endpoint.rest.validator.ExtendedProspectUpdateValidator;
import app.bpartners.api.endpoint.rest.validator.ProspectRestValidator;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.service.utils.GeoUtils;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class ProspectRestMapperTest {
  ProspectRestValidator prospectRestValidator = new ProspectRestValidator();
  ExtendedProspectUpdateValidator extendedProspectUpdateValidatorMock = mock();
  CreateProspectValidator createProspectValidatorMock = mock();
  ProspectRestMapper subject =
      new ProspectRestMapper(
          prospectRestValidator, createProspectValidatorMock, extendedProspectUpdateValidatorMock);

  @Test
  void to_rest() {
    var coordinate = GeoUtils.Coordinate.builder().build();
    var info =
        ProspectEvalInfo.builder()
            .postalCode("123")
            .reference(1234L)
            .name("name")
            .phoneNumber("phoneNumber")
            .email("email")
            .address("address")
            .city("city")
            .coordinates(coordinate)
            .managerName("managerName")
            .contactNature(ProspectEvalInfo.ContactNature.PROSPECT)
            .build();
    var prospectEval = ProspectEval.builder().prospectEvalInfo(info).build();
    var interventionResult = new ProspectResult.InterventionResult(1.1, 10.1, "address");
    var now = now();
    var prospectResult =
        ProspectResult.builder()
            .prospectEval(prospectEval)
            .evaluationDate(now)
            .interventionResult(interventionResult)
            .build();

    var actual = subject.toRest(prospectResult);

    var expected =
        new EvaluatedProspect()
            .id(actual.getId())
            .reference(String.valueOf(info.getReference()))
            .name(info.getName())
            .phone(info.getPhoneNumber())
            .email(info.getEmail())
            .address(info.getAddress())
            .city(info.getCity())
            .townCode(Integer.valueOf(info.getPostalCode()))
            .area(new Area().geojson(new Geojson().type("Point")))
            .managerName(info.getManagerName())
            .contactNature(ContactNature.PROSPECT)
            .evaluationDate(prospectResult.getEvaluationDate())
            .interventionResult(
                new InterventionResult()
                    .address(interventionResult.getAddress())
                    .distanceFromProspect(BigDecimal.valueOf(interventionResult.getDistance()))
                    .value(BigDecimal.valueOf(interventionResult.getRating())))
            .oldCustomerResult(null);
    ;
    assertEquals(expected, actual);
  }

  @Test
  void to_rest_result() {
    var domain =
        ProspectEvaluationJob.builder()
            .id("id")
            .type(ADDRESS_CONVERSION)
            .startedAt(now())
            .endedAt(now().plus(1, ChronoUnit.MINUTES))
            .results(new ArrayList<>())
            .build();

    var actual = subject.toRestResult(domain);

    var expected =
        new ProspectEvaluationJobDetails()
            .id(domain.getId())
            .type(domain.getType())
            .status(domain.getJobStatus())
            .startedAt(domain.getStartedAt())
            .endedAt(domain.getEndedAt())
            .results(new ArrayList<>())
            .metadata(domain.getMetadata());
    assertEquals(expected, actual);
  }
}
