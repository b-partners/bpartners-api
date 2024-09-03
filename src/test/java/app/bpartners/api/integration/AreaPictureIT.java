package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER_IGN;
import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.PROSPECT_1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.lang.Boolean.TRUE;
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
import app.bpartners.api.endpoint.rest.model.Tile;
import app.bpartners.api.endpoint.rest.model.Zoom;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.ban.response.GeoJsonProperty;
import app.bpartners.api.repository.ban.response.GeoJsonResponse;
import app.bpartners.api.service.WMS.ArcgisZoom;
import app.bpartners.api.service.WMS.AreaPictureMapLayerService;
import app.bpartners.api.service.WMS.imageSource.WmsImageSource;
import app.bpartners.api.service.utils.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.FileSystemResource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
public class AreaPictureIT extends S3MockedThirdParties {
  @Deprecated public static final OpenStreetMapLayer DEFAULT_OSM_LAYER = TOUS_FR;
  public static final String AREA_PICTURE_1_ID = "area_picture_1_id";
  public static final String AREA_PICTURE_2_ID = "area_picture_2_id";
  private static final GeoUtils.Coordinate SOMEWHERE_IN_CHARENTE_KNOWN_COORDINATES =
      GeoUtils.Coordinate.builder().longitude(0.148409).latitude(45.644018).build();
  private static final GeoPosition CHARENTE_KNOWN_GEO_POSITION =
      GeoPosition.builder()
          .label("charente")
          .coordinates(SOMEWHERE_IN_CHARENTE_KNOWN_COORDINATES)
          .build();
  private static final app.bpartners.api.service.WMS.Tile DEFAULT_KNOWN_TILE =
      app.bpartners.api.service.WMS.Tile.builder()
          .x(524720)
          .y(374531)
          .arcgisZoom(ArcgisZoom.HOUSES_0)
          .build();
  private static final GeoJsonResponse.Feature HIGHEST_FEAT_GEOJSON_FEATURE =
      GeoJsonResponse.Feature.builder()
          .properties(
              GeoJsonProperty.builder()
                  .label("Adresse")
                  .geoLegalPosX(DEFAULT_KNOWN_TILE.getLongitude())
                  .geoLegalPosY(DEFAULT_KNOWN_TILE.getLatitude())
                  .score(15.0)
                  .build())
          .geometry(
              GeoJsonResponse.Geometry.builder()
                  .coordinates(
                      List.of(DEFAULT_KNOWN_TILE.getLongitude(), DEFAULT_KNOWN_TILE.getLatitude()))
                  .build())
          .build();
  @Autowired ObjectMapper om;
  @Autowired AreaPictureMapLayerService mapLayerService;
  @MockBean BanApi banApiMock;
  @MockBean WmsImageSource wmsImageSourceMock;
  @Autowired AccountRepository accountRepository;
  @MockBean AccountHolderRepository accountHolderRepository;

