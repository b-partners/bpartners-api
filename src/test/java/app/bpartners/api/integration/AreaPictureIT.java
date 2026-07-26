package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.AIRBUS;
import static app.bpartners.api.endpoint.rest.model.AreaPictureImageSource.GEOSERVER;
import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.BUILDING;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static java.lang.Boolean.TRUE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.AreaPictureApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.mapper.AreaPictureRestMapper;
import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.AreaPictureMapLayer;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.endpoint.rest.model.Tile;
import app.bpartners.api.endpoint.rest.model.Zoom;
import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.mapper.AreaPictureMapLayerMapper;
import app.bpartners.api.model.mapper.AreaPictureMapper;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.ban.response.GeoJsonProperty;
import app.bpartners.api.repository.ban.response.GeoJsonResponse;
import app.bpartners.api.repository.google.geocode.GeoCodeApi;
import app.bpartners.api.service.areapicture.AreaPictureZoomValidator;
import app.bpartners.api.service.areapicture.MetaDataComponent;
import app.bpartners.api.service.geodata.ImageryService;
import app.bpartners.api.service.utils.GeoUtils;
import app.bpartners.api.service.wms.ArcgisZoom;
import app.bpartners.api.service.wms.AreaPictureMapLayerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

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
  private static final app.bpartners.api.service.wms.Tile DEFAULT_KNOWN_TILE =
      app.bpartners.api.service.wms.Tile.builder()
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
  @MockBean AreaPictureMapLayerService mapLayerServiceMock;
  @MockBean BanApi banApiMock;
  @Autowired AccountRepository accountRepository;
  @MockBean AccountHolderRepository accountHolderRepository;
  @MockBean GeoCodeApi geoCodeApiMock;
  @MockBean MetaDataComponent metaDataComponentMock;
  @MockBean AreaPictureZoomValidator areaPictureZoomValidatorMock;
  @MockBean ImageryService imageryServiceMock;
  @Autowired AreaPictureMapper areaPictureMapper;
  @Autowired AreaPictureRestMapper areaPictureRestMapper;
  @Autowired AreaPictureMapLayerMapper areaPictureMapLayerMapper;

  static AreaPictureMapLayer charenteLayer() {
    return new AreaPictureMapLayer()
        .id("08af0028-69ae-43a4-879a-a1950508ae6c")
        .name("CHARENTE_2019_5cm")
        .year(2019)
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("Charente")
        .lastUpdatedAt(LocalDate.parse("2019-01-01"))
        .creationDateTime(null)
        .expiredAt(null)
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER);
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainCharenteLayer() {
    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("08af0028-69ae-43a4-879a-a1950508ae6c")
        .name("CHARENTE_2019_5cm")
        .year(2019)
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("Charente")
        .lastUpdatedAt(LocalDate.parse("2019-01-01"))
        .precisionLevelInCm(5)
        .source(GEOSERVER)
        .build();
  }

  static AreaPictureMapLayer angouleme2019() {
    return new AreaPictureMapLayer()
        .id("5c80c22a-b5a4-4a34-8a5a-4f8fd9028a2a")
        .name("Angouleme_2019")
        .year(2019)
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("Charente")
        .lastUpdatedAt(LocalDate.parse("2019-01-01"))
        .creationDateTime(null)
        .expiredAt(null)
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER);
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainIGN2025() {

    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("1cccfc17-cbef-4320-bdfa-0d1920b91f11")
        .name("FLUX_IGN_2025_20CM")
        .year(2025)
        .precisionLevelInCm(20)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .lastUpdatedAt(LocalDate.parse("2025-01-01"))
        .source(GEOSERVER)
        .build();
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainAirbus2025() {

    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("532ea7da-918e-4bb7-bc34-e167a3829e19")
        .name("AIRBUS_PNEO")
        .year(2025)
        .precisionLevelInCm(30)
        .maximumZoomLevel(BUILDING)
        .departementName("ALL")
        .lastUpdatedAt(LocalDate.parse("2025-01-01"))
        .source(AIRBUS)
        .build();
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainPCRS2025() {

    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("726f5b3b-d23b-40c3-b38e-68a43d7ae155")
        .name("PCRS")
        .year(2025)
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .lastUpdatedAt(LocalDate.parse("2025-01-01"))
        .source(GEOSERVER)
        .build();
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainAngouleme2019() {
    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("5c80c22a-b5a4-4a34-8a5a-4f8fd9028a2a")
        .name("Angouleme_2019")
        .year(2019)
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("Charente")
        .lastUpdatedAt(LocalDate.parse("2019-01-01"))
        .source(GEOSERVER)
        .build();
  }

  static AreaPictureMapLayer geoserverIGNPrimaryDefaultServerLayer() {
    return new AreaPictureMapLayer()
        .id("1cccfc17-cbef-4320-bdfa-0d1920b91f11")
        .name("FLUX_IGN_2025_20CM")
        .year(2025)
        .lastUpdatedAt(LocalDate.of(2023, 1, 1))
        .precisionLevelInCm(20)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .lastUpdatedAt(LocalDate.of(2025, 1, 1))
        .source(GEOSERVER);
  }

  static AreaPictureMapLayer airbusDefaultServerLayer() {
    return new AreaPictureMapLayer()
        .id("532ea7da-918e-4bb7-bc34-e167a3829e19")
        .name("AIRBUS_PNEO")
        .year(2025)
        .lastUpdatedAt(LocalDate.of(2025, 1, 1))
        .precisionLevelInCm(30)
        .maximumZoomLevel(BUILDING)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(BUILDING).number(19))
        .source(AIRBUS);
  }

  static AreaPictureMapLayer geoserverPCRSLayer() {
    return new AreaPictureMapLayer()
        .id("726f5b3b-d23b-40c3-b38e-68a43d7ae155")
        .name("PCRS")
        .year(2025)
        .lastUpdatedAt(LocalDate.of(2025, 1, 1))
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER);
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainRhonePCRS2025() {

    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("2f343dba-dd5f-4895-9006-49472f576c02")
        .name("Auvergne_Rhone_Alpes_PCRS_5cm")
        .year(2025)
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .lastUpdatedAt(LocalDate.parse("2025-01-01"))
        .source(GEOSERVER)
        .build();
  }

  static AreaPictureMapLayer geoserverRhonePCRSLayer() {
    return new AreaPictureMapLayer()
        .id("2f343dba-dd5f-4895-9006-49472f576c02")
        .name("Auvergne_Rhone_Alpes_PCRS_5cm")
        .year(2025)
        .lastUpdatedAt(LocalDate.of(2025, 1, 1))
        .precisionLevelInCm(5)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER);
  }

  static AreaPictureMapLayer geoserverIGNServerLayer() {
    return new AreaPictureMapLayer()
        .id("9a4bd8b7-556b-49a1-bea0-c35e961dab64")
        .name("FLUX_IGN_2023_20CM")
        .year(2020)
        .lastUpdatedAt(LocalDate.of(2020, 1, 1))
        .precisionLevelInCm(20)
        .maximumZoomLevel(HOUSES_0)
        .departementName("ALL")
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .source(GEOSERVER);
  }

  static AreaPictureMapLayer restGeoserverCharenteLayerLatest() {
    return new AreaPictureMapLayer()
        .id("4b8e79bd-12ac-4c1b-8195-f9575d5fc4c8")
        .name("CHARENTE_2025")
        .year(2025)
        .departementName("Charente")
        .source(GEOSERVER)
        .maximumZoomLevel(HOUSES_0)
        .maximumZoom(new Zoom().level(HOUSES_0).number(20))
        .lastUpdatedAt(LocalDate.of(2025, 1, 1))
        .precisionLevelInCm(5);
  }

  static app.bpartners.api.model.AreaPictureMapLayer domainGeoserverCharenteLayerLatest() {
    return app.bpartners.api.model.AreaPictureMapLayer.builder()
        .id("4b8e79bd-12ac-4c1b-8195-f9575d5fc4c8")
        .name("CHARENTE_2025")
        .year(2025)
        .departementName("Charente")
        .source(GEOSERVER)
        .maximumZoomLevel(HOUSES_0)
        .lastUpdatedAt(LocalDate.of(2025, 1, 1))
        .precisionLevelInCm(5)
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
        .otherLayers(
            List.of(
                restGeoserverCharenteLayerLatest(),
                charenteLayer(),
                angouleme2019(),
                geoserverPCRSLayer(),
                geoserverRhonePCRSLayer(),
                geoserverIGNPrimaryDefaultServerLayer(),
                airbusDefaultServerLayer()))
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
        .xOffset(1234)
        .yOffset(123)
        .isOpaque(false)
        .shiftDirection(null)
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
            .x(originalTile.getX() - 3)
            .y(originalTile.getY() - 3)
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
        .otherLayers(
            List.of(
                restGeoserverCharenteLayerLatest(),
                charenteLayer(),
                angouleme2019(),
                geoserverPCRSLayer(),
                geoserverRhonePCRSLayer(),
                geoserverIGNPrimaryDefaultServerLayer(),
                airbusDefaultServerLayer()))
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
                    .latitude(45.644018)))
        .isOpaque(false)
        .shiftDirection(null)
        .xOffset(1234)
        .yOffset(123);
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
    return new CrupdateAreaPictureDetails()
        .address("Angoulême")
        .fileId("43bc1920-1d55-4106-8229-c12fe1a24b8c")
        .prospectId("prospect1_id")
        .zoomLevel(HOUSES_0)
        .createdAt(null)
        .shiftNb(0)
        .isExtended(true)
        .updatedAt(null);
  }

  static AreaPictureDetails createFrom(CrupdateAreaPictureDetails crupdate, String payloadId) {
    ZoomLevel zoomLevel = crupdate.getZoomLevel();
    var isExtended = TRUE.equals(crupdate.getIsExtended());
    Zoom zoom = new Zoom().level(zoomLevel).number(ArcgisZoom.from(zoomLevel).getZoomLevel());
    Tile currentTile = new Tile().x(524744).y(374510).zoom(zoom);
    return new AreaPictureDetails()
        .id(payloadId)
        .xTile(524744)
        .yTile(374510)
        .address(crupdate.getAddress())
        .prospectId(crupdate.getProspectId())
        .fileId(crupdate.getFileId())
        .actualLayer(charenteLayer())
        .zoomLevel(zoomLevel)
        .otherLayers(
            List.of(
                geoserverPCRSLayer(),
                geoserverRhonePCRSLayer(),
                geoserverIGNPrimaryDefaultServerLayer()))
        .filename(null)
        .layer(null)
        .currentTile(currentTile)
        .referenceTile(getReferenceTile(currentTile, isExtended))
        // need to update or nullify createdAt and updatedAt during equality check
        .createdAt(null)
        .zoom(zoom)
        .isExtended(isExtended)
        .shiftNb(0)
        .xOffset(1030)
        .yOffset(1410)
        .isOpaque(false)
        .updatedAt(null);
  }

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(null, JOE_DOE_API_KEY, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpBanApiMock(banApiMock);
    setUpUserSubscription(subscriptionService);
    when(metaDataComponentMock.getXOffset()).thenReturn(1234);
    when(metaDataComponentMock.getYOffset()).thenReturn(123);
    when(metaDataComponentMock.getAirbusYear()).thenReturn(2025);
    when(metaDataComponentMock.getLastUpdatedAt()).thenReturn(LocalDate.of(2025, 1, 1));
    doNothing().when(areaPictureZoomValidatorMock).accept(any());
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
  void joe_doe_read_his_pictures_ok() throws ApiException, IOException, InterruptedException {
    when(mapLayerServiceMock.getById(any()))
        .thenReturn(
            app.bpartners.api.model.AreaPictureMapLayer.builder()
                .id("9a4bd8b7-556b-49a1-bea0-c35e961dab64")
                .name("FLUX_IGN_2023_20CM")
                .year(2020)
                .lastUpdatedAt(LocalDate.of(2020, 1, 1))
                .precisionLevelInCm(20)
                .maximumZoomLevel(HOUSES_0)
                .departementName("ALL")
                .source(GEOSERVER)
                .build());
    when(mapLayerServiceMock.getAvailableLayersFrom(any()))
        .thenReturn(
            List.of(
                domainGeoserverCharenteLayerLatest(),
                domainCharenteLayer(),
                domainAngouleme2019(),
                domainPCRS2025(),
                domainRhonePCRS2025(),
                domainIGN2025(),
                domainAirbus2025()));
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
  void crupdate_area_picture_details()
      throws ApiException, IOException, InterruptedException, com.google.maps.errors.ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    AreaPictureApi api = new AreaPictureApi(joeDoeClient);
    String payloadId = randomUUID().toString();
    CrupdateAreaPictureDetails payload = crupdatableAreaPictureDetails();
    when(geoCodeApiMock.searchGeoPositionFromAddress(any()))
        .thenReturn(
            new app.bpartners.api.endpoint.rest.model.GeoPosition()
                .latitude(CHARENTE_KNOWN_GEO_POSITION.getCoordinates().getLatitude())
                .longitude(CHARENTE_KNOWN_GEO_POSITION.getCoordinates().getLongitude())
                .score(0.0));
    when(accountHolderRepository.findById(any()))
        .thenReturn(AccountHolder.builder().id("accountHolderId").build());
    when(mapLayerServiceMock.getById(payload.getLayerId()))
        .thenReturn(app.bpartners.api.model.AreaPictureMapLayer.builder().build());
    when(imageryServiceMock.downloadFromGeodataSource(any()))
        .thenReturn(expectedAreaPictureDetails());

    var actual = api.crupdateAreaPictureDetails(JOE_DOE_ACCOUNT_ID, payloadId, payload);

    AreaPictureDetails expected = removeAvailableLayers(createFrom(payload, payloadId));
    expected.setGeoPositions(actual.getGeoPositions());
    expected.setCurrentGeoPosition(actual.getCurrentGeoPosition());
    assertNotNull(actual.getImagePresignedUrl().getValue());
    actual.setImagePresignedUrl(null);
    assertEquals(expected, removeAvailableLayers(ignoreGeneratedDataOf(actual)));
  }

  public AreaPictureDetails expectedAreaPictureDetails() {
    Zoom zoom = new Zoom().level(HOUSES_0).number(20);
    Tile currentTile = new Tile().x(524744).y(374510).zoom(zoom);
    Tile referenceTile = new Tile().x(524741).y(374507).zoom(zoom);
    return new AreaPictureDetails()
        .id("05caa230-4f29-4461-bbee-7d48a97e9e39")
        .xTile(524744)
        .yTile(374510)
        .xOffset(1030)
        .yOffset(1410)
        .zoom(zoom)
        .currentTile(currentTile)
        .referenceTile(referenceTile)
        .currentGeoPosition(
            new app.bpartners.api.endpoint.rest.model.GeoPosition()
                .score(0.0)
                .longitude(0.1567288)
                .latitude(45.6488766))
        .geoPositions(
            List.of(
                new app.bpartners.api.endpoint.rest.model.GeoPosition()
                    .score(0.0)
                    .longitude(0.1567288)
                    .latitude(45.6488766)))
        .imagePresignedUrl(
            new PreSignedURL()
                .value(
                    "https://preprod-storage-bucket-geodata-b0fc7615-bucket-o0beeg6cdla6.s3.eu-west-3.amazonaws.com/CHARENTE_2019_5cm_HOUSES_0_524741_374507_extended?X-Amz-Security-Token=IQoJb3JpZ2luX2VjEG8aCWV1LXdlc3QtMyJHMEUCIEFDPSI7SjoLSLsrxShXRpB7NlLgnIUFhMC9cyFpXNjRAiEAyWlz4kMxVOlsZ%2FeGxQ3d423eYOCrvCXQNxr6iRXOA68qxwQIOBAAGgwxOTQ3MjI0MjcyMTMiDE75ogE4i09BIlRpTyqkBAM8HkbfL07leD6cuUaIH3RgEDwZJfYP%2FvVvN4gfJBfmzsx8DrXxk9AdBPGycBrkjUT53sCCUBTsqlAZqP%2FyA6Le6xXXDTyBBVgoATGuiIV8mmJ3oRzG%2BTWnazpr9hAOgRxRltnPhJT8PqgYfik27f0SbPpEO9V09lXF2FKptNyJur72FyATO95JPPeVCE8jxXxp1mPxkR6xZ3mJqenYkt0fnPVcxQPb2FxWDfP4JX37uqeQCJfFPa5c5XAIDtts%2Bzv36SxHL01Z%2BswrKNwBcbsTPMC6nWz0z%2FK9EtHupZ1Y56Xx1tUNE1eRJaSHXMztUrp7qW%2BAHoES6iP%2BmtKqcb6WMD9yFRY5ZlIzaGtjApt432y6%2FKXVNlyk1jy1e%2F1t8PjIpTYld9dAqpcEUlFFNLNeVZXQC4jByzl2xw8%2FmElAwg8YrXUNmdWIsz%2BWJq%2FVaKMw%2Fj%2FHv%2FyS3c4MUgOyMmkmFyfEaEFJUb%2BueIpLKun0M19AbmscUCSDF0%2BY16tBA6mvRd%2BV5dwZmXnjlY4iQQn4qNJpZo6DhS2P%2B9J6WHVtOVAjOm7JGwWkoWf9MLOs4wo%2FDy1cn6uX%2Baxt1ubw5FA1r4OxZkfXMizkQ8nSRdJs9MqbAi0X1wWEZ8kT64EmDdoqamCbfCJLxsa8k2lU0IZAwiF2MeiHIFSWNlcFWUmzYif9L6BQH4gcJfTWB9xiE6soWe7pSeYUTFxuoDg52F0rkBxEMMK4mNMGOqMB1hb%2BrjSZolt0lws8Utw16CAQtK7oi8fQnquDti6DcW6VDiPxMykcUD4UzB0r4xIULB99wTOIP6kN6ASj8Ku0jHQ8kwgFQvXmXuNGkmZgLcVek73hSzHdOi6taGxbIBAFWKMnLHptcwdeLU91MXFZpR4s78H4BvpLOlphPCz8VMK8%2BsRkq3FzeZRFYS23sGV9Oz4brZbiGjrLnNTk4S9JgGgmQQ%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260726T144040Z&X-Amz-SignedHeaders=host&X-Amz-Credential=ASIAS2VS4OFGVTDFQCIJ%2F20260726%2Feu-west-3%2Fs3%2Faws4_request&X-Amz-Expires=86400&X-Amz-Signature=b8e4286348326834defdee13187593d41c3480a614bfbcdc72b04ae056a2efdb")
                .expirationDelay(86400)
                .updatedAt(Instant.parse("2026-07-26T14:40:40.887298304Z")))
        .availableLayers(null)
        .actualLayer(charenteLayer())
        .address("Angoulême")
        .zoomLevel(HOUSES_0)
        .fileId("43bc1920-1d55-4106-8229-c12fe1a24b8c")
        .filename("CHARENTE_2019_5cm_HOUSES_0_524741_374507_extended")
        .prospectId(null)
        .createdAt(null)
        .updatedAt(null)
        .layer(null)
        .isExtended(true)
        .shiftNb(0)
        .isOpaque(false)
        .shiftDirection(null);
  }
}
