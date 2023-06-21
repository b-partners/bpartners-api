package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.jpa.MunicipalityJpaRepository;
import app.bpartners.api.repository.jpa.model.HMunicipality;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermit;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.BuildingPermitList;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.model.GeoJson;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.integration.conf.TestUtils.NOT_JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ProspectIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class ProspectIT {
  private static final String UNKNOWN_PROSPECT_ID = "unknown_prospect_id";
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private CognitoComponent cognitoComponentMock;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private BuildingPermitApi buildingPermitApiMock;
  @MockBean
  private BuildingPermitConf buildingPermitConfMock;
  @MockBean
  private BridgeApi bridgeApi;
  @Autowired
  private MunicipalityJpaRepository municipalityJpaRepository;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    when(buildingPermitApiMock.getBuildingPermitList(any())).thenReturn(buildingPermitList());
  }

  BuildingPermit buildingPermit() {
    return BuildingPermit.builder()
        .insee("123456")
        .ref("ref")
        .fileId(123456)
        .fileRef("fileRef")
        .sitadel(true)
        .type("PC")
        .longType("Permis de construire")
        .centroidGeoJson(
            GeoJson.<List<Object>>builder()
                .coordinates(List.of(1.0, 2.0))
                .type("Point")
                .build()
        )
        .build();
  }

  BuildingPermitList buildingPermitList() {
    return BuildingPermitList.builder()
        .total(1000)
        .limit(1000)
        .records(List.of(buildingPermit()))
        .build();
  }

  Prospect prospect1() {
    return new Prospect()
        .id("prospect1_id")
        .name(null)
        .location(null)
        .status(TO_CONTACT)
        .email(null)
        .phone(null)
        .address(null)
        .townCode(92002);
  }

  Prospect prospect2() {
    return new Prospect()
        .id("prospect2_id")
        .name("jane doe")
        .location(null)
        .status(TO_CONTACT)
        .email("janeDoe@gmail.com")
        .phone("+261340465339")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .townCode(92002);
  }

  Prospect prospect3() {
    return new Prospect()
        .id("prospect3_id")
        .name("markus adams")
        .location(null)
        .status(TO_CONTACT)
        .email("markusAdams@gmail.com")
        .phone("+261340465340")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .townCode(92001);
  }

  UpdateProspect updateProspect() {
    return new UpdateProspect()
        .id("prospect1_id")
        .name("paul adams")
        .status(TO_CONTACT)
        .email("paulAdams@gmail.com")
        .phone("+261340465341")
        .address("30 Rue de la Montagne Sainte-Genevieve");
  }

  Prospect expectedProspect() {
    return new Prospect()
        .name("paul adams")
        .location(null)
        .status(TO_CONTACT)
        .email("paulAdams@gmail.com")
        .phone("+261340465341")
        .address("30 Rue de la Montagne Sainte-Genevieve");
  }

  HMunicipality antony() {
    return HMunicipality.builder()
        .id("43c152ed-89f2-43c5-a3d2-d5cb1fb59f62")
        .name("Antony")
        .code("92002")
        .build();
  }

  @Test
  @Order(1)
  void read_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Prospect> actual = api.getProspects(ACCOUNTHOLDER_ID);

    assertEquals(2, actual.size());
    assertTrue(actual.contains(prospect1()));
    assertTrue(actual.contains(prospect2()));
    assertFalse(actual.contains(prospect3()));
  }

  @Test
  @Order(2)
  void update_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Prospect> actual = api.updateProspects(ACCOUNTHOLDER_ID, List.of(updateProspect()));

    assertEquals(List.of(expectedProspect()), ignoreIdsOf(actual));
  }

  @Test
  void update_prospects_ko() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Prospect." + UNKNOWN_PROSPECT_ID
            + " not found. \"}",
        () -> api.updateProspects(ACCOUNTHOLDER_ID,
            List.of(updateProspect().id(UNKNOWN_PROSPECT_ID))));
  }

  @Test
  void convert_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsApiException("{\"type\":\"501 NOT_IMPLEMENTED\","
            + "\"message\":\"prospect conversion not implemented yet\"}",
        () -> api.convertProspect(ACCOUNTHOLDER_ID, prospect1().getId(), List.of()));
  }

  @Test
  void joe_doe_access_other_prospects_ko() {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.updateProspects(NOT_JOE_DOE_ACCOUNT_HOLDER_ID, List.of()));
    assertThrowsForbiddenException(() -> api.getProspects(NOT_JOE_DOE_ACCOUNT_HOLDER_ID));
    assertThrowsForbiddenException(
        () -> api.convertProspect(NOT_JOE_DOE_ACCOUNT_HOLDER_ID, prospect1().getId(), List.of()));
  }

  @Test
  void find_municipalities_within_distance_from_point_coordinates_ok() {
    String prospectingMunicipalityCode = "92002";
    List<HMunicipality> within0km =
        municipalityJpaRepository.findMunicipalitiesWithinDistance(prospectingMunicipalityCode, 0);
    List<HMunicipality> within2km =
        municipalityJpaRepository.findMunicipalitiesWithinDistance(prospectingMunicipalityCode, 2);
    List<HMunicipality> within5km =
        municipalityJpaRepository.findMunicipalitiesWithinDistance(prospectingMunicipalityCode, 5);

    assertTrue(within0km.contains(antony()));
    assertTrue(within2km.contains(antony()));
    assertTrue(within5km.contains(antony()));
    assertEquals(15, within5km.size());
  }

  private List<Prospect> ignoreIdsOf(List<Prospect> prospects) {
    return prospects.stream()
        .map(e -> {
          e.setId(null);
          return e;
        })
        .collect(Collectors.toUnmodifiableList());
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
