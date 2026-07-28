package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.file.FileWriter;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFProcessor;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import app.bpartners.api.service.aws.S3Service;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AreaPictureAnnotationServiceTest extends MockedThirdParties {
  @Autowired AreaPictureAnnotationService subject;
  @MockBean FileWriter fileWriterMock;
  @MockBean S3Service s3ServiceMock;
  @MockBean AreaAnnotationPDFProcessor areaAnnotationPDFProcessorMock;
  @MockBean UserRepository userRepository;

  @Test
  void export_area_picture_annotation_ok() throws IOException {
    var exportAreaPictureAnnotationMock = mock(AreaAnnotationExportPayload.class);
    var expectedUrl = "https://s3.dummy.com";

    when(areaAnnotationPDFProcessorMock.process(any(), any())).thenReturn(new byte[] {1, 2, 3, 4});
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
}
