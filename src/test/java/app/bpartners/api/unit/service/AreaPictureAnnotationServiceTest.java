package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.annotation.AnnotationReportService;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import app.bpartners.api.service.aws.S3Service;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AreaPictureAnnotationServiceTest extends MockedThirdParties {
  @Autowired AreaPictureAnnotationService subject;
  @MockBean FileWriter fileWriterMock;
  @MockBean S3Service s3ServiceMock;
  @MockBean AnnotationReportService annotationReportServiceMock;
  @MockBean UserRepository userRepository;

  @Test
  void export_area_picture_annotation_ok() {
    var exportAreaPictureAnnotationMock = mock(ExportAreaPictureAnnotation.class);
    var expectedUrl = "https://s3.dummy.com";

    when(annotationReportServiceMock.process(any(), any())).thenReturn(new byte[] {1, 2, 3, 4});
    when(fileWriterMock.apply(any(), any())).thenReturn(mock());
    when(s3ServiceMock.uploadFile(any(), any(), any(), any())).thenReturn(mock());
    when(s3ServiceMock.presignURL(any(), any(), any(), any())).thenReturn(expectedUrl);
    when(userRepository.getById(anyString()))
        .thenReturn(
            User.builder()
                .id(randomUUID().toString())
                .logoFileId(randomUUID().toString())
                .firstName("John")
                .lastName("Doe")
                .email("john@mail.com")
                .mobilePhoneNumber("0000000000")
                .build());

    var actual =
        subject.exportAreaPictureAnnotationToPdf(
            randomUUID().toString(), exportAreaPictureAnnotationMock, new byte[] {1, 2, 3, 4});

    assertEquals(expectedUrl, actual.getValue());
  }

  @Test
  void export_area_picture_annotation_ko() {
    var exportAreaPictureAnnotationMock = mock(ExportAreaPictureAnnotation.class);
    var userId = randomUUID().toString();
    var errorMessage = "unable to process image";

    when(userRepository.getById(anyString()))
        .thenReturn(
            User.builder()
                .id(randomUUID().toString())
                .logoFileId(randomUUID().toString())
                .firstName("John")
                .lastName("Doe")
                .email("john@mail.com")
                .mobilePhoneNumber("0000000000")
                .build());
    when(annotationReportServiceMock.process(any(), any(), any()))
        .thenThrow(new IllegalStateException(errorMessage));

    var actualException =
        assertThrows(
            RuntimeException.class,
            () ->
                subject.exportAreaPictureAnnotationToPdf(
                    userId, exportAreaPictureAnnotationMock, new byte[] {1, 2, 3, 4}));

    assertEquals(errorMessage, actualException.getMessage());
    verifyNoInteractions(fileWriterMock, s3ServiceMock);
  }
}
