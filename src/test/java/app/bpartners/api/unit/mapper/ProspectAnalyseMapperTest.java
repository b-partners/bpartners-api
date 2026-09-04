package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.mapper.ProspectAnalyseMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.jpa.model.HProspectAnalyse;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProspectAnalyseMapperTest {
  ProspectRepository prospectRepositoryMock = mock(ProspectRepository.class);
  ProspectAnalyseMapper subject = new ProspectAnalyseMapper(prospectRepositoryMock);

  @Test
  void to_entity_ok() {
    var prospect = Prospect.builder().id("prospectId").build();
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

    var actual = subject.toEntity(domain);

    assertEquals("analyseId", actual.getId());
    assertEquals("prospectId", actual.getIdProspect());
    assertEquals(Map.of("key", "value"), actual.getMetadata());
    assertEquals(createdAt, actual.getCreatedAt());
    assertEquals(updatedAt, actual.getUpdatedAt());
  }

  @Test
  void to_domain_ok() {
    var prospect = Prospect.builder().id("prospectId").build();
    var createdAt = Instant.parse("2024-01-01T00:00:00Z");
    var updatedAt = Instant.parse("2024-01-02T00:00:00Z");
    var entity =
        HProspectAnalyse.builder()
            .id("analyseId")
            .idProspect("prospectId")
            .metadata(Map.of("key", "value"))
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    when(prospectRepositoryMock.getById("prospectId")).thenReturn(prospect);

    var actual = subject.toDomain(entity);

    assertEquals(
        ProspectAnalyse.builder()
            .id("analyseId")
            .prospect(prospect)
            .metadata(Map.of("key", "value"))
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build(),
        actual);
  }
}
