package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.TOUS_FR;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.integration.conf.utils.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.utils.TestUtils.BEARER_QUERY_PARAMETER_NAME;
import static app.bpartners.api.integration.conf.utils.TestUtils.DEFAULT_FRANCE_LAYER;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.PROSPECT_1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.downloadBytes;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.AreaPictureApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.WMS.MapLayer;
import app.bpartners.api.service.WMS.MapLayerGuesser;
import app.bpartners.api.service.WMS.Tile;
import app.bpartners.api.service.utils.GeoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
public class AreaPictureIT extends S3MockedThirdParties {
  @Autowired ObjectMapper om;

  @Autowired MapLayerGuesser mapLayerGuesser;
  @MockBean BanApi banApiMock;

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  public static final String AREA_PICTURE_1_ID = "area_picture_1_id";
  public static final String AREA_PICTURE_2_ID = "area_picture_2_id";

  static AreaPictureDetails areaPicture1() {
    return new AreaPictureDetails()
        .id(AREA_PICTURE_1_ID)
        .xTile(524720)
        .yTile(374531)
        .zoomLevel(HOUSES_0)
        .layer(TOUS_FR)
        .address("Montauban Address")
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .fileId("montauban_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
        .availableLayers(List.of(TOUS_FR))
        .filename("tous_fr_HOUSES_0_524720_374531");
  }

  static AreaPictureDetails areaPicture2() {
    return new AreaPictureDetails()
        .id("area_picture_2_id")
        .zoomLevel(HOUSES_0)
        .layer(TOUS_FR)
        .xTile(524720)
        .yTile(374531)
        .availableLayers(List.of(TOUS_FR))
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .address("Cannes Address")
        .fileId("mulhouse_1_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
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
        allAreaPictures.containsAll(
            List.of(removeAvailableLayers(areaPicture1()), removeAvailableLayers(areaPicture2()))));
    assertTrue(addressFilteredAreaPictures.contains(removeAvailableLayers(areaPicture1())));
    assertFalse(addressFilteredAreaPictures.contains(removeAvailableLayers(areaPicture2())));
    assertTrue(filenameFilteredAreaPictures.contains(removeAvailableLayers(areaPicture1())));
    assertFalse(filenameFilteredAreaPictures.contains(removeAvailableLayers(areaPicture2())));
  }

  @Test
  void download_and_save_area_picture_ok() throws IOException, InterruptedException, ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    AreaPictureApi api = new AreaPictureApi(joeDoeClient);
    String basePath = "http://localhost:" + localPort;
    String payloadId = randomUUID().toString();
    CrupdateAreaPictureDetails payload = crupdatableAreaPictureDetails();

    var actual = downloadAndSaveAreaPicture(basePath, payloadId, null, JOE_DOE_TOKEN, payload);
    var saved = api.getAreaPictureById(JOE_DOE_ACCOUNT_ID, payloadId);

    assertEquals(createFrom(payload, payloadId), ignoreDatesOf(saved));
    assertTrue(actual.body().length > 0);
  }

  static AreaPictureDetails ignoreDatesOf(AreaPictureDetails areaPictureDetails) {
    areaPictureDetails.setCreatedAt(null);
    areaPictureDetails.setUpdatedAt(null);
    return areaPictureDetails;
  }

  static AreaPictureDetails removeAvailableLayers(AreaPictureDetails areaPictureDetails) {
    return areaPictureDetails.availableLayers(List.of());
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
    return new AreaPictureDetails()
        .id(payloadId)
        .xTile(tile.getX())
        .yTile(tile.getY())
        .address(crupdate.getAddress())
        .prospectId(crupdate.getProspectId())
        .fileId(crupdate.getFileId())
        .layer(DEFAULT_FRANCE_LAYER)
        .zoomLevel(crupdate.getZoomLevel())
        .availableLayers(List.of(DEFAULT_FRANCE_LAYER))
        .filename(getFilename(crupdate, tile))
        // need to update or nullify createdAt and updatedAt during equality check
        .createdAt(null)
        .updatedAt(null);
  }

  private static String getFilename(CrupdateAreaPictureDetails crupdate, Tile tile) {
    return "%s_%s_%s_%s"
        .formatted(
            DEFAULT_FRANCE_LAYER, crupdate.getZoomLevel().getValue(), tile.getX(), tile.getY());
  }

  public HttpResponse<byte[]> downloadAndSaveAreaPicture(
      String basePath,
      String areaPictureId,
      String queryBearer,
      String token,
      CrupdateAreaPictureDetails toCreate)
      throws ApiException, IOException, InterruptedException {
    var requestBuilder =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    basePath
                        + "/accounts/"
                        + JOE_DOE_ACCOUNT_ID
                        + "/areaPictures/"
                        + areaPictureId
                        + "/raw?"
                        + BEARER_QUERY_PARAMETER_NAME
                        + "="
                        + queryBearer))
            .header("Access-Control-Request-Method", "POST")
            .header("Authorization", BEARER_PREFIX + token)
            .header("Content-Type", "application/json")
            .header("Accept", "image/png,image/jpeg");

    try {
      var body = om.writeValueAsBytes(toCreate);
      requestBuilder.method("PUT", HttpRequest.BodyPublishers.ofByteArray(body));
    } catch (IOException e) {
      throw new ApiException(e);
    }

    return downloadBytes(requestBuilder.build(), "downloadAndSaveAreaPicture");
  }

  @Test
  void openstreetmap_layer_guesser_ok() {
    var guessedLayers =
        mapLayerGuesser.apply(
            AreaPicture.builder()
                .longitude(DEFAULT_KNOWN_COORDINATES.getLongitude())
                .latitude(DEFAULT_KNOWN_COORDINATES.getLatitude())
                .build());

    assertEquals(List.of(MapLayer.TOUS_FR), guessedLayers);
  }

  @Test
  void openstreetmap_layer_guesser_ko() {
    assertThrows(
        BadRequestException.class,
        () -> mapLayerGuesser.apply(AreaPicture.builder().longitude(10).latitude(11).build()));
  }
}
