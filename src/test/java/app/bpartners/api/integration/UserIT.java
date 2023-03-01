package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.SecurityApi;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.OnboardingSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanUser;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.INSUFFICIENT_DOCUMENT_QUALITY;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.INVALID_IDENTITY;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.PROCESSING;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.UNINITIATED;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JANE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_FAILURE_URL;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_SUCCESS_URL;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.janeDoe;
import static app.bpartners.api.integration.conf.TestUtils.joeDoe;
import static app.bpartners.api.integration.conf.TestUtils.restJaneDoeUser;
import static app.bpartners.api.integration.conf.TestUtils.restJoeDoeUser;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpOnboardingSwanRepositoryMock;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static app.bpartners.api.model.mapper.UserMapper.INSUFFICIENT_DOCUMENT_QUALITY_STATUS;
import static app.bpartners.api.model.mapper.UserMapper.INVALID_IDENTITY_STATUS;
import static app.bpartners.api.model.mapper.UserMapper.PROCESSING_STATUS;
import static app.bpartners.api.model.mapper.UserMapper.UNINITIATED_STATUS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class UserIT {
  public static final String UNKNOWN_IDENTIFICATION_STATUS = "Unknown";
  public static final String JOE_DOE_COGNITO_TOKEN = "joe_doe_cognito_token";
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
  private SwanComponent swanComponentMock;
  @MockBean
  private OnboardingSwanRepository onboardingSwanRepositoryMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private CognitoComponent cognitoComponent;
  @Autowired
  private UserJpaRepository userJpaRepository;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpOnboardingSwanRepositoryMock(onboardingSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @Test
  void unauthenticated_get_onboarding_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

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

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }
  // /!\ The swan project access token provided by AWS SSM seems to not support two calls in a
  // same test
  /*@Test
  void unauthenticated_get_onboarding_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + UserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/onboarding"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString("{\n"
                + "  \"successUrl\": \"" + BAD_REDIRECT_URL + "\",\n"
                + "  \"failureUrl\": \"string\"\n"
                + "}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }*/

  @Test
  void user_read_own_informations_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    SecurityApi api = new SecurityApi(joeDoeClient);

    User actualUser = api.whoami().getUser();

    assertEquals(restJoeDoeUser(), actualUser);
  }

  @Test
  void read_user_using_cognito_ok()
      throws ApiException, URISyntaxException, IOException, InterruptedException {
    String phoneNumber = "+261340465338";
    reset(swanComponentMock);
    when(swanComponentMock.getSwanUserIdByToken(any())).thenReturn(null);
    when(swanComponentMock.getSwanUserByToken(any())).thenReturn(null);
    when(cognitoComponent.getPhoneNumberByToken(JOE_DOE_COGNITO_TOKEN))
        .thenReturn(phoneNumber);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_COGNITO_TOKEN);
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeUser()
        .idVerified(false)
        .identificationStatus(PROCESSING), actualUser);
  }

  @Test
  void read_user_using_cognito_ko()
      throws URISyntaxException, IOException, InterruptedException {
    String phoneNumber = "+261341122334";
    reset(swanComponentMock);
    when(swanComponentMock.getSwanUserIdByToken(any())).thenReturn(null);
    when(swanComponentMock.getSwanUserByToken(any())).thenReturn(null);
    when(cognitoComponent.getPhoneNumberByToken(JOE_DOE_COGNITO_TOKEN))
        .thenReturn(phoneNumber);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_COGNITO_TOKEN);

    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getUserById(JOE_DOE_ID));
  }

  @Test
  void persist_user_info_while_reading_ok()
      throws URISyntaxException, IOException, InterruptedException, ApiException {
    String phoneNumber = "+261341122334";
    reset(swanComponentMock);
    when(swanComponentMock.getSwanUserIdByToken(any())).thenReturn("jane_doe_user_id");
    when(swanComponentMock.getSwanUserByToken(any())).thenReturn(janeDoe());
    HUser beforeUpdate = userJpaRepository.getByPhoneNumber(phoneNumber);
    ApiClient joeDoeClient = anApiClient(JANE_DOE_TOKEN);
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actual = api.getUserById(JANE_DOE_ID);

    assertEquals(HUser.builder()
        .id("jane_doe_id")
        .swanUserId(janeDoe().getId())
        .status(beforeUpdate.getStatus())
        .phoneNumber(beforeUpdate.getPhoneNumber())
        .monthlySubscription(beforeUpdate.getMonthlySubscription())
        .logoFileId(beforeUpdate.getLogoFileId())
        .firstName(null)
        .lastName(null)
        .idVerified(null)
        .identificationStatus(null)
        .accounts(beforeUpdate.getAccounts())
        .build(), beforeUpdate);
    assertEquals(restJaneDoeUser(), actual);
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

  @Test
  void user_has_invalid_identity_identificaiton_status() throws ApiException {
    setUpInvalidSwanUserMock(INVALID_IDENTITY_STATUS);
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeInvalidIdentity(INVALID_IDENTITY), actualUser);
  }

  @Test
  void user_has_processing_identification_status() throws ApiException {
    setUpInvalidSwanUserMock(PROCESSING_STATUS);
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeInvalidIdentity(PROCESSING), actualUser);
  }

  @Test
  void user_has_uninitiated_identification_status() throws ApiException {
    setUpInvalidSwanUserMock(UNINITIATED_STATUS);
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeInvalidIdentity(UNINITIATED), actualUser);
  }


  @Test
  void user_has_insufficient_document_quality_identification_status() throws ApiException {
    setUpInvalidSwanUserMock(INSUFFICIENT_DOCUMENT_QUALITY_STATUS);
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    User actualUser = api.getUserById(JOE_DOE_ID);

    assertEquals(restJoeDoeInvalidIdentity(INSUFFICIENT_DOCUMENT_QUALITY), actualUser);
  }

  @Test
  void user_has_unknown_identification_status() {
    setUpInvalidSwanUserMock(UNKNOWN_IDENTIFICATION_STATUS);
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"500 INTERNAL_SERVER_ERROR\",\"message\":\""
            + "Unknown identification status : Unknown\"}",
        () -> api.getUserById(JOE_DOE_ID));
  }

  private static User restJoeDoeInvalidIdentity(IdentificationStatus identificationStatus) {
    return restJoeDoeUser()
        .idVerified(false)
        .identificationStatus(identificationStatus);
  }

  private void setUpInvalidSwanUserMock(String swanIdentificationStatus) {
    reset(swanComponentMock);
    reset(userSwanRepositoryMock);
    when(userSwanRepositoryMock.whoami()).thenReturn(
        joeDoeInvalidIdentity(swanIdentificationStatus));
    when(userSwanRepositoryMock.getByToken(JOE_DOE_TOKEN)).thenReturn(
        joeDoeInvalidIdentity(swanIdentificationStatus));
    when(swanComponentMock.getSwanUserIdByToken(JOE_DOE_TOKEN)).thenReturn(
        joeDoeInvalidIdentity(swanIdentificationStatus).getId());
    try {
      when(swanComponentMock.getSwanUserByToken(JOE_DOE_TOKEN)).thenReturn(
          joeDoeInvalidIdentity(swanIdentificationStatus));
    } catch (URISyntaxException | IOException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  private static SwanUser joeDoeInvalidIdentity(String swanIdentificationStatus) {
    return joeDoe()
        .idVerified(false)
        .identificationStatus(swanIdentificationStatus);
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
