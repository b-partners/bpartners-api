package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.FilesApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
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
import static app.bpartners.api.integration.conf.TestUtils.USER1_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = FileIT.ContextInitializer.class)
@AutoConfigureMockMvc
class FileIT {
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
    setUpSwanRepository(swanRepositoryMock);
  }

  FileInfo file1() {
    return new FileInfo()
        .id(FILE_ID)
        .uploadedAt(Instant.parse("2022-08-31T13:35:26.853Z"))
        .uploadedByUserId(USER1_ID)
        .sizeInKB(76)
        .sha256("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
  }

  @Test
  void read_file_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    FilesApi api = new FilesApi(joeDoeClient);

    FileInfo actual = api.getFileById(FILE_ID);

    assertEquals(file1(), actual);
  }

  /*
    TODO: replace this by setting manually the HttpClient as below
  @Test
  void upload_file_ok() throws ApiException, IOException {

    ApiClient joeDoeClient = anApiClient(bearerToken);
    FilesApi api = new FilesApi(joeDoeClient);
    Resource resource = new ClassPathResource(
        "files/301327722_738668533870624_6151351867004964160_n.jpeg");

    File actual = api.uploadFile(CREATE_FILE_ID, resource.getFile());

    assertEquals(resource.getInputStream().readAllBytes(), actual);
  }
   */

  @Test
  void download_file_ok() throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;
    Resource logoFileResource = new ClassPathResource(
        "files/301327722_738668533870624_6151351867004964160_n.jpeg");

    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/files/" + FILE_ID + "/raw"))
            .header("Access-Control-Request-Method", "GET")
            .header("Authorization", "Bearer " + bearerToken)
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofByteArray());

    assertEquals(HttpStatus.OK.value(), response.statusCode());
    assertEquals(logoFileResource.getInputStream().readAllBytes().length, response.body().length);
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
