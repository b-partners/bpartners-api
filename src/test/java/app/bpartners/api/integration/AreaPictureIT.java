package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.OPENSTREETMAP;
import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.integration.conf.utils.TestUtils.DEFAULT_FRANCE_LAYER;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.PROSPECT_1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.AreaPictureApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.AreaPictureMapLayer;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.api.endpoint.rest.model.Zoom;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.WMS.ArcgisZoom;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.utils.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
public class AreaPictureIT extends S3MockedThirdParties {
  @Deprecated public static final OpenStreetMapLayer DEFAULT_OSM_LAYER = TOUS_FR;
  @Autowired ObjectMapper om;

  @Autowired AreaPictureMapLayerService mapLayerService;
  @MockBean BanApi banApiMock;

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  public static final String AREA_PICTURE_1_ID = "area_picture_1_id";
  public static final String AREA_PICTURE_2_ID = "area_picture_2_id";

  static AreaPictureMapLayer tousFrLayer() {
    return new AreaPictureMapLayer()
        .id("2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8")
        .name("tous_fr")
        .year(0)
        .precisionLevelInCm(20)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(OPENSTREETMAP);
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainOsmLayer() {
    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("2cb589c1-45b0-4cb8-b84e-f1ed40e97bd8")
        .name("tous_fr")
        .year(0)
        .departementName("ALL")
        .source(OPENSTREETMAP)
        .maximumZoomLevel(HOUSES_0)
        .precisionLevelInCm(20)
        .build();
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainGeoserverLayer() {
    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("area_picture_map_1_id")
        .name("area_picture_map_1_name")
        .year(2020)
        .departementName("ain")
        .source(GEOSERVER)
        .maximumZoomLevel(HOUSES_0)
        .precisionLevelInCm(20)
        .build();
  }

  static AreaPictureDetails areaPicture1() {
    return new AreaPictureDetails()
        .id(AREA_PICTURE_1_ID)
        .xTile(524720)
        .yTile(374531)
        .zoomLevel(HOUSES_0)
        .actualLayer(tousFrLayer())
        .address("Montauban Address")
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .fileId("montauban_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
        .otherLayers(List.of(tousFrLayer()))
        .layer(DEFAULT_OSM_LAYER)
        .zoom(new Zoom().level(HOUSES_0).number(20))
        .availableLayers(List.of(DEFAULT_OSM_LAYER))
        .filename("tous_fr_HOUSES_0_524720_374531");
  }

  static AreaPictureDetails areaPicture2() {
    return new AreaPictureDetails()
        .id("area_picture_2_id")
        .zoomLevel(HOUSES_0)
        .actualLayer(tousFrLayer())
        .xTile(524720)
        .yTile(374531)
        .layer(DEFAULT_OSM_LAYER)
        .otherLayers(List.of(tousFrLayer()))
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .address("Cannes Address")
        .fileId("mulhouse_1_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
        .availableLayers(List.of(DEFAULT_OSM_LAYER))
        .zoom(new Zoom().level(HOUSES_0).number(20))
        .filename("tous_fr_HOUSES_0_524720_374531");
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpBanApiMock(banApiMock);
  }

  void setUpBanApiMock(BanApi banApi) {
    when(banApi.search(any())).thenReturn(DEFAULT_KNOWN_GEO_POSITION);
    when(banApi.fSearch(any())).thenReturn(DEFAULT_KNOWN_GEO_POSITION);
  }

  private static final GeoUtils.Coordinate DEFAULT_KNOWN_COORDINATES =
      GeoUtils.Coordinate.builder().longitude(0.148409).latitude(45.644018).build();
  private static final GeoPosition DEFAULT_KNOWN_GEO_POSITION =
      GeoPosition.builder().coordinates(DEFAULT_KNOWN_COORDINATES).build();

  private static final Tile DEFAULT_KNOWN_TILE = Tile.builder().x(524720).y(374531).build();

  @Test
  void joe_doe_read_his_pictures_ok() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    AreaPictureApi api = new AreaPictureApi(joeDoeClient);

    List<AreaPictureDetails> allAreaPictures =
        api.getAllAreaPictures(JOE_DOE_ACCOUNT_ID, 1, 10, null, null);
    List<AreaPictureDetails> addressFilteredAreaPictures =
        api.getAllAreaPictures(JOE_DOE_ACCOUNT_ID, 1, 10, "Montauban", null);
    List<AreaPictureDetails> filenameFilteredAreaPictures =
        api.getAllAreaPictures(JOE_DOE_ACCOUNT_ID, 1, 10, null, "Montauban");
    AreaPictureDetails actualAreaPictureOne =
        api.getAreaPictureById(JOE_DOE_ACCOUNT_ID, AREA_PICTURE_1_ID);
    AreaPictureDetails actualAreaPictureTwo =
        api.getAreaPictureById(JOE_DOE_ACCOUNT_ID, "area_picture_2_id");

    assertEquals(areaPicture1(), actualAreaPictureOne);
    assertEquals(areaPicture2(), actualAreaPictureTwo);
    assertTrue(
        allAreaPictures.stream()
            .map(AreaPictureIT::removeAvailableLayers)
            .toList()
            .containsAll(
                List.of(
                    removeAvailableLayers(areaPicture2()), removeAvailableLayers(areaPicture1()))));
    assertTrue(
        addressFilteredAreaPictures.stream()
            .map(AreaPictureIT::removeAvailableLayers)
            .toList()
            .contains(removeAvailableLayers(areaPicture1())));
    assertFalse(
        addressFilteredAreaPictures.stream()
            .map(AreaPictureIT::removeAvailableLayers)
            .toList()
            .contains(removeAvailableLayers(areaPicture2())));
    assertTrue(
        filenameFilteredAreaPictures.stream()
            .map(AreaPictureIT::removeAvailableLayers)
            .toList()
            .contains(removeAvailableLayers(areaPicture1())));
    assertFalse(
        filenameFilteredAreaPictures.stream()
            .map(AreaPictureIT::removeAvailableLayers)
            .toList()
            .contains(removeAvailableLayers(areaPicture2())));
  }

  @Test
  void crupdate_area_picture_details() throws ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    AreaPictureApi api = new AreaPictureApi(joeDoeClient);
    String payloadId = randomUUID().toString();
    CrupdateAreaPictureDetails payload = crupdatableAreaPictureDetails();

    var actual = api.crupdateAreaPictureDetails(JOE_DOE_ACCOUNT_ID, payloadId, payload);

    assertEquals(
        removeAvailableLayers(createFrom(payload, payloadId)),
        removeAvailableLayers(ignoreDatesOf(actual)));
  }

  static AreaPictureDetails ignoreDatesOf(AreaPictureDetails areaPictureDetails) {
    areaPictureDetails.setCreatedAt(null);
    areaPictureDetails.setUpdatedAt(null);
    return areaPictureDetails;
  }

  static AreaPictureDetails removeAvailableLayers(AreaPictureDetails areaPictureDetails) {
    return areaPictureDetails.availableLayers(List.of()).otherLayers(List.of());
  }

  static CrupdateAreaPictureDetails crupdatableAreaPictureDetails() {
    String fileId = randomUUID().toString();
    return new CrupdateAreaPictureDetails()
        .address("Angoulême")
        .fileId(fileId)
        .prospectId(PROSPECT_1_ID)
        .zoomLevel(HOUSES_0)
        .createdAt(null)
        .updatedAt(null);
  }

  static AreaPictureDetails createFrom(CrupdateAreaPictureDetails crupdate, String payloadId) {
    var tile = DEFAULT_KNOWN_TILE;
    ZoomLevel zoomLevel = crupdate.getZoomLevel();
    return new AreaPictureDetails()
        .id(payloadId)
        .xTile(tile.getX())
        .yTile(tile.getY())
        .address(crupdate.getAddress())
        .prospectId(crupdate.getProspectId())
        .fileId(crupdate.getFileId())
        .actualLayer(tousFrLayer())
        .zoomLevel(zoomLevel)
        .otherLayers(List.of(tousFrLayer()))
        .filename(getFilename(crupdate, tile))
        .layer(TOUS_FR)
        // need to update or nullify createdAt and updatedAt during equality check
        .createdAt(null)
        .zoom(new Zoom().level(zoomLevel).number(ArcgisZoom.from(zoomLevel).getZoomLevel()))
        .updatedAt(null);
  }

  private static String getFilename(CrupdateAreaPictureDetails crupdate, Tile tile) {
    return "%s_%s_%s_%s"
        .formatted(
            DEFAULT_FRANCE_LAYER, crupdate.getZoomLevel().getValue(), tile.getX(), tile.getY());
  }

  @Test
  void openstreetmap_layer_guesser_ok() {
    GeoUtils.Coordinate coordinates = DEFAULT_KNOWN_COORDINATES;
    var guessedLayers =
        mapLayerService.getAvailableLayersFrom(
            Tile.from(coordinates.getLongitude(), coordinates.getLatitude(), ArcgisZoom.HOUSES_0));

    assertEquals(List.of(domainOsmLayer()), guessedLayers);
  }
}
