package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.TEST_FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ExceptionHandlerIT.ContextInitializer.class)
@AutoConfigureMockMvc
class ExceptionHandlerIT {
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
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private AccountHolderSwanRepository accountHolderMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpAccountHolderSwanRep(accountHolderMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  //TODO: should throw a bad request exception instead of a forbidden
  /*@Test
  void unsupported_method_to_bad_request() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;
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
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;
    Resource toUpload = new ClassPathResource("files/upload.jpg");

    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + TEST_FILE_ID
                    + "/raw"))
            .header("Authorization", "Bearer " + JOE_DOE_TOKEN)
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