  static AreaPictureMapLayer geoserverCharenteLayer() {
    return new AreaPictureMapLayer()
        .id("area_picture_map_1_id")
        .name("area_picture_map_1_name")
        .year(2023)
        .precisionLevelInCm(20)
        .maximumZoomLevel(HOUSES_0)
        .departementName("charente")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER);
  }

  static AreaPictureMapLayer geoserverIGNPrimaryDefaultServerLayer() {
    return new AreaPictureMapLayer()
        .id("1cccfc17-cbef-4320-bdfa-0d1920b91f11")
        .name("ORTHOIMAGERY.ORTHOPHOTOS")
        .year(2023)
        .precisionLevelInCm(20)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER_IGN);
  }

  static AreaPictureMapLayer geoserverIGNServerLayer() {
    return new AreaPictureMapLayer()
        .id("9a4bd8b7-556b-49a1-bea0-c35e961dab64")
        .name("FLUX_IGN_2023_20CM")
        .year(2023)
        .precisionLevelInCm(20)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER);
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainGeoserverCharenteLayer() {
    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("area_picture_map_1_id")
        .name("area_picture_map_1_name")
        .year(2023)
        .departementName("charente")
        .source(GEOSERVER)
        .maximumZoomLevel(HOUSES_0)
        .precisionLevelInCm(20)
        .build();
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainGeoserverIGNLayer() {
    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("1cccfc17-cbef-4320-bdfa-0d1920b91f11")
        .name("ORTHOIMAGERY.ORTHOPHOTOS")
        .year(2023)
        .departementName("ALL")
        .source(GEOSERVER_IGN)
        .maximumZoomLevel(HOUSES_0)
        .precisionLevelInCm(20)
        .build();
  }

  static AreaPictureDetails areaPicture1() {
    int xTile = 524720;
    int yTile = 374531;
    ZoomLevel zoomLevel = HOUSES_0;
    Zoom zoom = new Zoom().level(zoomLevel).number(20);
    boolean isExtended = false;
    Tile currentTile = new Tile().x(xTile).y(yTile).zoom(zoom);
    return new AreaPictureDetails()
        .id(AREA_PICTURE_1_ID)
        .xTile(xTile)
        .yTile(yTile)
        .zoomLevel(zoomLevel)
        .actualLayer(geoserverIGNServerLayer())
        .address("Montauban Address")
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .fileId("montauban_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
        .otherLayers(List.of(geoserverCharenteLayer(), geoserverIGNPrimaryDefaultServerLayer()))
        .layer(DEFAULT_OSM_LAYER)
        .zoom(zoom)
        .availableLayers(List.of(DEFAULT_OSM_LAYER))
        .isExtended(isExtended)
        .currentTile(currentTile)
        .referenceTile(getReferenceTile(currentTile, isExtended))
        .currentGeoPosition(
            new app.bpartners.api.endpoint.rest.model.GeoPosition()
                .score(90.0)
                .longitude(0.148409)
                .latitude(45.644018))
        .filename("FLUX_IGN_2023_20CM_HOUSES_0_524720_374531")
        .geoPositions(
            List.of(
                new app.bpartners.api.endpoint.rest.model.GeoPosition()
                    .score(90.0)
                    .longitude(0.148409)
                    .latitude(45.644018),
                new app.bpartners.api.endpoint.rest.model.GeoPosition()
                    .score(30.0)
                    .longitude(0.148409)
                    .latitude(45.644018),
                new app.bpartners.api.endpoint.rest.model.GeoPosition()
                    .score(40.0)
                    .longitude(0.148409)
                    .latitude(45.644018)));
  }

  private static Tile getReferenceTile(Tile originalTile, boolean isExtended) {
    return isExtended
        ? new Tile()
            .x(originalTile.getX() - 1)
            .y(originalTile.getY() - 1)
            .zoom(originalTile.getZoom())
        : originalTile;
  }

  static AreaPictureDetails areaPicture2() {
    int xTile = 524720;
    int yTile = 374531;
    Zoom zoom = new Zoom().level(HOUSES_0).number(20);
    Tile currentTile = new Tile().x(xTile).y(yTile).zoom(zoom);
    boolean isExtended = true;
    Tile referenceTile = getReferenceTile(currentTile, isExtended);
    return new AreaPictureDetails()
        .id("area_picture_2_id")
        .zoomLevel(HOUSES_0)
        .actualLayer(geoserverIGNServerLayer())
        .xTile(xTile)
        .yTile(yTile)
        .layer(DEFAULT_OSM_LAYER)
        .otherLayers(List.of(geoserverCharenteLayer(), geoserverIGNPrimaryDefaultServerLayer()))
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .address("Cannes Address")
        .fileId("mulhouse_1_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
        .availableLayers(List.of(DEFAULT_OSM_LAYER))
        .zoom(zoom)
        .isExtended(isExtended)
        .filename(
            "FLUX_IGN_2023_20CM_HOUSES_0_"
                + referenceTile.getX()
                + "_"
                + referenceTile.getY()
                + "_extended")
        .currentTile(currentTile)
        .referenceTile(referenceTile)
        .currentGeoPosition(
            new app.bpartners.api.endpoint.rest.model.GeoPosition()
                .score(60.0)
                .longitude(0.148409)
                .latitude(45.644018))
        .geoPositions(
            List.of(
                new app.bpartners.api.endpoint.rest.model.GeoPosition()
                    .score(60.0)
                    .longitude(0.148409)
                    .latitude(45.644018)));
  }

  static AreaPictureDetails ignoreGeneratedDataOf(AreaPictureDetails areaPictureDetails) {
    areaPictureDetails.setCreatedAt(null);
    areaPictureDetails.setUpdatedAt(null);
    areaPictureDetails.setFilename(null);
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
    var isExtended = TRUE.equals(crupdate.getIsExtended());
    int xTile = tile.getX();
    int yTile = tile.getY();
    Zoom zoom = new Zoom().level(zoomLevel).number(ArcgisZoom.from(zoomLevel).getZoomLevel());
    Tile currentTile = new Tile().x(xTile).y(yTile).zoom(zoom);
    return new AreaPictureDetails()
        .id(payloadId)
        .xTile(xTile)
        .yTile(yTile)
        .address(crupdate.getAddress())
        .prospectId(crupdate.getProspectId())
        .fileId(crupdate.getFileId())
        .actualLayer(geoserverCharenteLayer())
        .zoomLevel(zoomLevel)
        .otherLayers(List.of(geoserverIGNPrimaryDefaultServerLayer()))
        .filename(null)
        .layer(TOUS_FR)
        .currentTile(currentTile)
        .referenceTile(getReferenceTile(currentTile, isExtended))
        // need to update or nullify createdAt and updatedAt during equality check
        .createdAt(null)
        .zoom(zoom)
        .isExtended(isExtended)
        .updatedAt(null);
  }

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpBanApiMock(banApiMock);
    setUpWmsImageSourceMock(wmsImageSourceMock);
  }

  private void setUpWmsImageSourceMock(WmsImageSource wmsImageSource) {
    FileSystemResource mockJpegFile =
        new FileSystemResource(
            this.getClass().getClassLoader().getResource("files/downloaded.jpeg").getFile());
    when(wmsImageSource.downloadImage(any())).thenReturn(mockJpegFile.getFile());
  }

  void setUpBanApiMock(BanApi banApi) {
    when(banApi.search(any())).thenReturn(CHARENTE_KNOWN_GEO_POSITION);
    when(banApi.searchMultiplePos(any()))
        .thenReturn(
            GeoJsonResponse.builder()
                .features(
                    List.of(
                        GeoJsonResponse.Feature.builder()
                            .properties(
                                GeoJsonProperty.builder()
                                    .label("Adresse")
                                    .geoLegalPosX(13.0)
                                    .geoLegalPosY(10.0)
                                    .score(10.0)
                                    .build())
                            .geometry(
                                GeoJsonResponse.Geometry.builder()
                                    .coordinates(List.of(13.0, 10.0))
                                    .build())
                            .build(),
                        HIGHEST_FEAT_GEOJSON_FEATURE))
                .build());
    when(banApi.fSearch(any())).thenReturn(CHARENTE_KNOWN_GEO_POSITION);
  }

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
    when(accountHolderRepository.findById(any()))
        .thenReturn(AccountHolder.builder().id("accountHolderId").build());
    var actual = api.crupdateAreaPictureDetails(JOE_DOE_ACCOUNT_ID, payloadId, payload);

    AreaPictureDetails expected = removeAvailableLayers(createFrom(payload, payloadId));
    expected.setGeoPositions(actual.getGeoPositions());
    expected.setCurrentGeoPosition(actual.getCurrentGeoPosition());
    assertEquals(expected, removeAvailableLayers(ignoreGeneratedDataOf(actual)));
  }

  @Test
  void map_layer_guesser_ok() {
    GeoUtils.Coordinate coordinates = SOMEWHERE_IN_CHARENTE_KNOWN_COORDINATES;

    var guessedLayers =
        mapLayerService.getAvailableLayersFrom(
            app.bpartners.api.service.WMS.Tile.from(
                coordinates.getLongitude(), coordinates.getLatitude(), ArcgisZoom.HOUSES_0));

    assertEquals(List.of(domainGeoserverCharenteLayer(), domainGeoserverIGNLayer()), guessedLayers);
  }
}
