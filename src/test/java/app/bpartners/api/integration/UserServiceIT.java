package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.*;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.account.AccountService;
import app.bpartners.api.service.accountholder.AccountHolderService;
import app.bpartners.api.service.user.OnboardingService;
import app.bpartners.api.service.user.UserService;
import java.time.temporal.ChronoUnit;
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
  @Autowired private UserRepository userRepository;
  @Autowired private UserAnalysisApiKeyRepository userAnalysisApiKeyRepository;
  @Autowired private UserService userService;

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
        .build();
  }

  @Test
  void onboard_user_ok() {
    /*MockedStatic<AuthProvider> authProviderMockedStatic = Mockito.mockStatic(AuthProvider.class);
    authProviderMockedStatic
         .when(AuthProvider::getPrincipal)
         .thenReturn(new Principal(userToOnboard, JOE_DOE_TOKEN));*/
    User userToOnboard = toOnboard();

    OnboardedUser actual =
        onboardingService.onboardUser(new OnboardUser(userToOnboard, COMPANY_NAME, true));
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

  @Test
  public void get_analysis_api_keys_ok() {
    UserAnalysisApiKey userAnalysisApiKey =
        UserAnalysisApiKey.builder()
            .apiKey(randomUUID().toString())
            .creationDatetime(now().truncatedTo(ChronoUnit.MICROS))
            .enabled(true)
            .build();

    User user = User.builder().email("dummy").firstName("dummy").lastName("dummy").build();
    user = userRepository.save(user);
    user.addUserAnalysisApiKey(userAnalysisApiKey);

    var retrievedUser = userRepository.save(user);
    String userId = retrievedUser.getId();

    List<UserAnalysisApiKey> actual = userService.getAnalysisApiKeys(userId);

    assertEquals(userAnalysisApiKey.getApiKey(), actual.getFirst().getApiKey());
    assertEquals(retrievedUser.getAnalysisApiKeys(), actual);
  }

  private static void verifyUserValues(User userToOnboard, User actual) {
    assertNotNull(actual.getId());
    assertEquals(userToOnboard.getFirstName(), actual.getFirstName());
    assertEquals(userToOnboard.getLastName(), actual.getLastName());
    assertEquals(userToOnboard.getEmail(), actual.getEmail());
    assertEquals(ENABLED, actual.getStatus());
    assertEquals(userToOnboard.getMobilePhoneNumber(), actual.getMobilePhoneNumber());
  }

  private static void verifyAccountValues(User actual, List<Account> accounts) {
    Account account = accounts.get(0);
    assertEquals(actual.getName(), account.getName());
    assertEquals(new Money(), account.getAvailableBalance());
  }

  private static void verifyAccountHolderValues(User actual, List<AccountHolder> accountHolders) {
    AccountHolder accountHolder = accountHolders.get(0);
    assertEquals(actual.getEmail(), accountHolder.getEmail());
    assertEquals(actual.getMobilePhoneNumber(), accountHolder.getMobilePhoneNumber());
    assertEquals(new Fraction(), accountHolder.getInitialCashflow());
    assertEquals(COMPANY_NAME, accountHolder.getName());
    assertEquals(true, accountHolder.isSubjectToVat());
  }
}
