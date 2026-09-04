package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.ProspectAnalyseRepository;
import app.bpartners.api.service.prospect.ProspectAnalyseService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProspectAnalyseServiceTest {
  ProspectAnalyseRepository repositoryMock = mock(ProspectAnalyseRepository.class);
  ProspectAnalyseService subject = new ProspectAnalyseService(repositoryMock);

  @Test
  void create_ok() {
    var toCreate = ProspectAnalyse.builder().metadata(Map.of("key", "value")).build();
    var saved = toCreate.toBuilder().id("analyseId").build();
    when(repositoryMock.save(toCreate)).thenReturn(saved);

    var actual = subject.create(toCreate);

    assertEquals(saved, actual);
  }

  @Test
  void get_by_prospect_id_ok() {
    var analyse = ProspectAnalyse.builder().id("analyseId").build();
    when(repositoryMock.findAllByIdProspect("prospectId")).thenReturn(List.of(analyse));

    var actual = subject.getByProspectId("prospectId");

    assertEquals(List.of(analyse), actual);
  }

  @Test
  void get_by_id_ok() {
    var analyse = ProspectAnalyse.builder().id("analyseId").build();
    when(repositoryMock.findById("analyseId")).thenReturn(Optional.of(analyse));

    var actual = subject.getById("analyseId");

    assertEquals(analyse, actual);
  }

  @Test
  void get_by_id_throws_not_found() {
    var unknownId = randomUUID().toString();
    when(repositoryMock.findById(unknownId)).thenReturn(Optional.empty());

    var actualException = assertThrows(NotFoundException.class, () -> subject.getById(unknownId));

    assertEquals("ProspectAnalyse(id=" + unknownId + ") not found", actualException.getMessage());
  }

  @Test
  void update_ok() {
    var existing = ProspectAnalyse.builder().id("analyseId").metadata(Map.of("key", "old")).build();
    var updated = existing.toBuilder().metadata(Map.of("key", "new")).build();
    when(repositoryMock.findById("analyseId")).thenReturn(Optional.of(existing));
    when(repositoryMock.save(updated)).thenReturn(updated);

    var actual = subject.update("analyseId", Map.of("key", "new"));

    assertEquals(updated, actual);
  }
}
