package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_BALANCE;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_CASH_FLOW;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_STATUS;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_SUBJECT_TO_VAT;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_USER_IDENTIFICATION;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_USER_STATUS;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_VERIFICATION_STATUS;
import static app.bpartners.api.service.user.OnboardingService.DEFAULT_VERIFIED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.*;
import app.bpartners.api.service.account.AccountService;
import app.bpartners.api.service.accountholder.AccountHolderService;
import app.bpartners.api.service.user.OnboardingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
class UserServiceIT extends MockedThirdParties {
  private static final String COMPANY_NAME = "user company name";
  @MockBean private EventBridgeClient eventBridgeClientMock;
  @Autowired private OnboardingService onboardingService;
  @Autowired private AccountService accountService;
  @Autowired private AccountHolderService accountHolderService;

  @BeforeEach
  public void setUp() {
    setUpEventBridge(eventBridgeClientMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
  }

  public static User toOnboard() {
    return User.builder()
        .id(String.valueOf(randomUUID()))
        .firstName("User_firstname")
        .lastName("User_lastname")
        .mobilePhoneNumber("+261324063616")
        .email("exemple@email.com")
        .bridgePassword(String.valueOf(randomUUID()))
        .build();
  }

  @Test
  void onboard_user_ok() {
    /*MockedStatic<AuthProvider> authProviderMockedStatic = Mockito.mockStatic(AuthProvider.class);
    authProviderMockedStatic
         .when(AuthProvider::getPrincipal)
         .thenReturn(new Principal(userToOnboard, JOE_DOE_TOKEN));*/
    User userToOnboard = toOnboard();

    OnboardedUser actual = onboardingService.onboardUser(new OnboardUser(userToOnboard, COMPANY_NAME, true));
    User actualUser = actual.getOnboardedUser();
    List<Account> accounts = accountService.getAccountsByUserId(actualUser.getId());
    List<AccountHolder> accountHolders =
        accountHolderService.getAccountHoldersByAccountId(accounts.get(0).getId());

    assertEquals(1, accounts.size());
    assertEquals(1, accountHolders.size());
    assertEquals(actual.getOnboardedAccount(), accounts.get(0));
    assertEquals(actual.getOnboardedAccountHolder(), accountHolders.get(0));
    verifyUserValues(userToOnboard, actualUser);
    verifyAccountValues(actualUser, accounts);
    verifyAccountHolderValues(actualUser, accountHolders);
  }

  private static void verifyUserValues(User userToOnboard, User actual) {
    assertNotNull(actual.getBridgePassword());
    assertNotNull(actual.getId());
    assertEquals(userToOnboard.getFirstName(), actual.getFirstName());
    assertEquals(userToOnboard.getLastName(), actual.getLastName());
    assertEquals(userToOnboard.getEmail(), actual.getEmail());
    assertEquals(DEFAULT_USER_IDENTIFICATION, actual.getIdentificationStatus());
    assertEquals(DEFAULT_USER_STATUS, actual.getStatus());
    assertEquals(DEFAULT_VERIFIED, actual.getIdVerified());
    assertEquals(userToOnboard.getMobilePhoneNumber(), actual.getMobilePhoneNumber());
  }

  private static void verifyAccountValues(User actual, List<Account> accounts) {
    Account account = accounts.get(0);
    assertEquals(actual.getName(), account.getName());
    assertEquals(DEFAULT_BALANCE, account.getAvailableBalance());
    assertEquals(DEFAULT_STATUS, account.getStatus());
  }

  private static void verifyAccountHolderValues(User actual, List<AccountHolder> accountHolders) {
    AccountHolder accountHolder = accountHolders.get(0);
    assertEquals(actual.getEmail(), accountHolder.getEmail());
    assertEquals(actual.getMobilePhoneNumber(), accountHolder.getMobilePhoneNumber());
    assertEquals(DEFAULT_CASH_FLOW, accountHolder.getInitialCashflow());
    assertEquals(COMPANY_NAME, accountHolder.getName());
    assertEquals(DEFAULT_SUBJECT_TO_VAT, accountHolder.isSubjectToVat());
    assertEquals(DEFAULT_VERIFICATION_STATUS, accountHolder.getVerificationStatus());
  }
}
