package app.bpartners.api.integration;

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

import app.bpartners.api.endpoint.rest.api.SecurityApi;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.OnboardedUser;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.model.Whois;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
@Disabled("TODO(fail)")
class UserIT extends MockedThirdParties {
  public static final String JOE_DOE_COGNITO_TOKEN = "joe_doe_cognito_token";
  public static final String OTHER_JOE_ACCOUNT_ID = "other_joe_account_id";
  private static final String API_KEY = "dummy";
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @MockBean private EventBridgeClient eventBridgeClientMock;
  @MockBean private BridgeUserRepository bridgeUserRepositoryMock;
  @Autowired private UserJpaRepository userJpaRepository;

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
        .activeAccount(restJaneAccount())
        .roles(List.of());
  }

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, localPort);
  }

  private ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpEventBridge(eventBridgeClientMock);
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  @Test
  void unauthenticated_get_onboarding_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpResponse<String> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/onboardingInitiation"))
                .header("Access-Control-Request-Method", "POST")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("Origin", "http://localhost:3000")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\n"
                            + "\"redirectionStatusUrls\": {\n"
                            + "    \"successUrl\": \""
                            + REDIRECT_SUCCESS_URL
                            + "\",\n"
                            + "    \"failureUrl\": \""
                            + REDIRECT_FAILURE_URL
                            + "/error\"\n"
                            + "  }"
                            + "}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), response.statusCode());
  }

  @Test
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
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    SecurityApi api = new SecurityApi(janeDoeClient);

    User actualUser = api.whoami().getUser();

    assertEquals(restJaneDoeUser(), actualUser);
  }

  @Test
  void read_user_using_cognito_ok() throws ApiException {
    String email = "joe@email.com";
    when(cognitoComponentMock.getEmailByToken(JOE_DOE_COGNITO_TOKEN)).thenReturn(email);
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeUser(), actualUser);
  }

  @Test
  void read_user_using_cognito_ko() {
    when(cognitoComponentMock.getEmailByToken(JOE_DOE_COGNITO_TOKEN)).thenReturn("jane@email.com");
    ApiClient joeDoeClient = anApiClient(JOE_DOE_COGNITO_TOKEN);

    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getUserById(JOE_DOE_ID));
  }

  @Test
  void persist_user_info_while_reading_ok() throws ApiException {
    String email = "jane@email.com";
    HUser beforeUpdate = userJpaRepository.getByEmail(email);
    beforeUpdate.setAccounts(List.of());
    beforeUpdate.setAccountHolders(List.of());

    ApiClient joeDoeClient = anApiClient(JANE_DOE_TOKEN);
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actual = api.getUserById(JANE_DOE_ID);

    assertEquals(
        HUser.builder()
            .id("jane_doe_id")
            .status(beforeUpdate.getStatus())
            .phoneNumber(beforeUpdate.getPhoneNumber())
            .monthlySubscription(beforeUpdate.getMonthlySubscription())
            .logoFileId(beforeUpdate.getLogoFileId())
            .firstName("Jane")
            .lastName("Doe")
            .email("jane@email.com")
            .idVerified(true)
            .identificationStatus(VALID_IDENTITY)
            .accounts(List.of())
            .accountHolders(List.of())
            .roles(new Role[] {})
            .build(),
        beforeUpdate);
    assertEquals(restJaneDoeUser(), actual);
  }

  @Test
  void read_user_by_id_ok() throws ApiException {
    ApiClient janeDoeClient = anApiClient(JANE_DOE_TOKEN);
    UserAccountsApi api = new UserAccountsApi(janeDoeClient);

    User actualUser = api.getUserById(JANE_DOE_ID);

    assertEquals(restJaneDoeUser(), actualUser);
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
  
  void onboard_users_ok() throws IOException, InterruptedException {
    when(bridgeUserRepositoryMock.createUser(any())).thenReturn(bridgeUser());

    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    OnboardUser toOnboard = onboardUser();
    HttpResponse<String> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/onboarding"))
                .headers("Content-Type", "application/json")
                .headers("Accept", "*/*")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        new ObjectMapper().writeValueAsString(List.of(toOnboard))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    var actual = convertBody(response.body());
    OnboardedUser onboardedUser = actual.get(0);

    assertEquals(
        List.of(
            new OnboardedUser()
                .user(
                    onboardedUser
                        .getUser()
                        .firstName(toOnboard.getFirstName())
                        .lastName(toOnboard.getLastName())
                        .phone(toOnboard.getPhoneNumber()))
                .account(
                    onboardedUser
                        .getAccount()
                        .name(toOnboard.getFirstName() + " " + toOnboard.getLastName()))
                .accountHolder(
                    onboardedUser
                        .getAccountHolder()
                        .name(toOnboard.getCompanyName())
                        .companyInfo(
                            onboardedUser
                                .getAccountHolder()
                                .getCompanyInfo()
                                .email(toOnboard.getEmail())
                                .phone(toOnboard.getPhoneNumber()))
                        .name(toOnboard.getCompanyName()))),
        actual);
  }

  // TODO: check why two attempts with same email are all OK
  @Test
  void onboard_users_ko() throws IOException, InterruptedException {
    when(bridgeUserRepositoryMock.createUser(any())).thenReturn(bridgeUser());

    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    OnboardUser toOnboard = onboardUser();
    HttpResponse<String> firstAttempt =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/onboarding"))
                .headers("Content-Type", "application/json")
                .headers("Accept", "*/*")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        new ObjectMapper().writeValueAsString(List.of(toOnboard))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(HttpStatus.OK.value(), firstAttempt.statusCode());

    HttpResponse<String> secondAttempt =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(URI.create(basePath + "/onboarding"))
                .headers("Content-Type", "application/json")
                .headers("Accept", "*/*")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        new ObjectMapper()
                            .writeValueAsString(List.of(toOnboard.email("joe@email.com")))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertEquals(HttpStatus.BAD_REQUEST.value(), secondAttempt.statusCode());
    assertTrue(
        secondAttempt
            .body()
            .contains(
                "User with email "
                    + toOnboard.getEmail()
                    + " already exists."
                    + " Choose another email address"));
  }

  @Test
  void client_with_api_key_get_user_ok() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/whois/" + JANE_DOE_ID))
            .headers("x-api-key", API_KEY)
            .GET()
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    Whois expected = new Whois().user(restJaneDoeUser());
    Whois actual = objectMapper.readValue(response.body(), Whois.class);

    assertEquals(expected, actual);
    assertEquals(expected.getUser(), actual.getUser());
  }

  @Test
  void client_with_api_key_get_user_ko() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;
    HttpRequest request1 =
        HttpRequest.newBuilder().uri(URI.create(basePath + "/whois/" + JOE_DOE_ID)).GET().build();
    HttpRequest request2 =
        HttpRequest.newBuilder()
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
    CollectionType collectionType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, OnboardedUser.class);
    return objectMapper.readValue(responseBody, collectionType);
  }
}
