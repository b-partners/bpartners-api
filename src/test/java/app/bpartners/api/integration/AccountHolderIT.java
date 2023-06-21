package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.AccountHolderFeedback;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.endpoint.rest.model.CreatedFeedbackRequest;
import app.bpartners.api.endpoint.rest.model.FeedbackRequest;
import app.bpartners.api.endpoint.rest.model.UpdateAccountHolder;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import static app.bpartners.api.integration.conf.TestUtils.ACCOUNTHOLDER2_ID;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.annualRevenueTarget1;
import static app.bpartners.api.integration.conf.TestUtils.annualRevenueTarget2;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.companyBusinessActivity;
import static app.bpartners.api.integration.conf.TestUtils.companyInfo;
import static app.bpartners.api.integration.conf.TestUtils.createAnnualRevenueTarget;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static app.bpartners.api.integration.conf.TestUtils.customer2;
import static app.bpartners.api.integration.conf.TestUtils.location;
import static app.bpartners.api.integration.conf.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.toUpdateAnnualRevenueTarget;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AccountHolderIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class AccountHolderIT {
  @MockBean
  private BridgeApi bridgeApi;
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
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private AccountConnectorRepository accountConnectorRepository;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private EventBridgeClient eventBridgeClientMock;
  @MockBean
  private CognitoComponent cognitoComponentMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  private static AccountHolder joeDoeAccountHolder() {
    return new AccountHolder()
        .id("b33e6eb0-e262-4596-a91f-20c6a7bfd343")
        .verificationStatus(VerificationStatus.VERIFIED)
        .name("NUMER")
        .siren("899067250")
        .officialActivityName("businessAndRetail")
        .initialCashflow(6000)
        .companyInfo(new CompanyInfo()
            .isSubjectToVat(true)
            .phone("+33 6 11 22 33 44")
            .email("numer@hei.school")
            .socialCapital(40000)
            .tvaNumber("FR32123456789")
            .location(location())
            .townCode(92002))
        .businessActivities(new CompanyBusinessActivity()
            .primary("IT")
            .secondary("TECHNOLOGY"))
        .contactAddress(new ContactAddress()
            .prospectingPerimeter(0)
            .address("6 RUE PAUL LANGEVIN")
            .city("FONTENAY-SOUS-BOIS")
            .country("FRA")
            .postalCode("94120"))
        .revenueTargets(List.of(
            annualRevenueTarget1(),
            annualRevenueTarget2()))
        // /!\ Deprecated : just use contactAddress
        .address("6 RUE PAUL LANGEVIN")
        .city("FONTENAY-SOUS-BOIS")
        .country("FRA")
        .postalCode("94120")
        .feedback(new AccountHolderFeedback().feedbackLink("feedback link"));
  }


  public static FeedbackRequest feedbackRequest() {
    return new FeedbackRequest()
        .subject("JOE DOE - Ask Feedback")
        .message("message text")
        .attachments(null)
        .customerIds(List.of("customer1_id", "customer2_id"));
  }

  public static CreatedFeedbackRequest expectedCreatedFeedbackRequest() {
    return new CreatedFeedbackRequest()
        .customers(List.of(customer1(), customer2()));
  }

  private static AccountHolder expected() {
    return joeDoeAccountHolder()
        .businessActivities(companyBusinessActivity())
        .contactAddress(new ContactAddress()
            .address("6 RUE PAUL LANGEVIN")
            .city("FONTENAY-SOUS-BOIS")
            .country("FRA")
            .postalCode("94120")
            .prospectingPerimeter(0))
        .companyInfo(companyInfo()
            .phone("+33 6 11 22 33 44")
            .email("numer@hei.school")
            .tvaNumber("FR32123456789")
            .location(location())
            .townCode(92002))
        .feedback(new AccountHolderFeedback().feedbackLink("feedback link"));
  }

  CompanyInfo updatedCompanyInfo() {
    return new CompanyInfo()
        .isSubjectToVat(false)
        .email(companyInfo().getEmail())
        .phone(companyInfo().getPhone())
        .socialCapital(companyInfo().getSocialCapital())
        .tvaNumber(joeDoeAccountHolder().getCompanyInfo().getTvaNumber())
        .location(location()
            .latitude(43.5)
            .longitude(2.5))
        .townCode(92002);
  }

  UpdateAccountHolder globalInfo() {
    return new UpdateAccountHolder()
        .name("NUMER")
        .siren("FR123456789")
        .initialCashFlow(5000)
        .officialActivityName("businessAndRetail")
        .contactAddress(new ContactAddress()
            .address("Rue 91, Charles de Gaulle")
            .city("Paris")
            .postalCode("9100")
            .country("France")
            .prospectingPerimeter(28)
        );
  }

  UpdateAccountHolder expectedGlobalInfo() {
    return new UpdateAccountHolder()
        .name("NUMER")
        .siren("FR123456789")
        .initialCashFlow(5000)
        .officialActivityName("businessAndRetail")
        .contactAddress(new ContactAddress()
            .address("Rue 91, Charles de Gaulle")
            .city("Paris")
            .postalCode("9100")
            .country("France")
            .prospectingPerimeter(28)
        );
  }


  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void read_verified_account_holders_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(TestUtils.JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(joeDoeAccountHolder()));
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void update_company_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateCompanyInfo(JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), companyInfo()
            .location(location()
                .latitude(43.5)
                .longitude(2.5))
            .townCode(92002));

    assertEquals(expected()
        .companyInfo(updatedCompanyInfo())
        .businessActivities(new CompanyBusinessActivity()
            .primary("IT")
            .secondary("TECHNOLOGY"))
        //TODO: check why revenue targets does not work properly for this test
        .revenueTargets(actual.getRevenueTargets()), actual);
    //TODO: check the vat number overriding
    //    assertNotEquals(actual.getCompanyInfo().getTvaNumber(),
    //        companyInfo().getTvaNumber());
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void update_business_activities_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateBusinessActivities(JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), companyBusinessActivity());
    AccountHolder actual1 = api.updateBusinessActivities(JANE_DOE_USER_ID, JANE_ACCOUNT_ID,
        ACCOUNTHOLDER2_ID, new CompanyBusinessActivity()
            .primary("IT"));

    assertEquals("IT", actual1.getBusinessActivities().getPrimary());
    assertEquals(expected()
            .companyInfo(
                expected().getCompanyInfo()
                    .isSubjectToVat(true)),
        actual);
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void create_annual_revenue_target_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateRevenueTargets(JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), List.of(createAnnualRevenueTarget()));

    assertEquals(3, actual.getRevenueTargets().size());
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void update_annual_revenue_target_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateRevenueTargets(JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), List.of(toUpdateAnnualRevenueTarget()));

    assertEquals(2, actual.getRevenueTargets().size());
  }

  @Test
  void update_annual_revenue_target_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\"2024 is duplicated.\"}",
        () -> api.updateRevenueTargets(JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID,
            joeDoeAccountHolder().getId(), List.of(
                new CreateAnnualRevenueTarget()
                    .year(2024)
                    .amountTarget(15000),
                new CreateAnnualRevenueTarget()
                    .year(2024)
                    .amountTarget(11000))));
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void update_global_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actualUpdated = api.updateAccountHolderInfo(JOE_DOE_USER_ID,
        JOE_DOE_ACCOUNT_ID, ACCOUNTHOLDER_ID, globalInfo());

    assertEquals(ACCOUNTHOLDER_ID, actualUpdated.getId());
    assertEquals(expectedGlobalInfo().getContactAddress(), actualUpdated.getContactAddress());
  }

  @Test
  void add_feedback_link_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actualAddedFeedbackLink =
        api.updateFeedbackConf(JOE_DOE_ID, JOE_DOE_ACCOUNT_HOLDER_ID,
            new AccountHolderFeedback().feedbackLink("https://feedback.com"));
    AccountHolder actualUpdatedFeedbackLink =
        api.updateFeedbackConf(JOE_DOE_ID, JOE_DOE_ACCOUNT_HOLDER_ID,
            new AccountHolderFeedback().feedbackLink("https://updateFeedbackLink.com"));
    AccountHolder actualNoFeedbackLink =
        api.updateFeedbackConf(JOE_DOE_ID, JOE_DOE_ACCOUNT_HOLDER_ID,
            new AccountHolderFeedback());

    assertEquals(JOE_DOE_ACCOUNT_HOLDER_ID, actualAddedFeedbackLink.getId());
    assertEquals("https://feedback.com", actualAddedFeedbackLink.getFeedback().getFeedbackLink());
    assertEquals("https://updateFeedbackLink.com",
        actualUpdatedFeedbackLink.getFeedback().getFeedbackLink());
    assertNull(actualNoFeedbackLink.getFeedback().getFeedbackLink());
  }

  @Test
  void ask_feedback_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    CreatedFeedbackRequest actualCreatedFeedbackRequest =
        api.askFeedback(JOE_DOE_ID, JOE_DOE_ACCOUNT_HOLDER_ID, feedbackRequest());

    assertEquals(expectedCreatedFeedbackRequest()
            .id(actualCreatedFeedbackRequest.getId())
            .creationDatetime(actualCreatedFeedbackRequest.getCreationDatetime())
        , actualCreatedFeedbackRequest);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
