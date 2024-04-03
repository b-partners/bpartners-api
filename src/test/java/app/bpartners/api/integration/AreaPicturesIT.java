package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.MONTAUBAN_2020;
import static app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer.MULHOUSE_2018;
import static app.bpartners.api.endpoint.rest.model.ZoomLevel.HOUSES_0;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.AreaPictureApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AreaPictureDetails;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
public class AreaPicturesIT extends MockedThirdParties {
  private ApiClient joeDoeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  private static final String AREA_PICTURE_1_ID = "area_picture_1_id";

  static AreaPictureDetails areaPicture1() {
    return new AreaPictureDetails()
        .id(AREA_PICTURE_1_ID)
        .zoomLevel(HOUSES_0)
        .layer(MONTAUBAN_2020)
        .address("Montauban Address")
        .fileId("montauban_5cm_544729_383060.jpg")
        .filename("montauban_5cm_544729_383060.jpg");
  }

  static AreaPictureDetails areaPicture2() {
    return new AreaPictureDetails()
        .id("area_picture_2_id")
        .zoomLevel(HOUSES_0)
        .layer(MULHOUSE_2018)
        .address("Cannes Address")
        .fileId("mulhouse_1_5cm_544729_383060.jpg")
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
}
