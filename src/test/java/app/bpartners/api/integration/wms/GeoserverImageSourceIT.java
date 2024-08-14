package app.bpartners.api.integration.wms;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.file.FileDownloaderImpl;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.ArcgisZoom;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.imageSource.GeoserverImageSource;
import app.bpartners.api.service.converter.XYZToBoundingBox;
import java.io.File;
import java.math.BigDecimal;
import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;

class GeoserverImageSourceIT extends MockedThirdParties {
  @MockBean private FileDownloaderImpl fileDownloader;
  @MockBean private XYZToBoundingBox xyzToBoundingBox;
  private GeoserverImageSource subject;

  private @NotNull File getMockJpegFile() {
    FileSystemResource mockJpegResource =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/downloaded.jpeg").getFile());
    return mockJpegResource.getFile();
  }

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    String baseUrl = "http://localhost:8080/wms";
    subject = new GeoserverImageSource(fileDownloader, baseUrl, xyzToBoundingBox);
  }

  @Test
  void source_is_supported_ok() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER).build())
            .build();

    assertTrue(subject.supports(areaPicture));
  }

  @Test
  void dowload_image_ok() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().name("layerName").source(GEOSERVER).build())
            .currentGeoPosition(new GeoPosition().latitude(12.34).longitude(56.78))
            .zoomLevel(HOUSES_0)
            .currentTile(Tile.builder().arcgisZoom(ArcgisZoom.HOUSES_0).x(1).y(1).build())
            .build();
    when(xyzToBoundingBox.apply(any()))
        .thenReturn(
            new XYZToBoundingBox.BBOX(
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1)));
    when(fileDownloader.get(any(), any())).thenReturn(getMockJpegFile());

    File result = subject.downloadImage(areaPicture);

    assertNotNull(result);
    assertEquals("downloaded.jpeg", result.getName());
  }

  @Test
  void download_image_on_unsupported_source() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER_IGN).build())
            .build();

    ApiException thrown =
        assertThrows(
            ApiException.class,
            () -> {
              subject.downloadImage(areaPicture);
            });

    assertEquals(SERVER_EXCEPTION, thrown.getType());
    assertTrue(thrown.getMessage().contains("cannot download"));
  }

  @Test
  void get_uri_ok() {
    Tile tile = Tile.builder().y(1).x(1).arcgisZoom(ArcgisZoom.HOUSES_0).build();
    AreaPictureMapLayer areaPictureMapLayer =
        AreaPictureMapLayer.builder().name("layerName").build();

    when(xyzToBoundingBox.apply(any()))
        .thenReturn(
            new XYZToBoundingBox.BBOX(
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1),
                BigDecimal.valueOf(1)));
    URI actualUri = subject.getURI(tile, areaPictureMapLayer);
    String expectedUri =
        "http://localhost:8080/wms?layers=layerName&format=image/jpeg&width=1024&height=1024&bbox=1,%201,%201,%201&SRS=EPSG:3857&transparent=true&service=WMS&request=GetMap";

    assertEquals(expectedUri, actualUri.toString());
  }
}
