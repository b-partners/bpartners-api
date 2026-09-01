package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.ProspectAnalyseRestMapper;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateProspectAnalyse;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.ProspectRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProspectAnalyseRestMapperTest {
  ProspectRepository prospectRepositoryMock = mock(ProspectRepository.class);
  ProspectRestMapper prospectRestMapperMock = mock(ProspectRestMapper.class);
  ProspectAnalyseRestMapper subject =
      new ProspectAnalyseRestMapper(prospectRepositoryMock, prospectRestMapperMock);

  @Test
  void to_domain_ok() {
    var prospect = Prospect.builder().id("prospectId").build();
    var createProspectAnalyse = new CreateProspectAnalyse().metadata(Map.of("key", "value"));
    when(prospectRepositoryMock.getById("prospectId")).thenReturn(prospect);

    var actual = subject.toDomain("prospectId", createProspectAnalyse);

    assertEquals(
        ProspectAnalyse.builder().prospect(prospect).metadata(Map.of("key", "value")).build(),
        actual);
  }

  @Test
  void to_rest_ok() {
    var prospect = Prospect.builder().id("prospectId").build();
    var restProspect = new app.bpartners.api.endpoint.rest.model.Prospect().id("prospectId");
    var createdAt = Instant.parse("2024-01-01T00:00:00Z");
    var updatedAt = Instant.parse("2024-01-02T00:00:00Z");
    var domain =
        ProspectAnalyse.builder()
            .id("analyseId")
            .prospect(prospect)
            .metadata(Map.of("key", "value"))
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    when(prospectRestMapperMock.toRest(prospect)).thenReturn(restProspect);

    var actual = subject.toRest(domain);

    assertEquals(
        new app.bpartners.api.endpoint.rest.model.ProspectAnalyse()
            .id("analyseId")
            .prospect(restProspect)
            .metadata(Map.of("key", "value"))
            .createdAt(createdAt)
            .updatedAt(updatedAt),
        actual);
  }
}
