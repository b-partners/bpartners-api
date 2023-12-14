package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueApi;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
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

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
@AutoConfigureMockMvc
class PreUserIT extends MockedThirdParties {
  @MockBean
  private SendinblueApi sendinblueApi;

  @BeforeEach
  public void setUp() {
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
    String basePath = "http://localhost:" + localPort;

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
    String basePath = "http://localhost:" + localPort;

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
}
