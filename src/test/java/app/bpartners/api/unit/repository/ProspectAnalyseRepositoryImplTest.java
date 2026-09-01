package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.mapper.ProspectAnalyseMapper;
import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.implementation.ProspectAnalyseRepositoryImpl;
import app.bpartners.api.repository.jpa.ProspectAnalyseJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspectAnalyse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProspectAnalyseRepositoryImplTest {
  ProspectAnalyseJpaRepository jpaRepositoryMock;
  ProspectAnalyseMapper mapperMock;
  ProspectAnalyseRepositoryImpl subject;

  @BeforeEach
  void setUp() {
    jpaRepositoryMock = mock(ProspectAnalyseJpaRepository.class);
    mapperMock = mock(ProspectAnalyseMapper.class);
    subject = new ProspectAnalyseRepositoryImpl(jpaRepositoryMock, mapperMock);
  }

  @Test
  void save_ok() {
    var domain = ProspectAnalyse.builder().build();
    var entity = HProspectAnalyse.builder().id("analyseId").build();
    var savedEntity = entity.toBuilder().build();
    var savedDomain = domain.toBuilder().id("analyseId").build();
    when(mapperMock.toEntity(domain)).thenReturn(entity);
    when(jpaRepositoryMock.save(entity)).thenReturn(savedEntity);
    when(mapperMock.toDomain(savedEntity)).thenReturn(savedDomain);

    var actual = subject.save(domain);

    assertEquals(savedDomain, actual);
  }

  @Test
  void find_all_by_id_prospect_ok() {
    var entity = HProspectAnalyse.builder().id("analyseId").idProspect("prospectId").build();
    var domain = ProspectAnalyse.builder().id("analyseId").build();
    when(jpaRepositoryMock.findAllByIdProspect("prospectId")).thenReturn(List.of(entity));
    when(mapperMock.toDomain(entity)).thenReturn(domain);

    var actual = subject.findAllByIdProspect("prospectId");

    assertEquals(List.of(domain), actual);
  }

  @Test
  void find_by_id_found() {
    var entity = HProspectAnalyse.builder().id("analyseId").build();
    var domain = ProspectAnalyse.builder().id("analyseId").build();
    when(jpaRepositoryMock.findById("analyseId")).thenReturn(Optional.of(entity));
    when(mapperMock.toDomain(entity)).thenReturn(domain);

    var actual = subject.findById("analyseId");

    assertEquals(Optional.of(domain), actual);
  }

  @Test
  void find_by_id_not_found() {
    when(jpaRepositoryMock.findById("unknownId")).thenReturn(Optional.empty());

    var actual = subject.findById("unknownId");

    assertTrue(actual.isEmpty());
  }
}
