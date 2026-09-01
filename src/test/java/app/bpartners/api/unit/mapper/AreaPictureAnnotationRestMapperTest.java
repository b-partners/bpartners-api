package app.bpartners.api.unit.mapper;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureAnnotationInstanceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.AreaPictureAnnotationRestMapper;
import app.bpartners.api.endpoint.rest.mapper.AreaPictureRestMapper;
import app.bpartners.api.endpoint.rest.mapper.ProspectRestMapper;
import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.service.areapicture.AreaPictureService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AreaPictureAnnotationRestMapperTest {
  AreaPictureService areaPictureServiceMock;
  ProspectRepository prospectRepositoryMock;
  AreaPictureRestMapper areaPictureRestMapperMock;
  AreaPictureAnnotationRestMapper subject;

  @BeforeEach
  void setUp() {
    areaPictureServiceMock = mock(AreaPictureService.class);
    prospectRepositoryMock = mock(ProspectRepository.class);
    areaPictureRestMapperMock = mock(AreaPictureRestMapper.class);
    subject =
        new AreaPictureAnnotationRestMapper(
            mock(AreaPictureAnnotationInstanceRestMapper.class),
            areaPictureRestMapperMock,
            areaPictureServiceMock,
            prospectRepositoryMock,
            mock(ProspectRestMapper.class));

    when(areaPictureRestMapperMock.toRest(any(AreaPicture.class)))
        .thenReturn(new AreaPictureDetails());
  }

  private static AreaPictureAnnotation annotationOf(String id, String idAreaPicture) {
    return AreaPictureAnnotation.builder()
        .id(id)
        .idAreaPicture(idAreaPicture)
        .isDraft(true)
        .annotationInstances(List.of())
        .build();
  }

  @Test
  void to_rest_drafts_fetches_each_distinct_area_picture_only_once() {
    var userId = randomUUID().toString();
    var areaPicture1 = AreaPicture.builder().id("area_picture_1").idProspect("prospect_1").build();
    var areaPicture2 = AreaPicture.builder().id("area_picture_2").idProspect("prospect_2").build();
    when(areaPictureServiceMock.findBy(userId, "area_picture_1")).thenReturn(areaPicture1);
    when(areaPictureServiceMock.findBy(userId, "area_picture_2")).thenReturn(areaPicture2);
    when(prospectRepositoryMock.findAllByIds(any()))
        .thenReturn(
            List.of(
                Prospect.builder().id("prospect_1").name("John Doe").build(),
                Prospect.builder().id("prospect_2").name("Jane Smith").build()));
    var annotations =
        List.of(
            annotationOf("annotation_1", "area_picture_1"),
            annotationOf("annotation_2", "area_picture_1"),
            annotationOf("annotation_3", "area_picture_2"));

    var actual = subject.toRestDrafts(userId, annotations);

    verify(areaPictureServiceMock, times(1)).findBy(userId, "area_picture_1");
    verify(areaPictureServiceMock, times(1)).findBy(userId, "area_picture_2");
    verify(prospectRepositoryMock, never()).getById(anyString());
    assertEquals(3, actual.size());
    assertEquals("John Doe", actual.get(0).getProspectName());
    assertEquals("John Doe", actual.get(1).getProspectName());
    assertEquals("Jane Smith", actual.get(2).getProspectName());
  }

  @Test
  void to_rest_drafts_resolves_prospects_with_a_single_batched_call() {
    var userId = randomUUID().toString();
    var areaPicture = AreaPicture.builder().id("area_picture_1").idProspect("prospect_1").build();
    when(areaPictureServiceMock.findBy(userId, "area_picture_1")).thenReturn(areaPicture);
    when(prospectRepositoryMock.findAllByIds(List.of("prospect_1")))
        .thenReturn(List.of(Prospect.builder().id("prospect_1").name("John Doe").build()));
    var annotations = List.of(annotationOf("annotation_1", "area_picture_1"));

    var actual = subject.toRestDrafts(userId, annotations);

    verify(prospectRepositoryMock, times(1)).findAllByIds(List.of("prospect_1"));
    verify(prospectRepositoryMock, never()).getById(anyString());
    assertEquals("John Doe", actual.get(0).getProspectName());
  }

  @Test
  void to_rest_drafts_handles_area_picture_without_prospect() {
    var userId = randomUUID().toString();
    var areaPicture = AreaPicture.builder().id("area_picture_1").idProspect(null).build();
    when(areaPictureServiceMock.findBy(userId, "area_picture_1")).thenReturn(areaPicture);
    var annotations = List.of(annotationOf("annotation_1", "area_picture_1"));

    var actual = subject.toRestDrafts(userId, annotations);

    verify(prospectRepositoryMock).findAllByIds(List.of());
    assertEquals(1, actual.size());
    assertEquals(null, actual.get(0).getProspectName());
  }
}
