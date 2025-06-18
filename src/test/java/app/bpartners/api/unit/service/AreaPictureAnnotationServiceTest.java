package app.bpartners.api.unit.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.ExportAreaPictureAnnotationRequested;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import java.util.List;
import org.junit.jupiter.api.Test;

class AreaPictureAnnotationServiceTest {
  EventProducer<ExportAreaPictureAnnotationRequested> eventProducerMock = mock();
  AreaPictureAnnotationService subject =
      new AreaPictureAnnotationService(
          mock(AreaPictureAnnotationRepository.class), eventProducerMock);

  @Test
  void export_area_picture_annotation_ok() {
    var userId = "userId";
    var exportAreaPictureAnnotationMock = mock(ExportAreaPictureAnnotation.class);
    var expected =
        ExportAreaPictureAnnotationRequested.builder()
            .annotation(exportAreaPictureAnnotationMock)
            .userId(userId)
            .build();

    doNothing().when(eventProducerMock).accept(any());

    var actual = subject.exportAreaPictureAnnotationToPdf(userId, exportAreaPictureAnnotationMock);

    assertEquals(expected.getAnnotation(), actual);
    verify(eventProducerMock, times(1)).accept(List.of(expected));
  }
}
