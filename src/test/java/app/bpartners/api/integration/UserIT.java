package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.UsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.time.LocalDate;
import java.util.List;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = UserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class UserIT {
  public User user1(){
    User user = new User();
    user.setId("user1_id");
    user.setSwanId("swan_user1_id");
    user.setBirthDate(LocalDate.of(2022, 8, 8));
    user.setIdVerified(true);
    user.setIdentificationStatus("ValidIdentity");
    user.setNationalityCCA3("FRA");
    user.setMobilePhoneNumber("+33123456789");
    user.setMonthlySubscription(5);
    user.setStatus(EnableStatus.ENABLED);
    return user;
  }

  public User user2(){
    User user = new User();
    user.setId("user2_id");
    user.setSwanId("swan_user2_id");
    user.setBirthDate(LocalDate.of(2022, 8, 8));
    user.setIdVerified(true);
    user.setIdentificationStatus("ValidIdentity");
    user.setNationalityCCA3("FRA");
    user.setMobilePhoneNumber("+33123456789");
    user.setMonthlySubscription(5);
    user.setStatus(EnableStatus.ENABLED);
    return user;
  }

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

  @Test
  void users_page_are_not_implemented() {
    UsersApi usersApi = new UsersApi(anApiClient(TestUtils.USER1_TOKEN));

    assertThrowsApiException("{"
            + "\"type\":\"501 NOT_IMPLEMENTED\",\"message\":\""
            + "/users endpoint not yet implemented\"}",
        () -> usersApi.getUsers(1, 10));
  }
  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }

  @Test
  void user_read_ok() throws ApiException {
    ApiClient user1Client = anApiClient(TestUtils.USER1_TOKEN);
    UsersApi api = new UsersApi(user1Client);
    SwanUser swanUser = SwanComponent.getUserById(TestUtils.SWAN_USER1_ID);
    User actual = api.getUserById(TestUtils.USER1_ID);
    List<User> userList = api.getUsers(1,20);

    Assertions.assertEquals(user1(), actual);
    Assertions.assertTrue(userList.contains(user1()));
    Assertions.assertTrue(userList.contains(user2()));
  }
}
