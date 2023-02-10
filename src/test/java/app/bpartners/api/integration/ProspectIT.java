package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateProspect;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.integration.conf.TestUtils.NOT_JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.SWAN_ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ProspectIT.ContextInitializer.class)
@AutoConfigureMockMvc
class ProspectIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  Prospect prospect1() {
    return new Prospect()
        .id("prospect1_id")
        .name("john doe")
        .location("paris")
        .status(TO_CONTACT)
        .email("johnDoe@gmail.com")
        .phone("+261340465338");
  }

  Prospect prospect2() {
    return new Prospect()
        .id("prospect2_id")
        .name("jane doe")
        .location("paris")
        .status(TO_CONTACT)
        .email("janeDoe@gmail.com")
        .phone("+261340465339");
  }

  Prospect prospect3() {
    return new Prospect()
        .id("prospect3_id")
        .name("markus adams")
        .location("paris")
        .status(TO_CONTACT)
        .email("markusAdams@gmail.com")
        .phone("+261340465340");
  }

  CreateProspect creatableProspect() {
    return new CreateProspect()
        .name("paul adams")
        .location("paris")
        .status(TO_CONTACT)
        .email("paulAdams@gmail.com")
        .phone("+261340465341");
  }

  Prospect expectedProspect() {
    return new Prospect()
        .name("paul adams")
        .location("paris")
        .status(TO_CONTACT)
        .email("paulAdams@gmail.com")
        .phone("+261340465341");
  }

  @Test
  void read_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Prospect> actual = api.getProspects(SWAN_ACCOUNTHOLDER_ID);

    assertTrue(actual.contains(prospect1()));
    assertTrue(actual.contains(prospect2()));
    assertTrue(actual.contains(prospect3()));
  }

  @Test
  void create_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Prospect> actual = api.createProspects(SWAN_ACCOUNTHOLDER_ID,
        List.of(creatableProspect()));

    assertEquals(List.of(expectedProspect()), ignoreIdsOf(actual));
  }

  @Test
  void convert_prospects_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsApiException("{\"type\":\"501 NOT_IMPLEMENTED\","
            + "\"message\":\"prospect conversion not implemented yet\"}",
        () -> api.convertProspect(SWAN_ACCOUNTHOLDER_ID,
            prospect1().getId(), List.of()));
  }

  @Test
  void joe_doe_access_other_prospects_ko() {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.createProspects(NOT_JOE_DOE_ACCOUNT_HOLDER_ID, List.of()));
    assertThrowsForbiddenException(() -> api.getProspects(NOT_JOE_DOE_ACCOUNT_HOLDER_ID));
    assertThrowsForbiddenException(
        () -> api.convertProspect(NOT_JOE_DOE_ACCOUNT_HOLDER_ID, prospect1().getId(), List.of()));
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
