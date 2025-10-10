package app.bpartners.api.unit.service;

import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AreaPictureAnnotationServiceTest {
  ExportAreaPictureAnnotationPDFProcessor exportAreaPictureAnnotationPDFProcessorMock = mock();
  AreaPictureAnnotationService subject =
      new AreaPictureAnnotationService(
          mock(AreaPictureAnnotationRepository.class), exportAreaPictureAnnotationPDFProcessorMock);

  @Test
  @SneakyThrows
  void export_area_picture_annotation_ok() {
    var exportAreaPictureAnnotationMock = mock(ExportAreaPictureAnnotation.class);
    var expected = new byte[1];

    when(exportAreaPictureAnnotationPDFProcessorMock.process(any())).thenReturn(expected);

    var actual = subject.exportAreaPictureAnnotationToPdf(exportAreaPictureAnnotationMock);

    Assertions.assertEquals(expected, actual);
  }
}
