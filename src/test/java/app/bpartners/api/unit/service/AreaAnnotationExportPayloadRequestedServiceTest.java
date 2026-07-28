package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.ExportAreaPictureAnnotationRequested;
import app.bpartners.api.endpoint.rest.mapper.ExportAreaPictureAnnotationRestMapper;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFProcessor;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.ExportAreaPictureAnnotationRequestedService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.Duration;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AreaAnnotationExportPayloadRequestedServiceTest {
  S3Service s3ServiceMock = mock();
  SesService mailerMock = mock();
  FileWriter fileWriterMock = mock();
  UserService userServiceMock = mock();
  TemplateResolverEngine templateResolverEngineMock = mock();
  AreaAnnotationPDFProcessor areaAnnotationPDFProcessorMock = mock();
  ExportAreaPictureAnnotationRestMapper restMapperMock = mock();

  ExportAreaPictureAnnotationRequestedService subject =
      new ExportAreaPictureAnnotationRequestedService(
          mailerMock,
          s3ServiceMock,
          fileWriterMock,
          userServiceMock,
          templateResolverEngineMock,
          areaAnnotationPDFProcessorMock,
          restMapperMock);

  private static final String USER_ID = "userId";
  private static final String USER_MAIL = "user@gmail.com";
  private static final String ADDRESS = "Rue 102";

  @BeforeEach
  void setUp() {
    when(fileWriterMock.apply(any(), any())).thenReturn(mock());
    when(userServiceMock.getUserById(USER_ID))
        .thenReturn(User.builder().id(USER_ID).email(USER_MAIL).build());
    when(s3ServiceMock.presignURL(any(), any(), any(), any())).thenReturn("dummyUrl");
    when(s3ServiceMock.uploadFile(any(), any(), any(), any())).thenReturn(mock());
  }

  @SuppressWarnings("unchecked")
  @Test
  void export_area_picture_annotation_requested_ok() throws IOException, MessagingException {
    var exportAreaPictureAnnotationRequested = createExportAreaPictureAnnotationRequested();

    doNothing().when(mailerMock).sendEmail(any(), any(), any(), any());
    when(areaAnnotationPDFProcessorMock.process(any(), any())).thenReturn("".getBytes());
    when(templateResolverEngineMock.parseTemplateResolver(any(), any()))
        .thenReturn("<html><body>Rapport généré</body></html>");
    AreaAnnotationExportPayload domainAnnotation =
        AreaAnnotationExportPayload.builder()
            .address(ADDRESS)
            .imageUrl("url")
            .annotations(java.util.List.of())
            .build();
    when(restMapperMock.toDomain(
            any(app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class)))
        .thenReturn(domainAnnotation);

    subject.accept(exportAreaPictureAnnotationRequested);

    verify(mailerMock, times(1)).sendEmail(any(), any(), any(), any());
    assertEquals(exportAreaPictureAnnotationRequested.maxConsumerDuration(), Duration.ofMinutes(5));
    assertEquals(
        exportAreaPictureAnnotationRequested.maxConsumerBackoffBetweenRetries(),
        Duration.ofMinutes(1));
  }

  @SuppressWarnings("unchecked")
  @Test
  void export_area_picture_annotation_requested_ko() throws IOException, MessagingException {
    var exportAreaPictureAnnotationRequested = createExportAreaPictureAnnotationRequested();

    doNothing().when(mailerMock).sendEmail(any(), any(), any(), any());
    when(areaAnnotationPDFProcessorMock.process(any(), any())).thenThrow(IOException.class);
    when(templateResolverEngineMock.parseTemplateResolver(any(), any()))
        .thenReturn("<html><body>Rapport généré</body></html>");
    AreaAnnotationExportPayload domainAnnotation =
        AreaAnnotationExportPayload.builder()
            .address(ADDRESS)
            .imageUrl("url")
            .annotations(java.util.List.of())
            .build();
    when(restMapperMock.toDomain(
            any(app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class)))
        .thenReturn(domainAnnotation);

    subject.accept(exportAreaPictureAnnotationRequested);

    verify(mailerMock, times(1)).sendEmail(any(), any(), any(), any());
  }

  ExportAreaPictureAnnotationRequested createExportAreaPictureAnnotationRequested() {
    var annotation = mock(app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class);

    return ExportAreaPictureAnnotationRequested.builder()
        .annotation(annotation)
        .userId(USER_ID)
        .build();
  }
}
