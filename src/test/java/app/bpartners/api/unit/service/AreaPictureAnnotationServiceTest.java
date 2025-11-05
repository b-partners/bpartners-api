package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import app.bpartners.api.service.aws.S3Service;
import java.io.IOException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AreaPictureAnnotationServiceTest extends MockedThirdParties {
  @Autowired AreaPictureAnnotationService subject;
  @MockBean FileWriter fileWriterMock;
  @MockBean S3Service s3ServiceMock;
  @MockBean ExportAreaPictureAnnotationPDFProcessor exportAreaPictureAnnotationPDFProcessorMock;

  @Test
  @Disabled
  void export_area_picture_annotation_ok() throws IOException {
    var exportAreaPictureAnnotationMock = mock(ExportAreaPictureAnnotation.class);
    var expectedUrl = "https://s3.dummy.com";

    when(exportAreaPictureAnnotationPDFProcessorMock.process(any()))
        .thenReturn(new byte[] {1, 2, 3, 4});
    when(fileWriterMock.apply(any(), any())).thenReturn(mock());
    when(s3ServiceMock.uploadFile(any(), any(), any(), any())).thenReturn(mock());
    when(s3ServiceMock.presignURL(any(), any(), any(), any())).thenReturn(expectedUrl);

    var actual =
        subject.exportAreaPictureAnnotationToPdf(
            randomUUID().toString(), exportAreaPictureAnnotationMock);

    assertEquals(expectedUrl, actual.getValue());
  }
}
