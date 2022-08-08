package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.UsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class UserIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private CognitoComponent cognitoComponentMock;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    TestUtils.setUpCognito(cognitoComponentMock);
  }
  User john() {
    User john = new User();
    john.setId("123444");
    john.setMonthlySubscription(5);
    john.setStatus(EnableStatus.ENABLED);
    john.setMobilePhoneNumber("1231231");
    return john;
  }
  @Test
  void user_read_ok() throws ApiException {
    UsersApi usersApi = new UsersApi(anApiClient(TestUtils.USER1_TOKEN));

    User actual = usersApi.getUserById("123444");

    assertEquals(john(),actual);
  }

  @Test
  void user_read_list_ok() throws ApiException{
    UsersApi usersApi = new UsersApi(anApiClient(TestUtils.USER1_TOKEN));
    List<User> expected = new ArrayList<>();
    expected.add(john());
    expected.add(john());

    List<User> actual = usersApi.getUsers(1,10);

    assertEquals(expected,actual);
  }
  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
