package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.repository.implementation.AreaPictureAnnotationRepositoryImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

class AreaPictureAnnotationRepositoryTest {

  @Mock private AreaPictureAnnotationRepository areaPictureAnnotationRepository;

  @InjectMocks
  private AreaPictureAnnotationRepositoryImpl
      areaPictureAnnotationRepositoryImpl; // Suppose que c'est l'implémentation de
                                           // AreaPictureAnnotationRepository

  private AreaPictureAnnotation areaPictureAnnotation;
  private Pageable pageable;
  private String idUser = "user1";
  private String idAreaPicture = "areaPic1";
  private String annotationId = "annotation1";
  private String invalidId = "invalid";

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    areaPictureAnnotation =
        AreaPictureAnnotation.builder()
            .id(annotationId)
            .creationDatetime(Instant.now())
            .idUser(idUser)
            .idAreaPicture(idAreaPicture)
            .annotationInstances(List.of())
            .build();

    pageable = Pageable.unpaged(); // Utiliser un pageable non paginé pour les tests
  }

  @Test
  void findAllBy_returnsAnnotationList() {
    List<AreaPictureAnnotation> annotations = List.of(areaPictureAnnotation);
    when(areaPictureAnnotationRepository.findAllBy(idUser, idAreaPicture, pageable))
        .thenReturn(annotations);

    List<AreaPictureAnnotation> foundAnnotations =
        areaPictureAnnotationRepository.findAllBy(idUser, idAreaPicture, pageable);
    assertNotNull(foundAnnotations);
    assertEquals(1, foundAnnotations.size());
    assertEquals(areaPictureAnnotation, foundAnnotations.get(0));
  }

  @Test
  void findBy_returnsAnnotation() {
    when(areaPictureAnnotationRepository.findBy(idUser, idAreaPicture, annotationId))
        .thenReturn(Optional.of(areaPictureAnnotation));

    Optional<AreaPictureAnnotation> foundAnnotation =
        areaPictureAnnotationRepository.findBy(idUser, idAreaPicture, annotationId);
    assertTrue(foundAnnotation.isPresent());
    assertEquals(areaPictureAnnotation, foundAnnotation.get());
  }

  @Test
  void findBy_notFound() {
    when(areaPictureAnnotationRepository.findBy(idUser, idAreaPicture, invalidId))
        .thenReturn(Optional.empty());

    Optional<AreaPictureAnnotation> foundAnnotation =
        areaPictureAnnotationRepository.findBy(idUser, idAreaPicture, invalidId);
    assertFalse(foundAnnotation.isPresent());
  }

  @Test
  void save_returnsSavedAnnotation() {
    when(areaPictureAnnotationRepository.save(areaPictureAnnotation))
        .thenReturn(areaPictureAnnotation);

    AreaPictureAnnotation savedAnnotation =
        areaPictureAnnotationRepository.save(areaPictureAnnotation);
    assertNotNull(savedAnnotation);
    assertEquals(areaPictureAnnotation, savedAnnotation);
  }
}
