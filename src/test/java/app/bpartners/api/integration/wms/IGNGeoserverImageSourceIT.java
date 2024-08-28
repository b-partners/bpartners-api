package app.bpartners.api.integration.wms;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.file.FileDownloaderImpl;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.AccountHolder;
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
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;

class IGNGeoserverImageSourceIT extends MockedThirdParties {
  @MockBean private FileDownloaderImpl fileDownloader;
  @MockBean private AreaPictureValidator areaPictureValidator;
  private IGNGeoserverImageSource subject;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    String baseUrl = "http://localhost:8080/wms";
    subject = new IGNGeoserverImageSource(baseUrl, fileDownloader, areaPictureValidator);
  }

  private @NotNull File getMockJpegFile() {
    FileSystemResource mockJpegResource =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/downloaded.jpeg").getFile());
    return mockJpegResource.getFile();
  }

  public AreaPicture geoserverAreaPicture() {
    return AreaPicture.builder()
        .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER).build())
        .zoomLevel(HOUSES_0)
        .currentGeoPosition(new GeoPosition().latitude(12.34).longitude(56.78))
        .build();
  }

  public AreaPicture ignAreaPicture() {
    return AreaPicture.builder()
        .currentLayer(AreaPictureMapLayer.builder().source(GEOSERVER_IGN).build())
        .currentGeoPosition(new GeoPosition().latitude(12.34).longitude(56.78))
        .zoomLevel(HOUSES_0)
        .currentTile(Tile.builder().arcgisZoom(ArcgisZoom.HOUSES_0).x(1).y(1).build())
        .build();
  }

  @Test
  void download_image_ok() {
    when(fileDownloader.getFromS3(any(), any())).thenReturn(getMockJpegFile());

    File result = subject.downloadImage(ignAreaPicture());

    assertNotNull(result);
    assertEquals("downloaded.jpeg", result.getName());
  }

  @Test
  void download_image_when_source_is_not_supported_ok() {
    var areaPicture = geoserverAreaPicture();
    ApiException thrown =
        assertThrows(ApiException.class, () -> subject.downloadImage(areaPicture));

    assertEquals(SERVER_EXCEPTION, thrown.getType());
    assertTrue(thrown.getMessage().contains("cannot download"));
  }

  @Test
  void get_uri_ok() {
    URI expectedUri = URI.create("http://localhost:8080/wms?zoom=20&lat=12.34&long=56.78");
    URI actualUri = subject.getURI(geoserverAreaPicture());

    assertEquals(expectedUri, actualUri);
  }

  @Test
  void download_image_with_account_holder_in_param_ok() {
    var actual =
        subject.downloadImage(ignAreaPicture(), AccountHolder.builder().id("ahId").build());

    assertNull(actual);
  }
}
