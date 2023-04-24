package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Marketplace;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.MARKETPLACE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.MARKETPLACE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.NOT_JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = MarketPlaceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class MarketPlaceIT {
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private BuildingPermitConf buildingPermitConf;
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

  Marketplace marketPlace1() {
    return new Marketplace()
        .id(MARKETPLACE1_ID)
        .phoneNumber("+261340465338")
        .name("marketplace1_name")
        .description("marketplace1_description")
        .websiteUrl("website URL")
        .logoUrl("logo URL");
  }

  Marketplace marketPlace2() {
    return new Marketplace()
        .id(MARKETPLACE2_ID)
        .phoneNumber("+261340465338")
        .name("marketplace2_name")
        .description("marketplace2_description")
        .websiteUrl("website URL")
        .logoUrl("logo URL");
  }

  Marketplace defaultMarketPlace1() {
    return new Marketplace()
        .id("428473e5-f075-48b5-9b7f-60035e103db5")
        .name("Marchés publics - Travaux")
        .websiteUrl(
            "https://www.marches-publics.gouv.fr/?page=Entreprise"
                + ".EntrepriseAdvancedSearch&searchAnnCons&type=multicriteres")
        .logoUrl(
            "https://public-logo-resources.s3.eu-west-3.amazonaws.com/place-logo.png");
  }

  Marketplace defaultMarketPlace2() {
    return new Marketplace()
        .id("e09290f0-34f9-40cd-b643-e8afd1a721b4")
        .name("Marchés publics - Services")
        .websiteUrl(
            "https://www.marches-publics.gouv.fr/?page=Entreprise"
                + ".EntrepriseAdvancedSearch&searchAnnCons&type=multicriteres")
        .logoUrl(
            "https://public-logo-resources.s3.eu-west-3.amazonaws.com/place-logo.png");
  }

  @Test
  void read_marketplaces_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Marketplace> actual =
        ignoreDescription(api.getMarketplaces(JOE_DOE_ACCOUNT_ID, 1, 20));
    assertTrue(actual.containsAll(List.of(marketPlace1(), marketPlace2(), defaultMarketPlace1(),
        defaultMarketPlace2())));
  }

  @Test
  void read_marketplaces_ko() {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getMarketplaces(NOT_JOE_DOE_ACCOUNT_ID, 1, 20));
  }

  private List<Marketplace> ignoreDescription(List<Marketplace> marketplaces) {
    marketplaces.forEach(marketplace -> {
      if (marketplace.getId().equals(defaultMarketPlace1().getId())
          || marketplace.getId().equals(defaultMarketPlace2().getId())) {
        marketplace.setDescription(null);
      }
    });
    return marketplaces;
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
