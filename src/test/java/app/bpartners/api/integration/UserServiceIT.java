package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.OnboardingSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.OnboardingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserServiceIT.ContextInitializer.class)
class UserServiceIT {
  private static final String COMPANY_NAME = "user company name";
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
  private OnboardingSwanRepository onboardingSwanRepository;
  @MockBean
  private CognitoComponent cognitoComponent;
  @MockBean
  private AccountHolderSwanRepository accountHolderSwanRepository;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private BridgeBankRepository bridgeBankRepositoryMock;
  @MockBean
  private BridgeApi bridgeApi;
  @MockBean
  private EventBridgeClient eventBridgeClientMock;
  @Autowired
  private OnboardingService onboardingService;
  @MockBean
  private BridgeUserRepository bridgeUserRepositoryMock;
  @Autowired
  private AccountService accountService;
  @Autowired
  private AccountHolderService accountHolderService;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpEventBridge(eventBridgeClientMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  public static BridgeUser bridgeUser() {
    return BridgeUser.builder()
        .email(toOnboard().getEmail())
        .uuid(String.valueOf(randomUUID()))
        .build();
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
    MockedStatic<AuthProvider> authProviderMockedStatic = Mockito.mockStatic(AuthProvider.class);
    authProviderMockedStatic.when(AuthProvider::getPrincipal)
        .thenReturn(new Principal(toOnboard(), JOE_DOE_TOKEN));
    when(bridgeApi.findItemsByToken(any())).thenReturn(List.of(new BridgeItem()));
    when(bridgeBankRepositoryMock.refreshBankConnection(any(), any())).thenReturn("success");
    when(bridgeUserRepositoryMock.createUser(any())).thenReturn(bridgeUser());

    User actual = onboardingService.onboardUser(toOnboard(), COMPANY_NAME);
    List<Account> accounts =
        accountService.getAccountsByUserId(actual.getId());
    List<AccountHolder> accountHolders =
        accountHolderService.getAccountHoldersByAccountId(accounts.get(0).getId());

    assertNotNull(actual.getBridgePassword());
    assertNotNull(actual.getId());
    assertEquals(toOnboard().getFirstName(), actual.getFirstName());
    assertEquals(toOnboard().getLastName(), actual.getLastName());
    assertEquals(toOnboard().getEmail(), actual.getEmail());
    assertEquals(toOnboard().getMobilePhoneNumber(), actual.getMobilePhoneNumber());
    assertEquals(1, accounts.size());
    assertEquals(1, accountHolders.size());
    Account account = accounts.get(0);
    AccountHolder accountHolder = accountHolders.get(0);
    assertEquals(actual.getName(), account.getName());
    assertEquals(COMPANY_NAME, accountHolder.getName());
    assertEquals(actual.getEmail(), accountHolder.getEmail());
    assertEquals(actual.getMobilePhoneNumber(), accountHolder.getMobilePhoneNumber());

  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}