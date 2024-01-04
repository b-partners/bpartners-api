package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.TEST_FILE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.integration.conf.MockedThirdParties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class ExceptionHandlerIT extends MockedThirdParties {

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  // TODO: should throw a bad request exception instead of a forbidden
  /*@Test
  void unsupported_method_to_bad_request() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + DbEnvContextInitializer.getHttpServerPort();
    Resource toUpload = new ClassPathResource("files/upload.jpg");

    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + TEST_FILE_ID
                    + "/raw"))
            .PUT(HttpRequest.BodyPublishers.ofFile(toUpload.getFile().toPath()))
            .header("Authorization", "Bearer " + bearerToken).build(),
        HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }*/

  @Test
  void message_not_readable_to_bad_request() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;
    Resource toUpload = new ClassPathResource("files/upload.jpg");

    HttpResponse<byte[]> response =
        unauthenticatedClient.send(
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        basePath
                            + "/accounts/"
                            + JOE_DOE_ACCOUNT_ID
                            + "/files/"
                            + TEST_FILE_ID
                            + "/raw"))
                .header("Authorization", "Bearer " + JOE_DOE_TOKEN)
                .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.getFile().toPath()))
                .build(),
            HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
  }
}
