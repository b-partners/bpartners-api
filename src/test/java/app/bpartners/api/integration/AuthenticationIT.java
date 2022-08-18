package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.model.AuthParams;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.TokenParams;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AuthenticationIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AuthenticationIT {
  @MockBean
  private SentryConf sentryConf;

  @Autowired
  private SwanComponent swanComponent;


  private static final String PHONE_NUMBER = "+261340465338";
  @Value("${test.swan.user.code}")
  private String userCode;

  TokenParams validCode() {
    return new TokenParams().code(userCode);
  }

  TokenParams badCode() {
    return new TokenParams().code("bad_code");
  }

  @Test
  void unauthenticated_get_auth_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + AuthenticationIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/auth"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString("{\n"
                + "  \"phoneNumber\": \"" + PHONE_NUMBER + "\",\n"
                + "  \"successUrl\": \"" + REDIRECT_URL + "\",\n"
                + "  \"failureUrl\": \"failureUrl\"\n"
                + "}"))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }

  @Test
  void valid_code_provide_token_ok() {
    Token validToken = swanComponent.getTokenByCode(userCode, REDIRECT_URL);
    assertNotNull(validToken);
  }

  @Test
  void bad_code_provide_token_ko() {
    assertNull(swanComponent.getTokenByCode(badCode().getCode(), REDIRECT_URL));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
