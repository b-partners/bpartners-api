package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;

import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AreaPictureIT extends S3MockedThirdParties {
  @Autowired ObjectMapper om;

  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  public static final String AREA_PICTURE_1_ID = "area_picture_1_id";
  public static final String AREA_PICTURE_2_ID = "area_picture_2_id";
  /*
  static AreaPictureDetails areaPicture1() {
    return new AreaPictureDetails()
        .id(AREA_PICTURE_1_ID)
        .zoomLevel(HOUSES_0)
        .layer(MONTAUBAN_2020)
        .xTile(553415)
        .yTile(492049)
        .address("Montauban Address")
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .fileId("montauban_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
        .filename("montauban_5cm_544729_383060.jpg");
  }

  static AreaPictureDetails areaPicture2() {
    return new AreaPictureDetails()
        .id("area_picture_2_id")
        .zoomLevel(HOUSES_0)
        .layer(MULHOUSE_2018)
        .xTile(553415)
        .yTile(492049)
        .createdAt(Instant.parse("2022-01-08T01:00:00Z"))
        .updatedAt(Instant.parse("2022-01-08T01:00:00Z"))
        .address("Cannes Address")
        .fileId("mulhouse_1_5cm_544729_383060.jpg")
        .prospectId(PROSPECT_1_ID)
        .filename("mulhouse_5cm_544729_383060.jpg");
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
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

    assertEquals(areaPicture1(), actualAreaPictureOne);
    assertTrue(allAreaPictures.containsAll(List.of(areaPicture1(), areaPicture2())));
    assertTrue(addressFilteredAreaPictures.contains(areaPicture1()));
    assertFalse(addressFilteredAreaPictures.contains(areaPicture2()));
    assertTrue(filenameFilteredAreaPictures.contains(areaPicture1()));
    assertFalse(filenameFilteredAreaPictures.contains(areaPicture2()));
  }

  @Test
  @Disabled("TODO: fail because of LocalStack")
  void download_and_save_area_picture_ok() throws IOException, InterruptedException, ApiException {
    ApiClient joeDoeClient = joeDoeClient();
    AreaPictureApi api = new AreaPictureApi(joeDoeClient);
    String basePath = "http://localhost:" + localPort;
    String payloadId = randomUUID().toString();
    CrupdateAreaPictureDetails payload = crupdatableAreaPictureDetails();
    var expected = from(payload, payloadId);
    var actual = download_with_request_body(basePath, payloadId, null, JOE_DOE_TOKEN, payload);
    var saved = api.getAreaPictureById(JOE_DOE_ACCOUNT_ID, payloadId);

    assertEquals(expected, saved);
  }

  static CrupdateAreaPictureDetails crupdatableAreaPictureDetails() {
    String fileId = randomUUID().toString();
    return new CrupdateAreaPictureDetails()
        .address("Angoulême")
        .fileId(fileId)
        .zoomLevel(HOUSES_0)
        .filename(fileId)
        .createdAt(null)
        .updatedAt(null);
  }

  static AreaPictureDetails from(CrupdateAreaPictureDetails crupdate, String payloadId) {
    return new AreaPictureDetails()
        .id(payloadId)
        .address(crupdate.getAddress())
        .fileId(crupdate.getFileId())
        .layer(DEFAULT_FRANCE_LAYER)
        .zoomLevel(crupdate.getZoomLevel())
        .filename(crupdate.getFilename())
        // need to update or nullify createdAt and updatedAt during equality check
        .createdAt(null)
        .updatedAt(null);
  }

  public HttpResponse<byte[]> download_with_request_body(
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
  */
}
