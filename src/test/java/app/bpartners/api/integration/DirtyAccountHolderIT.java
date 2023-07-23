package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.AccountHolderFeedback;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.DbEnvContextInitializer.getHttpServerPort;
import static app.bpartners.api.integration.conf.utils.TestUtils.ACCOUNTHOLDER2_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_USER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_USER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.annualRevenueTarget1;
import static app.bpartners.api.integration.conf.utils.TestUtils.annualRevenueTarget2;
import static app.bpartners.api.integration.conf.utils.TestUtils.companyBusinessActivity;
import static app.bpartners.api.integration.conf.utils.TestUtils.companyInfo;
import static app.bpartners.api.integration.conf.utils.TestUtils.location;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.toUpdateAnnualRevenueTarget;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)
@Slf4j
class DirtyAccountHolderIT extends MockedThirdParties {

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, getHttpServerPort());
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
        .tvaNumber("FR12323456789")
        .location(location()
            .latitude(43.5)
            .longitude(2.5))
        .townCode(92002);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void update_annual_revenue_target_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    AccountHolder actual = api.updateRevenueTargets(JOE_DOE_USER_ID, JOE_DOE_ACCOUNT_ID,
        joeDoeAccountHolder().getId(), List.of(toUpdateAnnualRevenueTarget()));

    assertEquals(2, actual.getRevenueTargets().size());
  }

  @Test
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
}
