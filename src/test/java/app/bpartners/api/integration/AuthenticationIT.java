package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.conf.FacadeIT;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoConf;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.connectors.account.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.BAD_CODE;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.REDIRECT_FAILURE_URL;
import static app.bpartners.api.integration.conf.utils.TestUtils.REDIRECT_SUCCESS_URL;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.domainLegalFile;
import static app.bpartners.api.integration.conf.utils.TestUtils.findAvailableTcpPort;
import static app.bpartners.api.integration.conf.utils.TestUtils.location;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@AutoConfigureMockMvc
class AuthenticationIT extends MockedThirdParties {
  public static final String DEFAULT_STATE = "12341234";
  private static final String PHONE_NUMBER = "+261340465338";
  @MockBean
  private CognitoConf cognitoConf;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
  }

  CreateToken invalidCreateToken() {
    return new CreateToken()
        .code(BAD_CODE)
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl(REDIRECT_SUCCESS_URL)
                .failureUrl(REDIRECT_FAILURE_URL)
        );
  }

  AuthInitiation validAuthInitiation() {
    return new AuthInitiation()
        .phone(PHONE_NUMBER)
        .state(DEFAULT_STATE)
        .redirectionStatusUrls(new RedirectionStatusUrls()
            .successUrl(REDIRECT_SUCCESS_URL)
            .failureUrl(REDIRECT_FAILURE_URL));
  }

  @Test
  void unauthenticated_get_auth_init_not_implemented_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;
    String data = new ObjectMapper().writeValueAsString(validAuthInitiation());
    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/authInitiation"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), response.statusCode());
  }

  //TODO: add cognito get token ko and ok
  @Test
  void unauthenticated_get_token_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;
    String data = new ObjectMapper().writeValueAsString(invalidCreateToken());

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/token"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString(data))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }

  @Test
  void user_has_not_approved_legal_file_forbidden() {
    reset(legalFileRepositoryMock);
    when(legalFileRepositoryMock.findAllToBeApprovedLegalFilesByUserId(JOE_DOE_ID))
        .thenReturn(List.of(domainLegalFile()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\""
            + "User.joe_doe_id has not approved the legal file cgu_20-11-23.pdf\"}",
        () -> api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID));
  }
}
