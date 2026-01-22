package app.bpartners.api.integration.wms;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.model.GeoPosition;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.file.FileDownloaderImpl;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.validator.AreaPictureValidator;
import app.bpartners.api.service.wms.ArcgisZoom;
import app.bpartners.api.service.wms.Tile;
import app.bpartners.api.service.wms.imageSource.AirbusPNEOImageSource;
import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

@Disabled("Run locally")
class AirbusImageSourceIT extends MockedThirdParties {
  @MockBean private FileDownloaderImpl fileDownloader;
  @MockBean private AreaPictureValidator areaPictureValidator;
  private RestTemplate restTemplate = new RestTemplate();
  private AirbusPNEOImageSource subject =
      new AirbusPNEOImageSource(
          System.getenv("AIRBUS_SEARCHAPI_BASEURL"),
          System.getenv("AIRBUS_AUTHENTICATION_BASEURL"),
          System.getenv("AIRBUS_API_KEY"),
          fileDownloader,
          areaPictureValidator,
          restTemplate);

  private AreaPicture toulouseAreaPicture() {
    return AreaPicture.builder()
        .currentTile(Tile.builder().x(264242).y(191449).arcgisZoom(ArcgisZoom.BUILDING).build())
        .currentGeoPosition(
            new GeoPosition().latitude(43.599621309901735).longitude(1.4410986644024693))
        .zoomLevel(ZoomLevel.BUILDING)
        .currentLayer(
            AreaPictureMapLayer.builder().name("TOULOUSE_BUILDING_19_264242_191449").build())
        .build();
  }

  @Test
  void download_image_from_airbus_source() {
    File file = subject.downloadImage(toulouseAreaPicture());

    assertNotNull(file);
  }

  @ParameterizedTest(name = "Download image for {0}")
  @MethodSource("cityTileProvider")
  void download_image_from_airbus_for_cities(CityTileTestCase city) {
    AreaPicture areaPicture = buildAreaPictureFromXYZ(city);

    File file = subject.downloadImage(areaPicture);

    assertNotNull(file, "Image should not be null for " + city.city());
    assertTrue(file.exists(), "File should exist for " + city.city());
  }

  private AreaPicture buildAreaPictureFromXYZ(CityTileTestCase city) {
    String ref = city.city();
    String[] parts = city.xyzTile().replace(".jpg", "").split("_");
    int x = Integer.parseInt(parts[1]);
    int y = Integer.parseInt(parts[2]);

    Tile tile = Tile.builder().x(x).y(y).arcgisZoom(ArcgisZoom.BUILDING).build();

    return AreaPicture.builder()
        .currentTile(tile)
        .currentGeoPosition(new GeoPosition().latitude(city.latitude()).longitude(city.longitude()))
        .zoomLevel(ZoomLevel.BUILDING)
        .currentLayer(
            AreaPictureMapLayer.builder().name("BUILDING_" + ref + "_" + city.xyzTile()).build())
        .build();
  }

  static Stream<CityTileTestCase> cityTileProvider() {
    return Stream.of(
        //        new CityTileTestCase("Dijon", 47.341749, 5.020057, "19_269454_183673.jpg"), //
        // failed
        new CityTileTestCase("Mans", 48.012534, 0.173570, "19_262396_182222.jpg"),
        new CityTileTestCase("Mans", 48.018972, 0.179513, "19_262405_182208.jpg"),
        new CityTileTestCase("Paris", 48.8566, 2.3522, "19_265569_180369.jpg"),
        new CityTileTestCase("Nantes", 47.2184, -1.5536, "19_259881_183938.jpg"),
        new CityTileTestCase("Lyon", 45.7640, 4.8357, "19_269186_187015.jpg"),
        new CityTileTestCase("Lille", 50.6292, 3.0573, "19_266596_176374.jpg"),
        new CityTileTestCase("Bordeaux", 44.8378, -0.5792, "19_261300_188933.jpg"),
        new CityTileTestCase("Marseille", 43.2965, 5.3698, "19_269964_192057.jpg"),
        new CityTileTestCase("Strasbourg", 48.5734, 7.7521, "19_273433_180995.jpg"),
        //        new CityTileTestCase("Montpellier", 44.1194, 3.2319, "19_266850_190399.jpg"), //
        // failed
        new CityTileTestCase("Caen", 49.4431, 1.0993, "19_263744_179064.jpg"),
        new CityTileTestCase("Grenoble", 45.1885, 5.7245, "19_270480_188210.jpg"),
        new CityTileTestCase("Nîmes", 43.9352, 4.1023, "19_268118_190772.jpg"),
        new CityTileTestCase("Perpignan", 42.6977, 2.8956, "19_266361_193249.jpg"),
        new CityTileTestCase("Annecy", 46.2044, 6.1432, "19_271090_186092.jpg"),
        ////        new CityTileTestCase("Cahors", 44.0140, 1.7043, "19_264626_190613.jpg"), //
        // failed
        new CityTileTestCase("Calais", 50.9513, 1.8587, "19_264850_175632.jpg"),
        new CityTileTestCase("Rennes", 48.1173, -1.6778, "19_259700_181994.jpg"),
        new CityTileTestCase("Orléans", 47.9029, 1.9093, "19_264924_182461.jpg"),
        new CityTileTestCase("Angers", 47.4784, -0.5632, "19_261323_183379.jpg"),
        new CityTileTestCase("Bayeux", 49.1829, -0.3700, "19_261605_179645.jpg"),
        new CityTileTestCase("Nice", 43.7102, 7.2620, "19_272720_191226.jpg"));
  }
}
