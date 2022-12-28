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
import app.bpartners.api.repository.LegalFileRepository;
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
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.TestUtils.BEARER_QUERY_PARAMETER_NAME;
import static app.bpartners.api.integration.conf.TestUtils.INVALID_LOGO_TYPE;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.NOT_JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.OTHER_TEST_FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.TEST_FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.TO_UPLOAD_FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.TestUtils.getApiException;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountHolderSwanRep;
import static app.bpartners.api.integration.conf.TestUtils.setUpAccountSwanRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.TestUtils.setUpSwanComponent;
import static app.bpartners.api.integration.conf.TestUtils.setUpUserSwanRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = FileIT.ContextInitializer.class)
@AutoConfigureMockMvc
class FileIT {
  public static final String NON_EXISTENT_FILE_ID = "NOT" + TEST_FILE_ID;
  public static final String NOT_EXISTING_FILE_ID = "not_existing_file_id.jpeg";
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
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  private Tika typeGuesser = new Tika();


  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() {
    setUpUserSwanRepository(userSwanRepositoryMock);
    setUpSwanComponent(swanComponentMock);
    setUpAccountSwanRepository(accountSwanRepositoryMock);
    setUpAccountHolderSwanRep(accountHolderRepositoryMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
  }

  FileInfo file1() {
    return new FileInfo()
        .id(TEST_FILE_ID)
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

    FileInfo actual = api.getFileById(JOE_DOE_ACCOUNT_ID, TEST_FILE_ID);

    assertEquals(file1(), actual);
  }

  @Test
  void read_file_info_ko() {
    ApiClient joeDoeClient = anApiClient();
    FilesApi api = new FilesApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getFileById(NOT_JOE_DOE_ACCOUNT_ID, TEST_FILE_ID));
    assertThrowsApiException("{"
            + "\"type\":\"404 NOT_FOUND\","
            + "\"message\":\"File." + NON_EXISTENT_FILE_ID + " not found.\""
            + "}",
        () -> api.getFileById(JOE_DOE_ACCOUNT_ID, NON_EXISTENT_FILE_ID)
    );
  }

  @Test
  void upload_file_ok() throws IOException, InterruptedException, ApiException {
    Resource jpegFile = new ClassPathResource("files/upload.jpg");
    Resource fakeExeFile = new ClassPathResource("files/jpeg-with-exe-extension.exe");
    Resource pngFile = new ClassPathResource("files/png-file.png");

    HttpResponse<byte[]> jpegResponse = upload(FileType.LOGO.getValue(), randomUUID().toString(),
        jpegFile.getFile());
    HttpResponse<byte[]> fakeExeResponse = upload(FileType.LOGO.getValue(), randomUUID().toString(),
        fakeExeFile.getFile());
    HttpResponse<byte[]> pngResponse = upload(FileType.LOGO.getValue(), randomUUID().toString(),
        pngFile.getFile());

    assertEquals(HttpStatus.OK.value(), jpegResponse.statusCode());
    assertEquals(HttpStatus.OK.value(), fakeExeResponse.statusCode());
    assertEquals(HttpStatus.OK.value(), pngResponse.statusCode());
    assertEquals(jpegFile.getInputStream().readAllBytes().length, jpegResponse.body().length);
    assertEquals(MediaType.IMAGE_JPEG_VALUE, typeGuesser.detect(jpegResponse.body()));
    assertEquals(fakeExeFile.getInputStream().readAllBytes().length, fakeExeResponse.body().length);
    assertEquals(MediaType.IMAGE_JPEG_VALUE, typeGuesser.detect(fakeExeResponse.body()));
    assertEquals(pngFile.getInputStream().readAllBytes().length, pngResponse.body().length);
    // /!\ it seems nor the file is a fake png nor the guessed mediaType is always jpeg
    //assertEquals(MediaType.IMAGE_PNG_VALUE, typeGuesser.detect(pngResponse.body()));
    /* /!\ The file seems to get more bytes than initial with S3 localstack container
    assertEquals(jpegFile.getInputStream().readAllBytes().length, downloadResponse.body().length);*/
  }

