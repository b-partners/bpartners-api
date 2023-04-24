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
import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.endpoint.rest.model.UpdateAccountHolder;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
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
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.implementation.AccountHolderSwanRepositoryImpl;
import app.bpartners.api.repository.swan.model.SwanAccount;
import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import app.bpartners.api.repository.swan.response.AccountHolderResponse;
import app.bpartners.api.repository.swan.response.AccountResponse;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.ACCOUNTHOLDER2_ID;
import static app.bpartners.api.integration.conf.TestUtils.ACCOUNT_OPENED;
import static app.bpartners.api.integration.conf.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_SWAN_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_SWAN_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.SWAN_ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.VERIFIED_STATUS;
import static app.bpartners.api.integration.conf.TestUtils.annualRevenueTarget1;
import static app.bpartners.api.integration.conf.TestUtils.annualRevenueTarget2;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.companyBusinessActivity;
import static app.bpartners.api.integration.conf.TestUtils.companyInfo;
import static app.bpartners.api.integration.conf.TestUtils.createAnnualRevenueTarget;
import static app.bpartners.api.integration.conf.TestUtils.janeSwanAccount;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccount;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccountHolder;
import static app.bpartners.api.integration.conf.TestUtils.location;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.toUpdateAnnualRevenueTarget;
import static app.bpartners.api.model.mapper.AccountHolderMapper.NOT_STARTED_STATUS;
import static app.bpartners.api.model.mapper.AccountHolderMapper.PENDING_STATUS;
import static app.bpartners.api.model.mapper.AccountHolderMapper.WAITING_FOR_INFORMATION_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AccountHolderIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountHolderIT {
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
  private SwanApi swanApiMock;
  private SwanCustomApi swanCustomApiMock;
  private AccountHolderSwanRepositoryImpl accountHolderRepository;

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
            .address(joeDoeSwanAccountHolder().getResidencyAddress().getAddressLine1())
            .city(joeDoeSwanAccountHolder().getResidencyAddress().getCity())
            .country(joeDoeSwanAccountHolder().getResidencyAddress().getCountry())
            .postalCode(joeDoeSwanAccountHolder().getResidencyAddress().getPostalCode()))
        .revenueTargets(List.of(
            annualRevenueTarget1(),
            annualRevenueTarget2()))
        // /!\ Deprecated : just use contactAddress
        .address(joeDoeSwanAccountHolder().getResidencyAddress().getAddressLine1())
        .city(joeDoeSwanAccountHolder().getResidencyAddress().getCity())
        .country(joeDoeSwanAccountHolder().getResidencyAddress().getCountry())
        .postalCode(joeDoeSwanAccountHolder().getResidencyAddress().getPostalCode());
  }

  public static AccountHolderResponse.Edge baseHolderResponse(
      String verificationStatus, String accountId) {
    SwanAccountHolder swanAccountHolder =
        joeDoeSwanAccountHolder().toBuilder()
            .verificationStatus(verificationStatus)
            .accounts(SwanAccountHolder.Accounts.builder()
                .edges(List.of(AccountResponse.Edge.builder()
                    .node(SwanAccount.builder()
                        .id(accountId)
                        .statusInfo(SwanAccount.StatusInfo.builder()
                            .status(ACCOUNT_OPENED)
                            .build())
                        .build())
                    .build()))
                .build())
            .build();
    return AccountHolderResponse.Edge.builder()
        .node(swanAccountHolder)
        .build();
  }

  public static AccountHolderResponse.Edge verifiedAccountholder() {
    return baseJoeHolder(VERIFIED_STATUS);
  }

  public static AccountHolderResponse.Edge baseJoeHolder(String verificationStatus) {
    return baseHolderResponse(verificationStatus, joeDoeSwanAccount().getId());
  }

  public static AccountHolderResponse.Edge notStartedAccountHolder() {
    return baseJoeHolder(NOT_STARTED_STATUS);
  }

  public static AccountHolderResponse.Edge pendingAccountHolder() {
    return baseJoeHolder(PENDING_STATUS);
  }

  public static AccountHolderResponse.Edge waitingAccountHolder() {
    return baseJoeHolder(WAITING_FOR_INFORMATION_STATUS);
  }

  public static AccountHolderResponse.Edge baseJaneHolder(String verificationStatus) {
    return baseHolderResponse(verificationStatus, janeSwanAccount().getId());
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
            .townCode(92002));
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
        .name(joeDoeSwanAccountHolder().getInfo().getName())
        .siren("FR123456789")
        .initialCashFlow(5000)
        .officialActivityName(joeDoeSwanAccountHolder().getInfo().getBusinessActivity())
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
        .name(joeDoeSwanAccountHolder().getInfo().getName())
        .siren("FR123456789")
        .initialCashFlow(5000)
        .officialActivityName(joeDoeSwanAccountHolder().getInfo().getBusinessActivity())
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
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    swanApiMock = mock(SwanApi.class);
    accountHolderRepository = new AccountHolderSwanRepositoryImpl(swanApiMock, swanCustomApiMock);
  }

  @Order(1)
  @Test
  void read_verified_account_holders_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(TestUtils.JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(joeDoeAccountHolder()));
  }

  @Order(1)
  @Test
  void read_multiple_verified_account_holders_ok() throws ApiException {
    setUpSwanApi(swanApiMock,
        verifiedAccountholder(),
        baseJaneHolder(VERIFIED_STATUS),
        notStartedAccountHolder());
    setUpRepositoryMock();
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(TestUtils.JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(joeDoeAccountHolder()));
  }

  @Order(1)
  @Test
  void read_unique_unverified_account_holders_ok() throws ApiException {
    setUpSwanApi(swanApiMock, notStartedAccountHolder());
    setUpRepositoryMock();
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(TestUtils.JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(
        joeDoeAccountHolder().verificationStatus(VerificationStatus.NOT_STARTED)));
  }

  @Order(2)
  @Test
  void read_multiple_account_holders_ok() throws ApiException {
    setUpSwanApi(swanApiMock,
        verifiedAccountholder(),
        notStartedAccountHolder(),
        pendingAccountHolder(),
        waitingAccountHolder());
    setUpRepositoryMock();
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertEquals(1, actual.size());
    assertEquals(joeDoeAccountHolder(), actual.get(0));
  }

  @Order(6)
  @Test
  void read_multiple_account_holders_ko() {
    setUpSwanApi(swanApiMock,
        verifiedAccountholder(),
        verifiedAccountholder(),
        notStartedAccountHolder(),
        pendingAccountHolder(),
        waitingAccountHolder());
    setUpRepositoryMock();
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":"
            + "\"One account with one verified account holder is supported for now"
            + " but following verified account holders are found : accountHolder."
            + verifiedAccountholder().getNode().getId() + "\"}",
        () -> api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID));

  }

  @Order(6)
  @Test
  void read_multiple_unverified_account_holders_ko() {
    setUpSwanApi(swanApiMock,
        notStartedAccountHolder(),
        pendingAccountHolder(),
        waitingAccountHolder());
    setUpRepositoryMock();
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":"
            + "\"Only one unverified account holder is supported for now"
            + " but following unverified accountHolders are present : "
            + "accountHolder.b33e6eb0-e262-4596-a91f-20c6a7bfd343,"
            + " accountHolder.b33e6eb0-e262-4596-a91f-20c6a7bfd343,"
            + " accountHolder.b33e6eb0-e262-4596-a91f-20c6a7bfd343" + "\"}",
        () -> api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID));
  }

  @Order(3)
  @Test
  void read_empty_account_holders_ko() {
    setUpSwanApi(swanApiMock);
    setUpRepositoryMock();
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"500 INTERNAL_SERVER_ERROR\","
            + "\"message\":\"One account should have at least one account holder\"}",
        () -> api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID));
  }

  @Order(6)
  @Test
  void update_company_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateCompanyInfo(JOE_DOE_SWAN_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), companyInfo()
            .location(location()
                .latitude(43.5)
                .longitude(2.5))
            .townCode(92002));

    assertEquals(expected()
        .companyInfo(updatedCompanyInfo())
        .businessActivities(new CompanyBusinessActivity()
            .primary(companyBusinessActivity().getPrimary())
            .secondary(companyBusinessActivity().getSecondary())), actual);
    //TODO: check the vat number overriding
    //    assertNotEquals(actual.getCompanyInfo().getTvaNumber(),
    //        companyInfo().getTvaNumber());
  }

  @Order(4)
  @Test
  void update_business_activities_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateBusinessActivities(JOE_DOE_SWAN_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), companyBusinessActivity());
    AccountHolder actual1 = api.updateBusinessActivities(JANE_DOE_SWAN_USER_ID, JANE_ACCOUNT_ID,
        ACCOUNTHOLDER2_ID, new CompanyBusinessActivity()
            .primary("IT"));

    assertEquals("IT", actual1.getBusinessActivities().getPrimary());
    assertEquals(expected()
            .companyInfo(
                expected().getCompanyInfo()
                    .isSubjectToVat(true)),
        actual);
  }

  @Order(8)
  @Test
  void create_annual_revenue_target_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateRevenueTargets(JOE_DOE_SWAN_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), List.of(createAnnualRevenueTarget()));

    assertEquals(3, actual.getRevenueTargets().size());
  }

  @Order(9)
  @Test
  void update_annual_revenue_target_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateRevenueTargets(JOE_DOE_SWAN_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), List.of(toUpdateAnnualRevenueTarget()));

    assertEquals(3, actual.getRevenueTargets().size());
  }

  @Order(10)
  @Test
  void update_annual_revenue_target_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\"2024 is duplicated.\"}",
        () -> api.updateRevenueTargets(JOE_DOE_SWAN_USER_ID, JOE_DOE_ACCOUNT_ID,
            joeDoeAccountHolder().getId(), List.of(
                new CreateAnnualRevenueTarget()
                    .year(2024)
                    .amountTarget(15000),
                new CreateAnnualRevenueTarget()
                    .year(2024)
                    .amountTarget(11000))));
  }

  @Order(11)
  @Test
  void update_global_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actualUpdated = api.updateAccountHolderInfo(JOE_DOE_SWAN_USER_ID,
        JOE_DOE_ACCOUNT_ID, SWAN_ACCOUNTHOLDER_ID, globalInfo());

    assertEquals(SWAN_ACCOUNTHOLDER_ID, actualUpdated.getId());
    assertEquals(expectedGlobalInfo().getContactAddress(), actualUpdated.getContactAddress());
  }

  private void setUpSwanApi(SwanApi swanApi, AccountHolderResponse.Edge... edges) {
    when(swanApi.getData(eq(AccountHolderResponse.class), any(String.class)))
        .thenReturn(
            AccountHolderResponse.builder()
                .data(
                    AccountHolderResponse.Data.builder()
                        .accountHolders(
                            AccountHolderResponse.AccountHolders.builder()
                                .edges(new ArrayList<>() {
                                  {
                                    this.addAll(List.of(edges));
                                  }
                                }).build()
                        ).build()
                ).build()
        );
  }


  private void setUpRepositoryMock() {
    when(accountHolderRepositoryMock.findAllByBearerAndAccountId(any(String.class),
        any(String.class)))
        .thenAnswer(
            invocation ->
                accountHolderRepository
                    .findAllByBearerAndAccountId(invocation.getArgument(0),
                        invocation.getArgument(1))

        );
    when(accountHolderRepositoryMock.findAllByAccountId(any(String.class)))
        .thenAnswer(
            invocation ->
                accountHolderRepository
                    .findAllByAccountId(invocation.getArgument(0))
        );
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
