package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.PreRegistrationApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.PRE_REGISTRATION1_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PreRegistrationIT.ContextInitializer.class)
@AutoConfigureMockMvc
public class PreRegistrationIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SwanComponent swanComponentMock;

  @BeforeEach
  public void setUp() {
    TestUtils.setUpSwan(swanComponentMock);
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  CreatePreRegistration validPreRegistration() {
    CreatePreRegistration createPreRegistration = new CreatePreRegistration();
    createPreRegistration.setEmail(TestUtils.VALID_EMAIL);
    createPreRegistration.setFirstName("john");
    createPreRegistration.setLastName("doe");
    createPreRegistration.setSociety("johnSociety");
    createPreRegistration.setPhoneNumber("+33 54 234 234");
    return createPreRegistration;
  }

  CreatePreRegistration badPreRegistration() {
    CreatePreRegistration createPreRegistration = new CreatePreRegistration();
    createPreRegistration.setEmail(TestUtils.INVALID_EMAIL);
    createPreRegistration.setFirstName("math");
    createPreRegistration.setLastName("doe");
    createPreRegistration.setSociety("johnSociety");
    createPreRegistration.setPhoneNumber("+33 54 234 234");
    return createPreRegistration;
  }

  PreRegistration preRegistration1() {
    PreRegistration preRegistration = new PreRegistration();
    preRegistration.setId(PRE_REGISTRATION1_ID);
    preRegistration.setEmail("mathieu@email.com");
    preRegistration.setFirstName("Mathieu");
    preRegistration.setLastName("Dupont");
    preRegistration.setSociety("Dupont SARL");
    preRegistration.setPhoneNumber("+33123456789");
    preRegistration.setEntranceDatetime(Instant.parse("2022-01-01T01:00:00.00Z"));
    return preRegistration;
  }

  @Test
  void unauthenticated_create_pre_registration_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + PreRegistrationIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/pre-registration"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(validPreRegistration())))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
  }

  @Test
  void unauthenticated_read_registrations_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + PreRegistrationIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/pre-registration"))
            .header("Access-Control-Request-Method", "GET")
            .header("Content-Type", "application/json")
            .header("Origin", "http://localhost:3000")
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.FORBIDDEN.value(), response.statusCode());
  }

  @Test
  void authenticated_read_registrations_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TestUtils.USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(apiClient);

    List<PreRegistration> actual = api.getPreRegistrations(1, 10);

    assertTrue(actual.contains(preRegistration1()));
  }

  @Test
  void create_pre_registration_ok() throws ApiException {
    ApiClient apiClient = anApiClient(TestUtils.USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(apiClient);

    PreRegistration actual = api.createPreRegistration(validPreRegistration());

    List<PreRegistration> actualList = api.getPreRegistrations(1, 10);
    assertTrue(actualList.contains(actual));
  }

  @Test
  void create_pre_registration_ko() {
    ApiClient apiClient = anApiClient(TestUtils.USER1_TOKEN);
    PreRegistrationApi api = new PreRegistrationApi(apiClient);

    TestUtils.assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Invalid email\"}",
        () ->
            api.createPreRegistration(badPreRegistration()));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
