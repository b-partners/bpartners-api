package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.SecurityApi;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.OnboardedUser;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.model.Whois;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static app.bpartners.api.integration.UserServiceIT.bridgeUser;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.REDIRECT_FAILURE_URL;
import static app.bpartners.api.integration.conf.utils.TestUtils.REDIRECT_SUCCESS_URL;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.restJaneAccount;
import static app.bpartners.api.integration.conf.utils.TestUtils.restJoeDoeUser;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpEventBridge;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
class UserIT {
  public static final String JOE_DOE_COGNITO_TOKEN = "joe_doe_cognito_token";
  public static final String OTHER_JOE_ACCOUNT_ID = "other_joe_account_id";
  private static final String API_KEY = "dummy";
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private BridgeApi bridgeApi;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private CognitoComponent cognitoComponent;
  @MockBean
  private EventBridgeClient eventBridgeClientMock;
  @MockBean
  private BridgeUserRepository bridgeUserRepositoryMock;
  @Autowired
  private UserJpaRepository userJpaRepository;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, DbEnvContextInitializer.getHttpServerPort());
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, DbEnvContextInitializer.getHttpServerPort());
  }

  public static User restJaneDoeUser() {
    return new User()
        .id(JANE_DOE_ID)
        .firstName("Jane")
        .lastName("Doe")
        .idVerified(true)
        .identificationStatus(VALID_IDENTITY)
        .phone("+261341122334")
        .monthlySubscriptionAmount(5)
        .logoFileId("logo.jpeg")
        .status(ENABLED)
        .activeAccount(restJaneAccount());
  }

  @BeforeEach
  public void setUp() {
    setUpEventBridge(eventBridgeClientMock);
    setUpCognito(cognitoComponent);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  @Test
  void unauthenticated_get_onboarding_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + DbEnvContextInitializer.getHttpServerPort();

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboardingInitiation"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString("{\n"
                + "\"redirectionStatusUrls\": {\n"
                + "    \"successUrl\": \"" + REDIRECT_SUCCESS_URL + "\",\n"
                + "    \"failureUrl\": \"" + REDIRECT_FAILURE_URL + "/error\"\n"
                + "  }"
                + "}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), response.statusCode());
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  void user_change_active_account_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);
    User before = api.getUserById(JOE_DOE_ID);

    User actual = api.setActiveAccount(JOE_DOE_ID, OTHER_JOE_ACCOUNT_ID);

    assertEquals(JOE_DOE_ACCOUNT_ID, before.getActiveAccount().getId());
    assertEquals("other_joe_account_id", actual.getActiveAccount().getId());
  }

  @Test
  void user_change_active_account_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.setActiveAccount(JOE_DOE_ID, JANE_ACCOUNT_ID));
  }

  @Test
  void user_read_own_informations_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    SecurityApi api = new SecurityApi(joeDoeClient);

    User actualUser = api.whoami().getUser();

    assertEquals(restJoeDoeUser(), actualUser);
  }

  @Test
  void read_user_using_cognito_ok() throws ApiException {
    String email = "joe@email.com";
    when(cognitoComponent.getEmailByToken(JOE_DOE_COGNITO_TOKEN))
        .thenReturn(email);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_COGNITO_TOKEN);
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeUser(), actualUser);
  }

  @Test
  void read_user_using_cognito_ko() {
    when(cognitoComponent.getEmailByToken(JOE_DOE_COGNITO_TOKEN))
        .thenReturn("jane@email.com");
    ApiClient joeDoeClient = anApiClient(JOE_DOE_COGNITO_TOKEN);

    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getUserById(JOE_DOE_ID));
  }

  @Test
  void persist_user_info_while_reading_ok()
      throws ApiException {
    String email = "jane@email.com";
    HUser beforeUpdate = userJpaRepository.getByEmail(email);
    beforeUpdate.setAccounts(List.of());
    beforeUpdate.setAccountHolders(List.of());

    ApiClient joeDoeClient = anApiClient(JANE_DOE_TOKEN);
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actual = api.getUserById(JANE_DOE_ID);

    assertEquals(HUser.builder()
        .id("jane_doe_id")
        .status(beforeUpdate.getStatus())
        .phoneNumber(beforeUpdate.getPhoneNumber())
        .monthlySubscription(beforeUpdate.getMonthlySubscription())
        .logoFileId(beforeUpdate.getLogoFileId())
        .firstName(null)
        .lastName(null)
        .email("jane@email.com")
        .idVerified(null)
        .identificationStatus(null)
        .accounts(List.of())
        .accountHolders(List.of())
        .build(), beforeUpdate);
    assertEquals(restJaneDoeUser()
        .firstName(null)
        .lastName(null)
        .idVerified(null)
        .identificationStatus(null), actual);
  }

  @Test
  void read_user_by_id_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeUser(), actualUser);
  }

  @Test
  void read_user_by_id_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getUserById(TestUtils.USER1_ID));
    assertThrowsForbiddenException(() -> api.getUserById(TestUtils.BAD_USER_ID));
  }

  public OnboardUser onboardUser() {
    return new OnboardUser()
        .firstName("Bernard")
        .lastName("Germain")
        .email("bernardgermain@email.com")
        .phoneNumber("+33123456789")
        .companyName("BG Company");
  }

  @Test
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void onboard_users_ok() throws IOException, InterruptedException {
    when(bridgeUserRepositoryMock.createUser(any())).thenReturn(bridgeUser());

    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + DbEnvContextInitializer.getHttpServerPort();

    OnboardUser toOnboard = onboardUser();
    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding"))
            .headers("Content-Type", "application/json")
            .headers("Accept", "*/*")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(toOnboard))))
            .build(), HttpResponse.BodyHandlers.ofString());
    var actual = convertBody(response.body());
    OnboardedUser onboardedUser = actual.get(0);

    assertEquals(List.of(new OnboardedUser()
        .user(onboardedUser.getUser()
            .firstName(toOnboard.getFirstName())
            .lastName(toOnboard.getLastName())
            .phone(toOnboard.getPhoneNumber()))
        .account(onboardedUser.getAccount()
            .name(toOnboard.getFirstName() + " " + toOnboard.getLastName()))
        .accountHolder(onboardedUser.getAccountHolder()
            .name(toOnboard.getCompanyName())
            .companyInfo(onboardedUser.getAccountHolder().getCompanyInfo()
                .email(toOnboard.getEmail())
                .phone(toOnboard.getPhoneNumber()))
            .name(toOnboard.getCompanyName()))), actual);
  }

  //TODO: check why two attempts with same email are all OK
  @Test
  void onboard_users_ko() throws IOException, InterruptedException {
    when(bridgeUserRepositoryMock.createUser(any())).thenReturn(bridgeUser());

    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + DbEnvContextInitializer.getHttpServerPort();

    OnboardUser toOnboard = onboardUser();
    HttpResponse<String> firstAttempt = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding"))
            .headers("Content-Type", "application/json")
            .headers("Accept", "*/*")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(toOnboard))))
            .build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(HttpStatus.OK.value(), firstAttempt.statusCode());

    HttpResponse<String> secondAttempt = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding"))
            .headers("Content-Type", "application/json")
            .headers("Accept", "*/*")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(toOnboard.email("joe@email.com")))))
            .build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(HttpStatus.BAD_REQUEST.value(), secondAttempt.statusCode());
    assertTrue(secondAttempt.body().contains(
        "User with email " + toOnboard.getEmail() + " already exists."
            + " Choose another email address"));
  }

  @Test
  void client_with_api_key_get_user_ok() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + DbEnvContextInitializer.getHttpServerPort();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(basePath + "/whois/" + JOE_DOE_ID))
        .headers("x-api-key", API_KEY)
        .GET()
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    Whois expected = new Whois().user(restJoeDoeUser());
    Whois actual = objectMapper.readValue(response.body(), Whois.class);

    assertEquals(expected, actual);
    assertEquals(expected.getUser(), actual.getUser());
  }

  @Test
  void client_with_api_key_get_user_ko() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + DbEnvContextInitializer.getHttpServerPort();
    HttpRequest request1 = HttpRequest.newBuilder()
        .uri(URI.create(basePath + "/whois/" + JOE_DOE_ID))
        .GET()
        .build();
    HttpRequest request2 = HttpRequest.newBuilder()
        .uri(URI.create(basePath + "/whois/" + JOE_DOE_ID))
        .headers("x-api-key", "api-key")
        .GET()
        .build();

    HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

    assertEquals("{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}", response1.body());
    assertEquals("{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}", response2.body());
  }

  private List<OnboardedUser> convertBody(String responseBody) throws JsonProcessingException {
    CollectionType collectionType = objectMapper.getTypeFactory()
        .constructCollectionType(List.class, OnboardedUser.class);
    return objectMapper.readValue(responseBody, collectionType);
  }
}
