package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_USER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.annualRevenueTarget1;
import static app.bpartners.api.integration.conf.utils.TestUtils.annualRevenueTarget2;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.companyBusinessActivity;
import static app.bpartners.api.integration.conf.utils.TestUtils.companyInfo;
import static app.bpartners.api.integration.conf.utils.TestUtils.createAnnualRevenueTarget;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer1;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer2;
import static app.bpartners.api.integration.conf.utils.TestUtils.location;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.FeedbackRequest;
import app.bpartners.api.endpoint.rest.model.UpdateAccountHolder;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.User;
import app.bpartners.api.service.UserService;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
class AccountHolderIT extends MockedThirdParties {
  @Autowired private UserService userService;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  private static AccountHolder joeDoeAccountHolder() {
    return new AccountHolder()
        .id("b33e6eb0-e262-4596-a91f-20c6a7bfd343")
        .verificationStatus(VerificationStatus.VERIFIED)
        .name("NUMER")
        .siren("899067250")
        .officialActivityName("businessAndRetail")
        .initialCashflow(6000)
        .companyInfo(
            new CompanyInfo()
                .isSubjectToVat(true)
                .phone("+33 6 11 22 33 44")
                .email("numer@hei.school")
                .socialCapital(40000)
                .tvaNumber("FR32123456789")
                .location(location())
                .townCode(92002))
        .businessActivities(new CompanyBusinessActivity().primary("IT").secondary("TECHNOLOGY"))
        .contactAddress(
            new ContactAddress()
                .prospectingPerimeter(0)
                .address("6 RUE PAUL LANGEVIN")
                .city("FONTENAY-SOUS-BOIS")
                .country("FRA")
                .postalCode("94120"))
        .revenueTargets(List.of(annualRevenueTarget1(), annualRevenueTarget2()))
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
    return new CreatedFeedbackRequest().customers(List.of(customer1(), customer2()));
  }

  private static AccountHolder expected() {
    return joeDoeAccountHolder()
        .businessActivities(companyBusinessActivity())
        .contactAddress(
            new ContactAddress()
                .address("6 RUE PAUL LANGEVIN")
                .city("FONTENAY-SOUS-BOIS")
                .country("FRA")
                .postalCode("94120")
                .prospectingPerimeter(0))
        .companyInfo(
            companyInfo()
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
        .tvaNumber("FR12323456789")
        .location(location().latitude(43.5).longitude(2.5))
        .townCode(92002);
  }

  UpdateAccountHolder globalInfo() {
    return new UpdateAccountHolder()
        .name("NUMER")
        .siren("FR123456789")
        .initialCashFlow(5000)
        .officialActivityName("businessAndRetail")
        .contactAddress(
            new ContactAddress()
                .address("Rue 91, Charles de Gaulle")
                .city("Paris")
                .postalCode("9100")
                .country("France")
                .prospectingPerimeter(28));
  }

  UpdateAccountHolder expectedGlobalInfo() {
    return new UpdateAccountHolder()
        .name("NUMER")
        .siren("FR123456789")
        .initialCashFlow(5000)
        .officialActivityName("businessAndRetail")
        .contactAddress(
            new ContactAddress()
                .address("Rue 91, Charles de Gaulle")
                .city("Paris")
                .postalCode("9100")
                .country("France")
                .prospectingPerimeter(28));
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  
  @Test
  void get_all_account_holders_ok() throws ApiException {
    User user = userService.getUserById(JOE_DOE_ID);
    userService.saveUser(user.toBuilder().roles(List.of(Role.EVAL_PROSPECT)).build());
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    String nameFilter1 = "NUMER";
    String nameFilter2 = "oth";

    List<AccountHolder> actualAll = api.getAllAccountHolders(null, null, null);
    List<AccountHolder> actualByName1 = api.getAllAccountHolders(nameFilter1, null, null);
    List<AccountHolder> actualByName2 = api.getAllAccountHolders(nameFilter2, null, null);

    assertEquals(3, actualAll.size());
    assertTrue(
        actualByName1.stream()
            .allMatch(
                accountHolder ->
                    Objects.requireNonNull(accountHolder.getName().toLowerCase())
                        .contains(nameFilter1.toLowerCase())));
    assertTrue(
        actualByName2.stream()
            .allMatch(
                accountHolder ->
                    Objects.requireNonNull(accountHolder.getName().toLowerCase())
                        .contains(nameFilter2.toLowerCase())));
  }

  @Test
  void get_all_account_holders_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getAllAccountHolders(null, null, null));
  }

