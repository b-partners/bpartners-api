package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.FinctectureTokenManager;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.model.TokenResponse;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.response.ProjectTokenResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AuthenticationIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AuthenticationIT {
  private static final String PHONE_NUMBER = "+261340465338";
  @MockBean
  private SentryConf sentryConf;
  @Autowired
  private SwanComponent swanComponent;

  @Autowired
  private FinctectureTokenManager finctectureTokenManager;


  @Autowired
  private SwanApi<ProjectTokenResponse> swanApi;

  @Autowired
  private ProjectTokenManager tokenManager;

  @Value("${test.swan.user.code}")
  private String userCode;

  /*CreateToken validCode() {
    return new CreateToken().code(userCode);
  }*/

  CreateToken badCode() {
    return new CreateToken()
        .code("bad_code")
        .redirectionStatusUrls(
            new RedirectionStatusUrls()
                .successUrl(REDIRECT_URL)
                .failureUrl("FailureUrl")
        );
  }

  @Test
  void unauthenticated_get_auth_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + AuthenticationIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/authInitiation"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString("{\n"
                + "  \"state\": \"12341234\",\n"
                + "  \"phone\": \"" + PHONE_NUMBER + "\",\n"
                + "\"redirectionStatusUrls\": {\n"
                + "    \"successUrl\": \"" + REDIRECT_URL + "\",\n"
                + "    \"failureUrl\": \"" + REDIRECT_URL + "/error\"\n"
                + "  }"
                + "}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertTrue(response.body().contains("phoneNumber=%2B"));
  }

  @Test
  void unauthenticated_get_token_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + AuthenticationIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/token"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString("{\n"
                + "  \"code\": \"" + badCode().getCode() + "\",\n"
                + "\"redirectionStatusUrls\": {\n"
                + "    \"successUrl\": \"https://localhost:3000/login/success\",\n"
                + "    \"failureUrl\": \"https://localhost:3000/login/failure\"\n"
                + "  }"
                + "}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }

  @Test
  void get_fintecture_project_token_ok() {
    TokenResponse actual = finctectureTokenManager.getProjectAccessToken();

    assertNotNull(actual);
    assertNotNull(actual.getTokenType());
    assertNotNull(actual.getAccessToken());
    assertNotNull(actual.getExpiresIn());
  }

  // /!\ This test is skipped because the userCode is only available for one test
  // and errors occurs for CI and CD tests
  //@Test
  //void valid_code_provide_token_ok() {
  //  Token validToken = swanComponent.getTokenByCode(userCode, "https://localhost:3000/login/success");
  //assertNull(validToken);
  //}

  @Test
  void bad_code_provide_token_ko() {
    String badCode = badCode().getCode();
    assertThrows(ApiException.class,
        () -> swanComponent.getTokenByCode(badCode, REDIRECT_URL));
  }

  @Test
  void get_access_token_from_swan_ok() {
    ProjectTokenResponse tokenResponse = swanApi.getProjectToken();

    assertNotNull(tokenResponse);
    assertNotNull(tokenResponse.getAccessToken());
    assertTrue(
        !tokenResponse.getAccessToken().isBlank() && !tokenResponse.getAccessToken().isEmpty());
    assertNotNull(tokenResponse.getTokenType());
    assertNotNull(tokenResponse.getScope());
    assertNotNull(tokenResponse.getExpiresIn());
  }

  @Test
  void get_swan_token_from_ssm_ok() {
    assertNotNull(tokenManager.getSwanProjecToken());
  }

  @Test
  void get_fintecture_project_from_ssm_ok() {
    assertNotNull(tokenManager.getFintectureProjectToken());
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
