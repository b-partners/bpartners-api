package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.model.AreaPictureAnnotationCriteria;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import app.bpartners.api.service.areapicture.AreaPictureService;
import app.bpartners.api.service.aws.S3Service;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AreaPictureAnnotationServiceFilterTest {
  AreaPictureAnnotationService subject;
  AreaPictureAnnotationRepository repositoryMock;
  AreaPictureService areaPictureServiceMock;
  ProspectRepository prospectRepositoryMock;

  @BeforeEach
  void setUp() {
    repositoryMock = mock(AreaPictureAnnotationRepository.class);
    areaPictureServiceMock = mock(AreaPictureService.class);
    prospectRepositoryMock = mock(ProspectRepository.class);
    subject =
        new AreaPictureAnnotationService(
            mock(FileWriter.class),
            mock(S3Service.class),
            repositoryMock,
            mock(ExportAreaPictureAnnotationPDFProcessor.class),
            mock(UserRepository.class),
            areaPictureServiceMock,
            prospectRepositoryMock);
  }

  private static AreaPicture areaPictureOf(String id, String idProspect) {
    return AreaPicture.builder().id(id).idProspect(idProspect).build();
  }

  @Test
  void find_all_draft_by_account_id_without_filters_does_not_restrict_area_pictures() {
    var idUser = randomUUID().toString();
    var criteriaCaptor = ArgumentCaptor.forClass(AreaPictureAnnotationCriteria.class);

    subject.findAllDraftByAccountId(
        idUser, null, null, null, null, new PageFromOne(1), new BoundedPageSize(10));

    verify(repositoryMock).findAllByCriteria(criteriaCaptor.capture());
    var actual = criteriaCaptor.getValue();
    assertEquals(idUser, actual.idUser());
    assertEquals(null, actual.idAreaPictureIds());
    assertEquals(true, actual.isDraft());
    assertEquals(0, actual.page());
    assertEquals(10, actual.pageSize());
  }

  @Test
  void find_all_draft_by_account_id_filters_by_address_narrows_to_matching_area_pictures() {
    var idUser = randomUUID().toString();
    var matchingAreaPicture = areaPictureOf("area_picture_1", "prospect_1");
    when(areaPictureServiceMock.findAllByAddress(idUser, "Paris"))
        .thenReturn(List.of(matchingAreaPicture));
    var criteriaCaptor = ArgumentCaptor.forClass(AreaPictureAnnotationCriteria.class);

    subject.findAllDraftByAccountId(
        idUser, null, "Paris", null, null, new PageFromOne(1), new BoundedPageSize(10));

    verify(repositoryMock).findAllByCriteria(criteriaCaptor.capture());
    assertEquals(List.of("area_picture_1"), criteriaCaptor.getValue().idAreaPictureIds());
  }

  @Test
  void find_all_draft_by_account_id_filters_by_prospect_name_across_all_area_pictures() {
    var idUser = randomUUID().toString();
    var matching = areaPictureOf("area_picture_1", "prospect_1");
    var notMatching = areaPictureOf("area_picture_2", "prospect_2");
    when(areaPictureServiceMock.findAllByIdUser(idUser)).thenReturn(List.of(matching, notMatching));
    when(prospectRepositoryMock.getById("prospect_1"))
        .thenReturn(Prospect.builder().name("John Doe").build());
    when(prospectRepositoryMock.getById("prospect_2"))
        .thenReturn(Prospect.builder().name("Jane Smith").build());
    var criteriaCaptor = ArgumentCaptor.forClass(AreaPictureAnnotationCriteria.class);

    subject.findAllDraftByAccountId(
        idUser, "John", null, null, null, new PageFromOne(1), new BoundedPageSize(10));

    verify(repositoryMock).findAllByCriteria(criteriaCaptor.capture());
    assertEquals(List.of("area_picture_1"), criteriaCaptor.getValue().idAreaPictureIds());
  }

  @Test
  void find_all_draft_by_account_id_returns_empty_without_querying_repository_when_no_match() {
    var idUser = randomUUID().toString();
    when(areaPictureServiceMock.findAllByAddress(idUser, "Unknown address")).thenReturn(List.of());

    var actual =
        subject.findAllDraftByAccountId(
            idUser,
            null,
            "Unknown address",
            null,
            null,
            new PageFromOne(1),
            new BoundedPageSize(10));

    assertEquals(List.of(), actual);
    verify(repositoryMock, never()).findAllByCriteria(any());
  }

  @Test
  void find_all_draft_by_account_id_passes_creation_date_range_through_to_criteria() {
    var idUser = randomUUID().toString();
    var creationFrom = Instant.parse("2024-01-08T01:00:00.00Z");
    var creationTo = Instant.parse("2024-01-08T02:00:00.00Z");
    var criteriaCaptor = ArgumentCaptor.forClass(AreaPictureAnnotationCriteria.class);

    subject.findAllDraftByAccountId(
        idUser, null, null, creationFrom, creationTo, new PageFromOne(1), new BoundedPageSize(10));

    verify(repositoryMock).findAllByCriteria(criteriaCaptor.capture());
    assertEquals(creationFrom, criteriaCaptor.getValue().creationFrom());
    assertEquals(creationTo, criteriaCaptor.getValue().creationTo());
  }
}