  @Test
  void create_annual_revenue_target_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual =
        api.updateRevenueTargets(
            JOE_DOE_USER_ID,
            JOE_DOE_ACCOUNT_ID,
            joeDoeAccountHolder().getId(),
            List.of(createAnnualRevenueTarget()));

    assertEquals(3, actual.getRevenueTargets().size());
  }

  @Test
  void update_annual_revenue_target_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\"2024 is duplicated.\"}",
        () ->
            api.updateRevenueTargets(
                JOE_DOE_USER_ID,
                JOE_DOE_ACCOUNT_ID,
                joeDoeAccountHolder().getId(),
                List.of(
                    new CreateAnnualRevenueTarget().year(2024).amountTarget(15000),
                    new CreateAnnualRevenueTarget().year(2024).amountTarget(11000))));
  }

  @Test
  void update_global_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actualUpdated =
        api.updateAccountHolderInfo(
            JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID, ACCOUNTHOLDER_ID, globalInfo());

    assertEquals(ACCOUNTHOLDER_ID, actualUpdated.getId());
    assertEquals(expectedGlobalInfo().getContactAddress(), actualUpdated.getContactAddress());
  }

  @Test
  void add_feedback_link_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actualAddedFeedbackLink =
        api.updateFeedbackConf(
            JOE_DOE_ID,
            JOE_DOE_ACCOUNT_HOLDER_ID,
            new AccountHolderFeedback().feedbackLink("https://feedback.com"));
    AccountHolder actualUpdatedFeedbackLink =
        api.updateFeedbackConf(
            JOE_DOE_ID,
            JOE_DOE_ACCOUNT_HOLDER_ID,
            new AccountHolderFeedback().feedbackLink("https://updateFeedbackLink.com"));
    AccountHolder actualNoFeedbackLink =
        api.updateFeedbackConf(JOE_DOE_ID, JOE_DOE_ACCOUNT_HOLDER_ID, new AccountHolderFeedback());

    assertEquals(JOE_DOE_ACCOUNT_HOLDER_ID, actualAddedFeedbackLink.getId());
    assertEquals("https://feedback.com", actualAddedFeedbackLink.getFeedback().getFeedbackLink());
    assertEquals(
        "https://updateFeedbackLink.com",
        actualUpdatedFeedbackLink.getFeedback().getFeedbackLink());
    assertNull(actualNoFeedbackLink.getFeedback().getFeedbackLink());
  }

  @Test
  void read_verified_account_holders_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(TestUtils.JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(joeDoeAccountHolder()));
  }

  @Test
  void ask_feedback_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    CreatedFeedbackRequest actualCreatedFeedbackRequest =
        api.askFeedback(JOE_DOE_ID, JOE_DOE_ACCOUNT_HOLDER_ID, feedbackRequest());

    List<Customer> actualCustomers =
        actualCreatedFeedbackRequest.getCustomers().stream()
            .map(
                customer -> {
                  customer.updatedAt(null);
                  customer.createdAt(null);
                  return customer;
                })
            .toList();
    actualCreatedFeedbackRequest.customers(actualCustomers);

    assertEquals(
        expectedCreatedFeedbackRequest()
            .id(actualCreatedFeedbackRequest.getId())
            .creationDatetime(actualCreatedFeedbackRequest.getCreationDatetime()),
        actualCreatedFeedbackRequest);
  }
}
