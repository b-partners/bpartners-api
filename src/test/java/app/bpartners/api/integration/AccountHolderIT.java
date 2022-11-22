package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_SWAN_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.companyBusinessActivity;
import static app.bpartners.api.integration.conf.TestUtils.companyInfo;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccountHolder;
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
@ContextConfiguration(initializers = AccountHolderIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AccountHolderIT {
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

  private static AccountHolder joeDoeAccountHolder() {
    return new AccountHolder()
        .id(joeDoeSwanAccountHolder().getId())
        .verificationStatus(VerificationStatus.VERIFIED)
        .name(joeDoeSwanAccountHolder().getInfo().getName())
        .siren(joeDoeSwanAccountHolder().getInfo().getRegistrationNumber())
        .officialActivityName(joeDoeSwanAccountHolder().getInfo().getBusinessActivity())
        .initialCashflow(6000)
        .companyInfo(new CompanyInfo()
            .phone("+33 6 11 22 33 44")
            .email("numer@hei.school")
            .socialCapital(40000)
            .tvaNumber("FR 32 123456789"))
        .businessActivities(new CompanyBusinessActivity()
            .primary("IT")
            .secondary("TECHNOLOGY"))
        .contactAddress(new ContactAddress()
            .address(joeDoeSwanAccountHolder().getResidencyAddress().getAddressLine1())
            .city(joeDoeSwanAccountHolder().getResidencyAddress().getCity())
            .country(joeDoeSwanAccountHolder().getResidencyAddress().getCountry())
            .postalCode(joeDoeSwanAccountHolder().getResidencyAddress().getPostalCode()))
        // /!\ Deprecated : just use contactAddress
        .address(joeDoeSwanAccountHolder().getResidencyAddress().getAddressLine1())
        .city(joeDoeSwanAccountHolder().getResidencyAddress().getCity())
        .country(joeDoeSwanAccountHolder().getResidencyAddress().getCountry())
        .postalCode(joeDoeSwanAccountHolder().getResidencyAddress().getPostalCode());
  }

  private static AccountHolder expected() {
    return joeDoeAccountHolder()
        .businessActivities(companyBusinessActivity())
        .companyInfo(companyInfo());
  }

  private static CompanyBusinessActivity businessActivityBeforeUpdate() {
    return new CompanyBusinessActivity()
        .primary("IT")
        .secondary("TECHNOLOGY");
  }

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  @Test
  void read_account_holders_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(TestUtils.JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(joeDoeAccountHolder()));
  }

  @Test
  void update_company_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateCompanyInfo(JOE_DOE_SWAN_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), companyInfo());

    assertEquals(expected().businessActivities(businessActivityBeforeUpdate()), actual);
  }

  @Test
  void update_business_activities_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateBusinessActivities(JOE_DOE_SWAN_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), companyBusinessActivity());

    assertEquals(expected(), actual);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
