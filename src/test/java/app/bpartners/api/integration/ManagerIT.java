package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import app.bpartners.api.endpoint.rest.api.UsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.Manager;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ManagerIT.ContextInitializer.class)
@AutoConfigureMockMvc
class ManagerIT {

  @MockBean
  private SentryConf sentryConf;

  @MockBean
  private CognitoComponent cognitoComponentMock;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  public static Manager manager1() {
    Manager manager = new Manager();
    manager.setId("manager1_id");
    manager.setFirstName("One");
    manager.setLastName("Manager");
    manager.setEmail("test+manager1@hei.school");
    manager.setRef("MGR21001");
    manager.setPhone("0322411127");
    manager.setStatus(EnableStatus.ENABLED);
    manager.setSex(Manager.SexEnum.M);
    manager.setBirthDate(LocalDate.parse("1890-01-01"));
    manager.setEntranceDatetime(Instant.parse("2021-09-08T08:25:29Z"));
    manager.setAddress("Adr 5");
    return manager;
  }

  @BeforeEach
  public void setUp() {
    TestUtils.setUpCognito(cognitoComponentMock);
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(TestUtils.STUDENT1_TOKEN);

    UsersApi api = new UsersApi(student1Client);
    TestUtils.assertThrowsForbiddenException(() -> api.getManagerById(TestUtils.MANAGER_ID));
    TestUtils.assertThrowsForbiddenException(() -> api.getManagers(1, 20));
  }

  @Test
  void teacher_read_ko() {
    ApiClient teacher1Client = anApiClient(TestUtils.TEACHER1_TOKEN);

    UsersApi api = new UsersApi(teacher1Client);
    TestUtils.assertThrowsForbiddenException(() -> api.getManagerById(TestUtils.MANAGER_ID));
    TestUtils.assertThrowsForbiddenException(() -> api.getManagers(1, 20));
  }

  @Test
  void manager_read_own_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);

    UsersApi api = new UsersApi(manager1Client);
    Manager actual = api.getManagerById(TestUtils.MANAGER_ID);

    assertEquals(manager1(), actual);
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);

    UsersApi api = new UsersApi(manager1Client);
    List<Manager> managers = api.getManagers(1, 20);

    assertEquals(1, managers.size());
    assertEquals(manager1(), managers.get(0));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
