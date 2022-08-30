package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.repository.swan.UserSwanRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PreUserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class PreUserIT {
  @MockBean
  UserSwanRepository swanRepositoryMock;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SwanComponent swanComponentMock;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpSwanRepository(swanRepositoryMock);
  }

  CreatePreUser validPreUser() {
    CreatePreUser createPreUser = new CreatePreUser();
    createPreUser.setEmail(TestUtils.VALID_EMAIL);
    createPreUser.setFirstName("john");
    createPreUser.setLastName("doe");
    createPreUser.setSociety("johnSociety");
    createPreUser.setPhone("+33 54 234 234");
    return createPreUser;
  }

  @Test
  void unauthenticated_create_pre_users_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + PreUserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/preUsers"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(validPreUser()))))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
