package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.model.RedirectionParams;
import app.bpartners.api.endpoint.rest.model.TokenParams;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AuthenticationIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AuthenticationIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SwanComponent swanComponentMock;

  @MockBean
  private SwanConf swanConf;

  private static final String phoneNumber = "+261343919883";
  @Value("${test.swan.user.code}")
  private static String userCode;

  @BeforeEach
  public void setUp() {
    TestUtils.setUpSwan(swanComponentMock);
  }

  static RedirectionParams phoneNumber() {
    return new RedirectionParams().phoneNumber(phoneNumber);
  }

  static TokenParams validCode() {
    return new TokenParams().code(userCode);
  }

  static TokenParams badCode() {
    return new TokenParams().code("bad_code");
  }




  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
