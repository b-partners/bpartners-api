package app.bpartners.api.unit.wms;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.file.FileDownloaderImpl;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.service.WMS.ArcgisZoom;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.WMS.imageSource.IGNGeoserverImageSource;
import java.io.File;
import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.FileSystemResource;

class IGNGeoserverImageSourceTest {
  @Mock private FileDownloaderImpl fileDownloader;
  @Mock private AreaPictureValidator areaPictureValidator;
  private IGNGeoserverImageSource subject;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    String baseUrl = "http://localhost:8080/wms";
    subject = new IGNGeoserverImageSource(baseUrl, fileDownloader, areaPictureValidator);
  }

  @Test
  void testSupportsWhenSupported() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER_IGN).build())
            .build();

    assertTrue(subject.supports(areaPicture));
  }

  @Test
  void testSupportsWhenNotSupported() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER).build())
            .build();

    assertFalse(subject.supports(areaPicture));
  }

  private @NotNull File getMockJpegFile() {
    FileSystemResource mockJpegResource =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/downloaded.jpeg").getFile());
    File mockJpegFile = mockJpegResource.getFile();
    return mockJpegFile;
  }

  @Test
  void testDownloadImage() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER_IGN).build())
            .currentGeoPosition(new GeoPosition().latitude(12.34).longitude(56.78))
            .zoomLevel(HOUSES_0)
            .currentTile(Tile.builder().arcgisZoom(ArcgisZoom.HOUSES_0).x(1).y(1).build())
            .build();

    when(fileDownloader.getFromS3(any(), any())).thenReturn(getMockJpegFile());
    File result = subject.downloadImage(areaPicture);

    assertNotNull(result);
    assertEquals("downloaded.jpeg", result.getName());
  }

  @Test
  void testDownloadImageWhenNotSupported() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER).build())
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
  void testGetURI() {
    AreaPicture areaPicture =
        AreaPicture.builder()
            .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER).build())
            .zoomLevel(HOUSES_0)
            .currentGeoPosition(new GeoPosition().latitude(12.34).longitude(56.78))
            .build();

    URI expectedUri = URI.create("http://localhost:8080/wms?zoom=20&lat=12.34&long=56.78");
    URI actualUri = subject.getURI(areaPicture);

    assertEquals(expectedUri, actualUri);
  }
}
