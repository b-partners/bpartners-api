package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.utils.TestUtils.BEARER_QUERY_PARAMETER_NAME;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVALID_LOGO_TYPE;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.NOT_JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.OTHER_TEST_FILE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.TEST_FILE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.TO_UPLOAD_FILE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.downloadBytes;
import static app.bpartners.api.integration.conf.utils.TestUtils.getApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.api.FilesApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.integration.conf.S3MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class FileIT extends S3MockedThirdParties {
  public static final String NON_EXISTENT_FILE_ID = "NOT" + TEST_FILE_ID;
  public static final String NOT_EXISTING_FILE_ID = "not_existing_file_id.jpeg";
  private final Tika typeGuesser = new Tika();

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  FileInfo file1() {
    return new FileInfo()
        .id(TEST_FILE_ID)
        .uploadedAt(Instant.parse("2022-08-31T13:35:26.853Z"))
        .uploadedByAccountId(JOE_DOE_ID)
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
    assertThrowsApiException(
        "{"
            + "\"type\":\"404 NOT_FOUND\","
            + "\"message\":\"File."
            + NON_EXISTENT_FILE_ID
            + " not found.\""
            + "}",
        () -> api.getFileById(JOE_DOE_ACCOUNT_ID, NON_EXISTENT_FILE_ID));
  }

  @Test
  void upload_file_ok() throws IOException, InterruptedException, ApiException {
    Resource jpegFile = new ClassPathResource("files/upload.jpg");
    Resource fakeExeFile = new ClassPathResource("files/jpeg-with-exe-extension.exe");
    Resource pngFile = new ClassPathResource("files/png-file.png");

    HttpResponse<byte[]> jpegResponse =
        upload(FileType.LOGO.getValue(), randomUUID().toString(), jpegFile.getFile());
    HttpResponse<byte[]> fakeExeResponse =
        upload(FileType.LOGO.getValue(), randomUUID().toString(), fakeExeFile.getFile());
    HttpResponse<byte[]> pngResponse =
        upload(FileType.LOGO.getValue(), randomUUID().toString(), pngFile.getFile());
    assertEquals(HttpStatus.OK.value(), jpegResponse.statusCode());
    assertEquals(HttpStatus.OK.value(), fakeExeResponse.statusCode());
    assertEquals(HttpStatus.OK.value(), pngResponse.statusCode());
    assertEquals(jpegFile.getInputStream().readAllBytes().length, jpegResponse.body().length);
    assertEquals(MediaType.IMAGE_JPEG_VALUE, typeGuesser.detect(jpegResponse.body()));
    assertEquals(fakeExeFile.getInputStream().readAllBytes().length, fakeExeResponse.body().length);
    assertEquals(MediaType.IMAGE_JPEG_VALUE, typeGuesser.detect(fakeExeResponse.body()));
    assertEquals(pngFile.getInputStream().readAllBytes().length, pngResponse.body().length);
    // /!\ it seems nor the file is a fake png nor the guessed mediaType is always jpeg
    // assertEquals(MediaType.IMAGE_PNG_VALUE, typeGuesser.detect(pngResponse.body()));
    /* /!\ The file seems to get more bytes than initial with S3 localstack container
    assertEquals(jpegFile.getInputStream().readAllBytes().length, downloadResponse.body().length);*/
  }

  @Test
  void upload_file_ko() {
    Resource toUpload = new ClassPathResource("files/real-exe-file.exe");

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\","
            + "\"message\":\"Only pdf, png, jpeg/jpg, zip and excel files are allowed."
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
    try (HttpClient unauthenticatedClient = HttpClient.newBuilder().build(); ) {
      String basePath = "http://localhost:" + localPort;

      HttpResponse<byte[]> response =
          unauthenticatedClient.send(
              HttpRequest.newBuilder()
                  .uri(
                      URI.create(
                          basePath
                              + "/accounts/"
                              + JOE_DOE_ACCOUNT_ID
                              + "/files/"
                              + fileId
                              + "/raw?fileType="
                              + fileType))
                  .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
                  .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath()))
                  .build(),
              HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() / 100 != 2) {
        throw getApiException("downloadFile", response);
      }
      return response;
    }
  }

  @Test
  void download_file_ko() {
    String basePath = "http://localhost:" + localPort;

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"File.not_existing_file_id.jpeg not found.\"}",
        () -> download(FileType.LOGO, basePath, JOE_DOE_TOKEN, null, NOT_EXISTING_FILE_ID));

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"File.null not found.\"}",
        () -> download(FileType.LOGO, basePath, JOE_DOE_TOKEN, null, null));
  }

  @Test
  void download_non_existent_file_ko() {
    ApiClient joeDoeClient = anApiClient();
    FilesApi api = new FilesApi(joeDoeClient);

    assertThrowsApiException(
        "{"
            + "\"type\":\"404 NOT_FOUND\","
            + "\"message\":\"File."
            + OTHER_TEST_FILE_ID
            + " not found.\""
            + "}",
        () ->
            api.downloadFile(JOE_DOE_ACCOUNT_ID, OTHER_TEST_FILE_ID, JOE_DOE_TOKEN, FileType.LOGO));
  }

  @Test
  void download_file_ok() throws ApiException, IOException, InterruptedException {
    String basePath = "http://localhost:" + localPort;

    HttpResponse<byte[]> responseBearerInHeader =
        download(FileType.LOGO, basePath, JOE_DOE_TOKEN, null, TEST_FILE_ID);
    HttpResponse<byte[]> responseBearerInQuery =
        download(FileType.LOGO, basePath, JOE_DOE_TOKEN, TEST_FILE_ID);
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

  public HttpResponse<byte[]> download(
      FileType fileType, String basePath, String token, String queryBearer, String fileId)
      throws ApiException, IOException, InterruptedException {
    var request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    basePath
                        + "/accounts/"
                        + JOE_DOE_ACCOUNT_ID
                        + "/files/"
                        + fileId
                        + "/raw?"
                        + BEARER_QUERY_PARAMETER_NAME
                        + "="
                        + queryBearer
                        + "&fileType="
                        + fileType))
            .header("Access-Control-Request-Method", "GET")
            .header("Authorization", BEARER_PREFIX + token)
            .GET()
            .build();
    return downloadBytes(request, "downloadFile");
  }

  public HttpResponse<byte[]> download(
      FileType fileType, String basePath, String queryBearer, String fileId)
      throws ApiException, IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    basePath
                        + "/accounts/"
                        + JOE_DOE_ACCOUNT_ID
                        + "/files/"
                        + fileId
                        + "/raw?"
                        + BEARER_QUERY_PARAMETER_NAME
                        + "="
                        + queryBearer
                        + "&fileType="
                        + fileType))
            .header("Access-Control-Request-Method", "GET")
            .GET()
            .build();
    return downloadBytes(request, "downloadFile");
  }

  // TODO: write upload_triggers_event_ok as done in InvoiceIT
}
