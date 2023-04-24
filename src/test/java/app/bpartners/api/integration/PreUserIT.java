package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueApi;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.PaymentScheduleService;
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

import static app.bpartners.api.integration.conf.TestUtils.VALID_EMAIL;
import static app.bpartners.api.integration.conf.TestUtils.setUpSendiblueApi;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PreUserIT.ContextInitializer.class)
@AutoConfigureMockMvc
class PreUserIT {
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private UserSwanRepository swanRepositoryMock;
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
  @MockBean
  private SendinblueApi sendinblueApi;

  @BeforeEach
  public void setUp() {
    setUpSwanComponent(swanComponentMock);
    setUpUserSwanRepository(swanRepositoryMock);
    setUpSendiblueApi(sendinblueApi);
  }

  CreatePreUser validPreUser() {
    CreatePreUser createPreUser = new CreatePreUser();
    createPreUser.setEmail(VALID_EMAIL);
    createPreUser.setFirstName("john");
    createPreUser.setLastName("doe");
    createPreUser.setSociety("johnSociety");
    createPreUser.setPhone("+33611223344");
    return createPreUser;
  }

  CreatePreUser preUserWithInvalidPhoneNumber() {
    CreatePreUser createPreUser = new CreatePreUser();
    createPreUser.setEmail(VALID_EMAIL);
    createPreUser.setFirstName("john");
    createPreUser.setLastName("doe");
    createPreUser.setSociety("johnSociety");
    createPreUser.setPhone("0");
    return createPreUser;
  }

  CreatePreUser preUserWithEmailOnly() {
    return new CreatePreUser().email(VALID_EMAIL);
  }

  CreatePreUser invalidPreUser() {
    return new CreatePreUser();
  }

  @Test
  void unauthenticated_create_pre_users_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + PreUserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> responseWithAttributes = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/preUsers"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(validPreUser()))))
            .build(),
        HttpResponse.BodyHandlers.ofString());
    HttpResponse<String> responseWithEmailOnly = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/preUsers"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(preUserWithEmailOnly()))))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.OK.value(), responseWithAttributes.statusCode());
    assertEquals(HttpStatus.OK.value(), responseWithEmailOnly.statusCode());
  }

  @Test
  void unauthenticated_create_pre_users_ko() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + PreUserIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/preUsers"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(invalidPreUser()))))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    HttpResponse<String> responseWithInvalidPhoneNumber = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/preUsers"))
            .header("Access-Control-Request-Method", "POST")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                new ObjectMapper().writeValueAsString(List.of(preUserWithInvalidPhoneNumber()))))
            .build(),
        HttpResponse.BodyHandlers.ofString());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    assertTrue(response.body().contains("Email is mandatory"));
    assertEquals(HttpStatus.BAD_REQUEST.value(), responseWithInvalidPhoneNumber.statusCode());
    assertTrue(responseWithInvalidPhoneNumber.body().contains("Invalid phone number"));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
