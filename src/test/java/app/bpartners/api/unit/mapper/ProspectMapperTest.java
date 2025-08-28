package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.ContactNature.PROSPECT;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONTACTED;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectStatusHistory;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProspectMapperTest {
  BanApi banApi = mock();
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();
  ProspectMapper subject = new ProspectMapper(banApi, customDateFormatter);

  @Test
  void to_entity() {
    var lastEvaluationDate = Instant.now().minus(10, MINUTES);
    var orderedStatusHistory =
        ProspectStatusHistory.builder()
            .updatedAt(lastEvaluationDate.minus(1, MINUTES))
            .status(CONTACTED)
            .build();
    var location = new Geojson().latitude(50.10).longitude(50.10);
    var domain =
        Prospect.builder()
            .id("domain")
            .firstName("firstName")
            .phone("phone")
            .managerName("managerName")
            .name("name")
            .email("email")
            .address("address")
            .townCode(123)
            .defaultComment("defaultComment")
            .contactNature(PROSPECT)
            .statusHistories(List.of(orderedStatusHistory))
            .location(location)
            .build();
    var prospectOwnerId = "prospectOwnerId";
    var rating = 5.0;

    var actual = subject.toEntity(domain, prospectOwnerId, rating, lastEvaluationDate);

    var expected =
        HProspect.builder()
            .id(domain.getId())
            .firstName(domain.getFirstName())
            .oldPhone(domain.getPhone())
            .managerName(domain.getManagerName())
            .oldName(domain.getName())
            .oldEmail(domain.getEmail())
            .oldAddress(domain.getAddress())
            .idAccountHolder(prospectOwnerId)
            .townCode(domain.getTownCode())
            .rating(rating)
            .lastEvaluationDate(lastEvaluationDate)
            .posLongitude(location.getLongitude())
            .posLatitude(location.getLatitude())
            .statusHistories(
                List.of(
                    HProspectStatusHistory.builder()
                        .id(actual.getStatusHistories().getFirst().getId())
                        .status(domain.getActualStatus())
                        .updatedAt(actual.getStatusHistories().getFirst().getUpdatedAt())
                        .build()))
            .defaultComment(domain.getDefaultComment())
            .contactNature(domain.getContactNature())
            .build();
    assertEquals(expected, actual);
  }
}
