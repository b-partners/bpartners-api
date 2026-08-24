package app.bpartners.api.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.mapper.AreaPictureAnnotationRestMapper;
import app.bpartners.api.endpoint.rest.model.ConverterAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.ConverterValidator;
import app.bpartners.api.service.annotation.AreaPictureAnnotationConverter;
import app.bpartners.api.service.areapicture.AreaPictureAnnotationService;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.web.multipart.MultipartFile;

class AreaPictureAnnotationControllerTest {
  AreaPictureAnnotationService serviceMock = mock();
  AreaPictureAnnotationRestMapper mapperMock = mock();
  AreaPictureAnnotationConverter areaPictureAnnotationConverterMock = mock();
  ConverterValidator converterValidatorMock = mock();

  AreaPictureAnnotationController subject =
      new AreaPictureAnnotationController(
          serviceMock, mapperMock, areaPictureAnnotationConverterMock, converterValidatorMock);

  @Test
  void export_area_picture_annotation_to_pdf_with_image_delegates_to_service() throws IOException {
    var accountId = randomUUID().toString();
    var userId = randomUUID().toString();
    var annotation = mock(ExportAreaPictureAnnotation.class);
    var globalImage3D = mock(MultipartFile.class);
    var imageBytes = new byte[] {1, 2, 3};
    var expected = mock(PreSignedURL.class);

    when(globalImage3D.getBytes()).thenReturn(imageBytes);
    when(serviceMock.exportAreaPictureAnnotationToPdf(userId, annotation, imageBytes))
        .thenReturn(expected);

    try (MockedStatic<AuthProvider> authProviderMockedStatic = mockStatic(AuthProvider.class)) {
      authProviderMockedStatic.when(AuthProvider::getAuthenticatedUserId).thenReturn(userId);

      var actual = subject.exportAreaPictureAnnotationToPdf(accountId, annotation, globalImage3D);

      assertEquals(expected, actual);
    }
  }

  @Test
  void export_area_picture_annotation_to_pdf_without_image_delegates_to_service()
      throws IOException {
    var accountId = randomUUID().toString();
    var userId = randomUUID().toString();
    var annotation = mock(ExportAreaPictureAnnotation.class);
    var expected = mock(PreSignedURL.class);

    when(serviceMock.exportAreaPictureAnnotationToPdf(userId, annotation, null))
        .thenReturn(expected);

    try (MockedStatic<AuthProvider> authProviderMockedStatic = mockStatic(AuthProvider.class)) {
      authProviderMockedStatic.when(AuthProvider::getAuthenticatedUserId).thenReturn(userId);

      var actual = subject.exportAreaPictureAnnotationToPdf(accountId, annotation, null);

      assertEquals(expected, actual);
    }
  }

  @Test
  void convert_lat_lon_polygon_to_pixel_validates_then_delegates_to_converter() {
    var accountId = randomUUID().toString();
    Map<String, ConverterAnnotation> converterAnnotationMap =
        Map.of("key", mock(ConverterAnnotation.class));
    Map<String, ConverterAnnotation> expected = Map.of("key", mock(ConverterAnnotation.class));
    doNothing().when(converterValidatorMock).accept(converterAnnotationMap);
    when(areaPictureAnnotationConverterMock.toPixel(converterAnnotationMap)).thenReturn(expected);

    var actual = subject.convertLatLonPolygonToPixel(accountId, converterAnnotationMap);

    verify(converterValidatorMock).accept(converterAnnotationMap);
    assertEquals(expected, actual);
  }

  @Test
  void convert_pixel_to_lat_lon_validates_then_delegates_to_converter() {
    var accountId = randomUUID().toString();
    Map<String, ConverterAnnotation> converterAnnotationMap =
        Map.of("key", mock(ConverterAnnotation.class));
    Map<String, ConverterAnnotation> expected = Map.of("key", mock(ConverterAnnotation.class));
    doNothing().when(converterValidatorMock).accept(converterAnnotationMap);
    when(areaPictureAnnotationConverterMock.toLatLong(converterAnnotationMap)).thenReturn(expected);

    var actual = subject.convertPixelToLatLon(accountId, converterAnnotationMap);

    verify(converterValidatorMock).accept(converterAnnotationMap);
    assertEquals(expected, actual);
  }
}
