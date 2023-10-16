package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.EventPoller;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.ExtendedProspectStatus;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.endpoint.rest.model.ProspectRating;
import app.bpartners.api.endpoint.rest.model.ProspectStatusHistory;
import app.bpartners.api.endpoint.rest.model.UpdateProspect;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.repository.BusinessActivityRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.connectors.account.AccountConnectorRepository;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONTACTED;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.NOT_JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.getStatusHistory;
import static app.bpartners.api.integration.conf.utils.TestUtils.joeDoeAccountHolder;
import static app.bpartners.api.integration.conf.utils.TestUtils.prospect1;
import static app.bpartners.api.integration.conf.utils.TestUtils.prospect2;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.repository.implementation.ProspectRepositoryImpl.ANTI_HARM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class ProspectIT {
  private static final String UNKNOWN_PROSPECT_ID = "unknown_prospect_id";
  @MockBean
  private EventPoller eventPoller;
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
  @Autowired
  private BusinessActivityRepository businessRepository;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        DbEnvContextInitializer.getHttpServerPort());
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

  Prospect prospect3() {
    return new Prospect()
        .id("prospect3_id")
        .name("markus adams")
        .location(null)
        .status(TO_CONTACT)
        .statusHistory(getStatusHistory(TO_CONTACT))
        .email("markusAdams@gmail.com")
        .phone("+261340465340")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .townCode(92001)
        .rating(new ProspectRating()
            .value(BigDecimal.valueOf(0.0))
            .lastEvaluation(Instant.parse("2023-01-01T00:00:00Z")));
  }

  UpdateProspect updateProspect() {
    return new UpdateProspect()
        .id("prospect1_id")
        .name("paul adams")
        .status(CONTACTED)
        .email("paulAdams@gmail.com")
        .phone("+261340465341")
        .address("30 Rue de la Montagne Sainte-Genevieve");
  }

  ExtendedProspectStatus interestingProspect() {
    return new ExtendedProspectStatus()
        .id(prospect1().getId())
        .name("Interesting prospect")
        .email(prospect1().getEmail())
        .phone(prospect1().getPhone())
        .address(prospect1().getAddress())
        .status(CONTACTED)
        .townCode(prospect1().getTownCode())
        .comment("Prospect to be updated")
        .invoiceID("invoice1_id")
        .contractAmount(2000)
        .prospectFeedback(ProspectFeedback.INTERESTED);
  }

  ExtendedProspectStatus notInterestingProspect() {
    return interestingProspect()
        .prospectFeedback(ProspectFeedback.NOT_INTERESTED);
  }

  ExtendedProspectStatus prospectToReset() {
    return interestingProspect()
        .status(TO_CONTACT);
  }

  Prospect expectedInterestingProspect() {
    Prospect expected =
        ignoreHistoryUpdatedOf(
            prospect1().statusHistory(
                Stream.of(prospect1().getStatusHistory(), getStatusHistory(CONTACTED))
                    .flatMap(List::stream)
                    .toList()));
    return expected
        .name("Interesting prospect")
        .comment("Prospect to be updated")
        .invoiceID("invoice1_id")
        .contractAmount(2000)
        .status(CONTACTED)
        .prospectFeedback(ProspectFeedback.INTERESTED);

  }

  Prospect expectedProspect() {
    List<ProspectStatusHistory> statusHistory =
        Stream.of(getStatusHistory(TO_CONTACT), getStatusHistory(CONTACTED))
            .flatMap(List::stream)
            .toList();
    return new Prospect()
        .name("paul adams")
        .location(null)
        .status(CONTACTED)
        .statusHistory(statusHistory.stream()
            .peek(history -> history.setUpdatedAt(null))
            .collect(Collectors.toList()))
        .email("paulAdams@gmail.com")
        .phone("+261340465341")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .rating(new ProspectRating()
            .value(BigDecimal.valueOf(9.993))
            .lastEvaluation(Instant.parse("2023-01-01T00:00:00.00Z")));
  }

  HMunicipality antony() {
    return HMunicipality.builder()
        .id("43c152ed-89f2-43c5-a3d2-d5cb1fb59f62")
        .name("Antony")
        .code("92002")
        .build();
  }

  @Order(1)
  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void read_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Prospect> actual1 = api.getProspects(ACCOUNTHOLDER_ID, null);
    businessRepository.save(BusinessActivity.builder()
        .accountHolder(joeDoeAccountHolder())
        .primaryActivity(ANTI_HARM)
        .secondaryActivity(null)
        .build());
    List<Prospect> actual2 = api.getProspects(ACCOUNTHOLDER_ID, null);
    String prospectName = "Alyssa";
    List<Prospect> actual3 = api.getProspects(ACCOUNTHOLDER_ID, prospectName);

    assertEquals(2, actual1.size());
    assertEquals(7, actual2.size());
    assertTrue(actual1.containsAll(List.of(prospect1(), prospect2())));
    assertFalse(actual1.contains(prospect3()));
    assertTrue(actual2.containsAll(List.of(prospect1(), prospect2(), prospect3())));
    assertEquals(1, actual3.size());
    assertTrue(actual3.stream()
        .allMatch(
            prospect -> prospect.getName() != null && prospect.getName().contains(prospectName)));
  }

  @Order(2)
  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void update_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Prospect> actual = api.updateProspects(ACCOUNTHOLDER_ID, List.of(updateProspect()));

    assertEquals(List.of(expectedProspect()), ignoreIdsAndHistoryUpdatedOf(actual));
  }

  @Test
  void update_prospects_ko() {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"Prospect." + UNKNOWN_PROSPECT_ID
            + " not found. \"}",
        () -> api.updateProspects(ACCOUNTHOLDER_ID,
            List.of(updateProspect().id(UNKNOWN_PROSPECT_ID))));
  }

  @Order(2)
  @Test
  void update_prospect_status_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    Prospect actualInterestingProspect =
        api.updateProspectsStatus(ACCOUNTHOLDER_ID, prospect1().getId(), interestingProspect());
    Prospect actualNotInterstingProspect =
        api.updateProspectsStatus(ACCOUNTHOLDER_ID, prospect1().getId(), notInterestingProspect());
    Prospect actualResetProspect =
        api.updateProspectsStatus(ACCOUNTHOLDER_ID, prospect1().getId(), prospectToReset());

    Prospect expected =
        ignoreHistoryUpdatedOf(
            prospect1().statusHistory(
                Stream.of(getStatusHistory(CONTACTED),
                        prospect1().getStatusHistory(),
                        getStatusHistory(TO_CONTACT))
                    .flatMap(List::stream)
                    .toList()));
    assertEquals(ignoreHistoryUpdatedOf(expectedInterestingProspect()),
        ignoreHistoryUpdatedOf(actualInterestingProspect));
    assertEquals(expected, ignoreHistoryUpdatedOf(actualNotInterstingProspect));
    /*
    TODO: check why it is not reset correctly
    assertEquals(ignoreHistoryUpdatedOf(
        expected.statusHistory(
            Stream.of(getStatusHistory(TO_CONTACT),
                    expected.getStatusHistory())
                .flatMap(List::stream)
                .toList())), ignoreHistoryUpdatedOf(actualResetProspect));*/
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
    assertThrowsForbiddenException(() -> api.getProspects(NOT_JOE_DOE_ACCOUNT_HOLDER_ID, null));
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

  private List<Prospect> ignoreIdsAndHistoryUpdatedOf(List<Prospect> prospects) {
    return prospects.stream()
        .peek(prospect -> {
          prospect.setId(null);
          Objects.requireNonNull(prospect.getStatusHistory()).forEach(
              history -> history.setUpdatedAt(null)
          );
        })
        .toList();
  }

  private List<Prospect> ignoreHistoryUpdatedOf(List<Prospect> prospects) {
    return prospects.stream()
        .peek(prospect ->
            Objects.requireNonNull(prospect.getStatusHistory()).forEach(
                history -> history.setUpdatedAt(null)
            ))
        .toList();
  }

  private Prospect ignoreHistoryUpdatedOf(Prospect prospect) {
    prospect.getStatusHistory().forEach(
        history -> history.setUpdatedAt(null)
    );
    return prospect;
  }
}
