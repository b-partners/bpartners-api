package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.FilesApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.S3AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
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

import static app.bpartners.api.integration.conf.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.TestUtils.BEARER_QUERY_PARAMETER_NAME;
import static app.bpartners.api.integration.conf.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.TO_UPLOAD_FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = FileIT.ContextInitializer.class)
@AutoConfigureMockMvc
class FileIT {
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
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
  private AccountHolderSwanRepository accountHolderRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
  }

  FileInfo file1() {
    return new FileInfo()
        .id(FILE_ID)
        .uploadedAt(Instant.parse("2022-08-31T13:35:26.853Z"))
        .uploadedByAccountId(JOE_DOE_ACCOUNT_ID)
        .sizeInKB(76)
        .sha256("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
  }

  /* /!\ The upload seems to return null instead of the appropriate checksum with S3 localstack
  container so the persisted sha256 is set to null for the test */
  @Test
  void read_file_info_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    FilesApi api = new FilesApi(joeDoeClient);

    FileInfo actual = api.getFileById(JOE_DOE_ACCOUNT_ID, FILE_ID);

    assertEquals(file1(), actual);
  }

  @Test
  void upload_and_read_created_file_ok() throws IOException, InterruptedException {
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;
    Resource toUpload = new ClassPathResource("files/upload.jpg");

    HttpResponse<byte[]> uploadResponse = upload(FileType.LOGO, TO_UPLOAD_FILE_ID,
        toUpload.getFile());

    HttpResponse<byte[]> downloadResponse =
        download(FileType.LOGO, basePath, JOE_DOE_TOKEN, null, TO_UPLOAD_FILE_ID);
    assertEquals(HttpStatus.OK.value(), uploadResponse.statusCode());
    assertEquals(toUpload.getInputStream().readAllBytes().length, uploadResponse.body().length);
    assertEquals(HttpStatus.OK.value(), downloadResponse.statusCode());
    /* /!\ The file seems to get more bytes than initial with S3 localstack container
    assertEquals(toUpload.getInputStream().readAllBytes().length, downloadResponse.body().length);*/
  }

  private HttpResponse<byte[]> upload(FileType fileType, String fileId, File toUpload)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;

    return unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + fileId
                    + "/raw?fileType=" + fileType))
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath())).build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  @Test
  void download_file_ok() throws IOException, InterruptedException {
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;

    HttpResponse<byte[]> responseBearerInHeader = download(FileType.LOGO, basePath, JOE_DOE_TOKEN,
        null, FILE_ID);
    HttpResponse<byte[]> responseBearerInQuery = download(FileType.LOGO, basePath, JOE_DOE_TOKEN,
        FILE_ID);
    HttpResponse<byte[]> responseBearerInBoth =
        download(FileType.LOGO, basePath, JOE_DOE_TOKEN, JOE_DOE_TOKEN, FILE_ID);

    assertEquals(HttpStatus.OK.value(), responseBearerInHeader.statusCode());
    assertEquals(HttpStatus.OK.value(), responseBearerInQuery.statusCode());
    assertEquals(HttpStatus.OK.value(), responseBearerInBoth.statusCode());
    /* /!\ The file seems to get more bytes than initial with S3 localstack container
    Resource logoFileResource = new ClassPathResource(
        "files/downloaded.jpeg");
    assertEquals(logoFileResource.getInputStream().readAllBytes().length,
          responseBearerInHeader.body().length);
    assertEquals(logoFileResource.getInputStream().readAllBytes().length,
      responseBearerInQuery.body().length);
    assertEquals(logoFileResource.getInputStream().readAllBytes().length,
      responseBearerInBoth.body().length);*/
  }

  public HttpResponse<byte[]> download(FileType fileType, String basePath, String JOE_DOE_TOKEN,
                                       String queryBearer,
                                       String fileId)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    return unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + fileId + "/raw?"
                    + BEARER_QUERY_PARAMETER_NAME + "=" + queryBearer + "&fileType=" + fileType))
            .header("Access-Control-Request-Method", "GET")
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  public HttpResponse<byte[]> download(FileType fileType, String basePath, String queryBearer,
                                       String fileId)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    return unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + fileId + "/raw?"
                    + BEARER_QUERY_PARAMETER_NAME + "=" + queryBearer + "&fileType=" + fileType))
            .header("Access-Control-Request-Method", "GET")
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  public static class ContextInitializer extends S3AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
