package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.repository.swan.UserSwanRepository;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ExceptionHandlerIT.ContextInitializer.class)
@AutoConfigureMockMvc
class ExceptionHandlerIT {
  @MockBean
  UserSwanRepository swanRepositoryMock;
  @Value("${test.user.access.token}")
  private String bearerToken;
  @MockBean
  private SentryConf sentryConf;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpUserSwanRepository(swanRepositoryMock);
  }

  @Test
  void unsupported_method_to_bad_request() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;
    Resource toUpload = new ClassPathResource("files/upload.jpg");

    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + FILE_ID
                    + "/raw"))
            .header("Authorization", "Bearer " + bearerToken)
            .method("PUT", HttpRequest.BodyPublishers.ofFile(toUpload.getFile().toPath())).build(),
        HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }

  @Test
  void message_not_readable_to_bad_request() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;
    Resource toUpload = new ClassPathResource("files/upload.jpg");

    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + FILE_ID
                    + "/raw"))
            .header("Authorization", "Bearer " + bearerToken)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.getFile().toPath())).build(),
        HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
