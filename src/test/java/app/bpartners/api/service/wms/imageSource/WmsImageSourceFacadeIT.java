package app.bpartners.api.service.wms.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.health.PingController;
import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.mail.Mailer;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.wms.ArcgisZoom;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import app.bpartners.api.service.wms.Tile;
import app.bpartners.api.service.wms.imageSource.exception.BlankImageException;
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

public class WmsImageSourceFacadeIT extends MockedThirdParties {
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
  @MockBean TileExtenderImageSource tileExtenderImageSource;
  @MockBean AreaPictureMapLayerService areaPictureMapLayerServiceMock;

  public static AreaPictureMapLayer aerialPhotographyLayer() {
    return AreaPictureMapLayer.builder()
        .id("2f343dba-dd5f-4895-9006-49472f576c02")
        .name("cite:PHOTO_AERIENNE")
        .source(GEOSERVER)
        .year(2024)
        .maximumZoomLevel(HOUSES_0)
        .precisionLevelInCm(20)
        .departementName("ALL")
        .build();
  }

  public static AreaPictureMapLayer pcrsLayer() {
    return AreaPictureMapLayer.builder()
        .id("726f5b3b-d23b-40c3-b38e-68a43d7ae155")
        .departementName("ALL")
        .year(2024)
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .name("cite:PCRS.LAMB93")
        .source(GEOSERVER)
        .build();
  }

  public static AreaPictureMapLayer ignLayer() {
    return AreaPictureMapLayer.builder()
        .name("ORTHOPHOTOS.ORTHOPHOTOIMAGERY")
        .source(GEOSERVER_IGN)
        .build();
  }

  private AreaPictureMapLayer dijon() {
    return AreaPictureMapLayer.builder().name("cite:Dijon").source(GEOSERVER).build();
  }

  private AreaPicture anAreaPicture(AreaPictureMapLayer areaPictureMapLayer) {
    return AreaPicture.builder()
        .currentLayer(areaPictureMapLayer)
        .currentGeoPosition(new GeoPosition().latitude(12.34).longitude(56.78))
        .zoomLevel(HOUSES_0)
        .currentLayer(AreaPictureMapLayer.builder().name("cite:Dijon").build())
        .currentTile(Tile.builder().arcgisZoom(ArcgisZoom.HOUSES_0).x(1).y(1).build())
        .build();
  }

  private @NotNull File getMockJpegFile() {
    FileSystemResource mockJpegResource =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/downloaded.jpeg").getFile());
    File mockJpegFile = mockJpegResource.getFile();
    return mockJpegFile;
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
    when(tileExtenderImageSource.downloadImage(any())).thenReturn(getMockJpegFile());
  }

  @Test
  void download_image_with_pcrs_layer_on_cascade_ok() {
    when(areaPictureMapLayerServiceMock.getPCRSLayer()).thenReturn(pcrsLayer());
    when(tileExtenderImageSource.downloadImage(any(AreaPicture.class)))
        .thenThrow(new BlankImageException("Blank image"));
    when(tileExtenderImageSource.downloadImage(
            argThat(area -> area.getCurrentLayer().equals(pcrsLayer()))))
        .thenReturn(getMockJpegFile());

    subject.downloadImage(anAreaPicture(dijon()));

    verify(tileExtenderImageSource, times(2)).downloadImage(any());
    verify(areaPictureMapLayerServiceMock, times(1)).getPCRSLayer();
  }

  @Test
  @Disabled("PHOTO_AERIENNE layer has been removed as default layer")
  void download_image_with_aerial_photography_layer_on_cascade_ok() {
    AreaPicture dijon = anAreaPicture(dijon());

    when(areaPictureMapLayerServiceMock.getPCRSLayer()).thenReturn(pcrsLayer());
    when(tileExtenderImageSource.downloadImage(any(AreaPicture.class)))
        .thenThrow(new BlankImageException("Blank image"));
    when(tileExtenderImageSource.downloadImage(
            argThat(area -> area.getCurrentLayer().equals(aerialPhotographyLayer()))))
        .thenReturn(getMockJpegFile());

    subject.downloadImage(dijon);

    verify(tileExtenderImageSource, times(3)).downloadImage(any());
    verify(areaPictureMapLayerServiceMock, times(1)).getAerialPhotography();
  }

  @Test
  void download_image_with_ign_layer_on_cascade_ok() {
    when(areaPictureMapLayerServiceMock.getPCRSLayer()).thenReturn(pcrsLayer());
    when(areaPictureMapLayerServiceMock.getDefaultIGNLayer()).thenReturn(ignLayer());
    when(tileExtenderImageSource.downloadImage(any(AreaPicture.class)))
        .thenThrow(new BlankImageException("Blank image"));
    when(tileExtenderImageSource.downloadImage(
            argThat(area -> area.getCurrentLayer().equals(ignLayer()))))
        .thenReturn(getMockJpegFile());

    subject.downloadImage(anAreaPicture(dijon()));

    verify(tileExtenderImageSource, times(3)).downloadImage(any());
    verify(areaPictureMapLayerServiceMock, times(1)).getDefaultIGNLayer();
  }

  @Test
  void downloadImage_cascade_on_server_error_ok() {
    setupGeoserverMock(geoserverImageSourceMock);

    File actual = subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(tileExtenderImageSource, times(1)).downloadImage(any());
    assertEquals(getMockJpegFile(), actual);
  }

  @Test
  void downloadImage_cascade_on_blank_image_ok() {
    when(tileExtenderImageSource.downloadImage(any())).thenReturn(getMockJpegFile());
    File actual = subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(tileExtenderImageSource, times(1)).downloadImage(any());
    assertEquals(getMockJpegFile(), actual);
  }

  @Test
  @Disabled
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
    when(tileExtenderImageSource.downloadImage(any())).thenReturn(getBlankJpegFile());

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
