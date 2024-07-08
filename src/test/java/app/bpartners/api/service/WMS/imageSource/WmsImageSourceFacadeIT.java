package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.controller.health.PingController;
import app.bpartners.api.file.FileDownloaderImpl;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.ArcgisZoom;
import app.bpartners.api.service.WMS.Tile;
import java.io.File;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.client.RestTemplate;

@Slf4j
class WmsImageSourceFacadeIT extends MockedThirdParties {
  private static final AreaPicture GEOSERVER_LAYER_AREA_PICTURE =
      AreaPicture.builder()
          .currentTile(Tile.builder().x(538340).y(374051).arcgisZoom(ArcgisZoom.HOUSES_0).build())
          .zoomLevel(HOUSES_0)
          .currentLayer(
              AreaPictureMapLayer.builder().source(GEOSERVER).name("area_picture").build())
          .build();
  @Autowired WmsImageSourceFacade subject;
  @MockBean RestTemplate restTemplateMock;
  @Autowired GeoserverImageSource geoserverImageSourceMock;
  @MockBean PingController pingControllerMock;
  @MockBean FileDownloaderImpl fileDownloaderImpl;

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

  private void setupGeoserverMock() {
    when(pingControllerMock.ping()).thenThrow(new ApiException(SERVER_EXCEPTION, "server error"));
    when(fileDownloaderImpl.get(
            "area_picture_HOUSES_0_538340_374051",
            URI.create(
                "http://localhost.com?layers=area_picture&zoom=20&x=538340&y=374051&format=image/jpeg")))
        .thenThrow(ApiException.class);
    when(fileDownloaderImpl.get(
            "area_picture_HOUSES_0_538340_374051",
            URI.create(
                "http://localhost.com?layers=FLUX_IGN_2023_20CM&zoom=20&x=538340&y=374051&format=image/jpeg")))
        .thenReturn(getMockJpegFile());
  }

  @Test
  void downloadImage_cascade_on_server_error_ok() {
    setupGeoserverMock();

    subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(fileDownloaderImpl, times(2)).get(any(), any());
  }

  @Test
  @Disabled("blank image is considered normal for now")
  void downloadImage_cascade_on_blank_image_ok() {
    when(geoserverImageSourceMock.downloadImage(any())).thenReturn(getBlankJpegFile());

    File actual = subject.downloadImage(GEOSERVER_LAYER_AREA_PICTURE);

    verify(geoserverImageSourceMock, times(2)).downloadImage(any());
    assertEquals(getMockJpegFile(), actual);
  }
}
