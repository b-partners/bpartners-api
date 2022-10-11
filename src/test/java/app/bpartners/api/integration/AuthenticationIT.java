package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.BAD_CODE;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_FAILURE_URL;
import static app.bpartners.api.integration.conf.TestUtils.REDIRECT_SUCCESS_URL;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AuthenticationIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AuthenticationIT {
  private static final String PHONE_NUMBER = "+261340465338";
  public static final String DEFAULT_STATE = "12341234";
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
  private SwanComponent swanComponentMock;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
  }

  //Uncomment to test the token provider
  //@Value("${test.swan.user.code}")
  //private String userCode;
  //CreateToken validCode() {
  //  return new CreateToken().code(userCode);
  //}

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
  void unauthenticated_get_auth_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + AuthenticationIT.ContextInitializer.SERVER_PORT;
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

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }

  @Test
  void unauthenticated_get_token_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + AuthenticationIT.ContextInitializer.SERVER_PORT;
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


  // /!\ This test is skipped because the userCode is only available for one test
  // and errors occurs for CI and CD tests
  //@Test
  //void valid_code_provide_token_ok() {
  //  Token validToken = swanComponent.getTokenByCode(userCode, "https://localhost:3000/login/success");
  //assertNull(validToken);
  //}

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