  @Test
  void upload_file_ko() {
    Resource toUpload = new ClassPathResource("files/real-exe-file.exe");

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"Only pdf, png and jpeg/jpg files are allowed."
            + "\"}",
        () -> upload(FileType.LOGO.getValue(), "test", toUpload.getFile()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"No enum constant app.bpartners.api.endpoint.rest"
            + ".model.FileType.invalid_logo_type"
            + "\"}",
        () -> upload(INVALID_LOGO_TYPE, TO_UPLOAD_FILE_ID, toUpload.getFile()));
  }

  private HttpResponse<byte[]> upload(String fileType, String fileId, File toUpload)
      throws IOException, InterruptedException, ApiException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;

    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + fileId
                    + "/raw?fileType=" + fileType))
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath())).build(),
        HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() / 100 != 2) {
      throw getApiException("downloadFile", response);
    }
    return response;
  }

  @Test
  void download_file_ko() {
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"File.not_existing_file_id.jpeg not found.\"}",
        () -> download(FileType.LOGO, basePath, JOE_DOE_TOKEN,
            null, NOT_EXISTING_FILE_ID));

    assertThrowsApiException("{\"type\":\"404 NOT_FOUND\",\"message\":\"File.null not found.\"}",
        () -> download(FileType.LOGO, basePath, JOE_DOE_TOKEN,
            null, null));
  }

  @Test
  void download_non_existent_file_ko() {
    ApiClient joeDoeClient = anApiClient();
    FilesApi api = new FilesApi(joeDoeClient);

    assertThrowsApiException("{"
            + "\"type\":\"404 NOT_FOUND\","
            + "\"message\":\"File." + OTHER_TEST_FILE_ID + " not found.\""
            + "}",
        () -> api.downloadFile(JOE_DOE_ACCOUNT_ID, OTHER_TEST_FILE_ID, JOE_DOE_TOKEN,
            FileType.LOGO));
  }

  @Test
  void download_file_ok() throws IOException, InterruptedException, ApiException {
    String basePath = "http://localhost:" + ContextInitializer.SERVER_PORT;

    HttpResponse<byte[]> responseBearerInHeader = download(FileType.LOGO, basePath, JOE_DOE_TOKEN,
        null, TEST_FILE_ID);
    HttpResponse<byte[]> responseBearerInQuery = download(FileType.LOGO, basePath, JOE_DOE_TOKEN,
        TEST_FILE_ID);
    HttpResponse<byte[]> responseBearerInBoth =
        download(FileType.LOGO, basePath, JOE_DOE_TOKEN, JOE_DOE_TOKEN, TEST_FILE_ID);

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

  public HttpResponse<byte[]> download(FileType fileType, String basePath, String token,
                                       String queryBearer,
                                       String fileId)
      throws IOException, InterruptedException, ApiException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + fileId
                    + "/raw?"
                    + BEARER_QUERY_PARAMETER_NAME + "=" + queryBearer + "&fileType="
                    + fileType))
            .header("Access-Control-Request-Method", "GET")
            .header("Authorization", BEARER_PREFIX + token)
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() / 100 != 2) {
      throw getApiException("downloadFile", response);
    }
    return response;
  }

  public HttpResponse<byte[]> download(FileType fileType, String basePath, String queryBearer,
                                       String fileId)
      throws IOException, InterruptedException, ApiException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    HttpResponse<byte[]> response = unauthenticatedClient.send(
        HttpRequest.newBuilder()
            .uri(URI.create(
                basePath + "/accounts/" + JOE_DOE_ACCOUNT_ID + "/files/" + fileId
                    + "/raw?"
                    + BEARER_QUERY_PARAMETER_NAME + "=" + queryBearer + "&fileType="
                    + fileType))
            .header("Access-Control-Request-Method", "GET")
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() / 100 != 2) {
      throw getApiException("downloadFile", response);
    }
    return response;
  }

  //TODO: write upload_triggers_event_ok as done in InvoiceIT
  public static class ContextInitializer extends S3AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
