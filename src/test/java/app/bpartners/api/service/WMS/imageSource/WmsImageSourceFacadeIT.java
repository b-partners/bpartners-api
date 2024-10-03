package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.health.PingController;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.mail.Mailer;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.ArcgisZoom;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.RestTemplate;

class WmsImageSourceFacadeIT extends MockedThirdParties {
  private static final AreaPicture GEOSERVER_LAYER_AREA_PICTURE =
      AreaPicture.builder()
          .currentTile(Tile.builder().x(10).y(10).arcgisZoom(ArcgisZoom.HOUSES_0).build())
          .zoomLevel(HOUSES_0)
          .currentLayer(
              AreaPictureMapLayer.builder().source(GEOSERVER).name("area_picture").build())
          .build();
  @Autowired WmsImageSourceFacade subject;
  @MockBean RestTemplate restTemplateMock;
  @MockBean GeoserverImageSource geoserverImageSourceMock;
  @MockBean PingController pingControllerMock;
  @MockBean IGNGeoserverImageSource ignGeoserverImageSource;
  @MockBean Mailer mailer;
  @MockBean AuthProvider authProviderMock;

  private @NotNull File getMockJpegFile() {
    FileSystemResource mockJpegResource =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/downloaded.jpeg").getFile());
    File mockJpegFile = mockJpegResource.getFile();
    return mockJpegFile;
  }

  private @NotNull File getBlackJpegFile() {
    FileSystemResource mockJpegResource =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/black.jpeg").getFile());
    return mockJpegResource.getFile();
  }

  private @NotNull File getBlankJpegFile() {
    FileSystemResource mockJpegResource =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/blank_image.jpeg").getFile());
    File mockJpegFile = mockJpegResource.getFile();
    return mockJpegFile;
  }

  /**
   * setup a mocked endpoint which returns error (server or client error) and use its URI instead of
   * real URI to mimic RestTemplate behaviour in such case. The mocked endpoint is /ping
   *
   * @param geoserverImageSourceMock
   */
  private void setupGeoserverMock(GeoserverImageSource geoserverImageSourceMock) {
    when(pingControllerMock.ping()).thenThrow(new ApiException(SERVER_EXCEPTION, "server error"));
    when(geoserverImageSourceMock.getURI(any(), any()))
        .thenReturn(URI.create("http://localhost:" + localPort + "/ping"));
    when(geoserverImageSourceMock.downloadImage(any())).thenReturn(getBlankJpegFile());
    when(ignGeoserverImageSource.downloadImage(any())).thenReturn(getMockJpegFile());
  }

  @Test
  void downloadImage_cascade_on_server_error_ok() {
    setupGeoserverMock(geoserverImageSourceMock);

    File actual = subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(geoserverImageSourceMock, times(1)).downloadImage(any());
    verify(ignGeoserverImageSource, times(1)).downloadImage(any());
    assertEquals(getMockJpegFile(), actual);
  }

  @Test
  void downloadImage_cascade_on_blank_image_ok() {
    when(geoserverImageSourceMock.downloadImage(any())).thenReturn(getBlankJpegFile());
    when(ignGeoserverImageSource.downloadImage(any())).thenReturn(getMockJpegFile());
    File actual = subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(geoserverImageSourceMock, times(1)).downloadImage(any());
    verify(ignGeoserverImageSource, times(1)).downloadImage(any());
    assertEquals(getMockJpegFile(), actual);
  }

  @Test
  void downloadImage_cascade_on_black_image_ok() {
    when(geoserverImageSourceMock.downloadImage(any())).thenReturn(getBlackJpegFile());
    when(ignGeoserverImageSource.downloadImage(any())).thenReturn(getMockJpegFile());
    File actual = subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(geoserverImageSourceMock, times(1)).downloadImage(any());
    verify(ignGeoserverImageSource, times(1)).downloadImage(any());
    assertEquals(getMockJpegFile(), actual);
  }

  @Test
  void geoserver_download_image_is_null() {
    when(geoserverImageSourceMock.downloadImage(any())).thenReturn(null);
    when(ignGeoserverImageSource.downloadImage(any())).thenReturn(getMockJpegFile());

    File actual = subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(geoserverImageSourceMock, times(1)).downloadImage(any());
    verify(ignGeoserverImageSource, times(1)).downloadImage(any());
    assertEquals(getMockJpegFile(), actual);
  }

  @Test
  @Disabled
  void send_email_when_image_not_found() {
    when(geoserverImageSourceMock.downloadImage(any())).thenReturn(getBlankJpegFile());
    when(ignGeoserverImageSource.downloadImage(any())).thenReturn(getBlankJpegFile());

    try (MockedStatic<AuthProvider> mockedAuthProvider = Mockito.mockStatic(AuthProvider.class)) {
      mockedAuthProvider
          .when(AuthProvider::getAuthenticatedUser)
          .thenReturn(
              User.builder()
                  .firstName("dummy")
                  .lastName("dummy")
                  .email("dummy@gmail.com")
                  .id("userId")
                  .build());

      assertThrows(ApiException.class, () -> subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE));
      verify(mailer, times(1)).accept(any());
    }
  }
}
