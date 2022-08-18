package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.model.RedirectionParams;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.TokenParams;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
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


  private static final String PHONE_NUMBER = "+261343919883";
  @Value("${test.swan.user.code}")
  private String userCode;

  RedirectionParams phoneNumberRedirection() {
    return new RedirectionParams().phoneNumber(PHONE_NUMBER);
  }

  TokenParams validCode() {
    return new TokenParams().code(userCode);
  }

  TokenParams badCode() {
    return new TokenParams().code("bad_code");
  }

  @Test
  void valid_code_provide_token_ok() {
    Token validToken = swanComponent.getTokenByCode(userCode);
    assertNotEquals(null, validToken);
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
