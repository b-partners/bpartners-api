package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_USER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.model.AccountHolderFeedback;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.model.UpdateAccountHolder;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AccountHolderRepositoryIT extends MockedThirdParties {
  private static final String OTHER_CONCERNED_EMAIL = "concerned@birdia.fr";

  @Autowired private AccountHolderRepository subject;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
  }

  @AfterEach
  void restore_holder() {
    var holder = subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID);
    holder.setOtherConcernedEmail(null);
    subject.save(holder);
  }

  @Test
  void other_concerned_email_is_null_by_default() {
    assertNull(subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID).getOtherConcernedEmail());
  }

  @Test
  void other_concerned_email_is_persisted() {
    var holder = subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID);
    holder.setOtherConcernedEmail(OTHER_CONCERNED_EMAIL);

    subject.save(holder);

    var actual = subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID);
    assertEquals(OTHER_CONCERNED_EMAIL, actual.getOtherConcernedEmail());
  }

  @Test
  void other_concerned_email_can_be_reset_to_null() {
    var holder = subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID);
    holder.setOtherConcernedEmail(OTHER_CONCERNED_EMAIL);
    subject.save(holder);

    holder.setOtherConcernedEmail(null);
    subject.save(holder);

    assertNull(subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID).getOtherConcernedEmail());
  }

  @Test
  void other_concerned_email_survives_company_info_update() throws Exception {
    givenOtherConcernedEmail();

    api()
        .updateCompanyInfo(
            JOE_DOE_USER_ID,
            JOE_DOE_ACCOUNT_ID,
            JOE_DOE_ACCOUNT_HOLDER_ID,
            new CompanyInfo().isSubjectToVat(false).website("https://birdia.fr"));

    assertOtherConcernedEmailUnchanged();
  }

  @Test
  void other_concerned_email_survives_global_info_update() throws Exception {
    givenOtherConcernedEmail();

    api()
        .updateAccountHolderInfo(
            JOE_DOE_USER_ID,
            JOE_DOE_ACCOUNT_ID,
            JOE_DOE_ACCOUNT_HOLDER_ID,
            new UpdateAccountHolder()
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
                        .prospectingPerimeter(28)));

    assertOtherConcernedEmailUnchanged();
  }

  @Test
  void other_concerned_email_survives_feedback_configuration_update() throws Exception {
    givenOtherConcernedEmail();

    api()
        .updateFeedbackConf(
            JOE_DOE_ID,
            JOE_DOE_ACCOUNT_HOLDER_ID,
            new AccountHolderFeedback().feedbackLink("https://feedback.com"));

    assertOtherConcernedEmailUnchanged();
  }

  @Test
  void other_concerned_email_survives_user_save() {
    givenOtherConcernedEmail();

    userRepository.save(userRepository.getById(JOE_DOE_ID));

    assertOtherConcernedEmailUnchanged();
  }

  private UserAccountsApi api() {
    return new UserAccountsApi(TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, null, localPort));
  }

  private void givenOtherConcernedEmail() {
    var holder = subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID);
    holder.setOtherConcernedEmail(OTHER_CONCERNED_EMAIL);
    subject.save(holder);
  }

  private void assertOtherConcernedEmailUnchanged() {
    var actual = subject.findById(JOE_DOE_ACCOUNT_HOLDER_ID);
    assertEquals(OTHER_CONCERNED_EMAIL, actual.getOtherConcernedEmail());
  }
}
