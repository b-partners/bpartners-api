package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.LegalFile;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class LegalFileIT extends MockedThirdParties {
  public static final String NOT_EXISTING_LEGAL_FILE = "not_existing_legal_file";

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
  }

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @Test
  void read_legal_files_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getLegalFiles("NOT" + JOE_DOE_ID));
  }

  @Test
  void read_legal_files_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<LegalFile> actual = api.getLegalFiles(JOE_DOE_ID);

    assertEquals(5, actual.size());
    assertTrue(actual.contains(legalFile1().toBeConfirmed(true)));
  }

  @Test
  void approve_legal_file_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    LegalFile actual = api.approveLegalFile(JOE_DOE_ID, defaultLegalFile().getId());

    assertNotNull(actual.getApprovalDatetime());
  }

  @Test
  void approve_legal_file_ko() {
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.approveLegalFile("NOT" + JOE_DOE_ID, legalFile1().getId()));
    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":"
            + "\"LegalFile.legal_file1_id was already approved on 2022-01-01T00:00:00Z\"}",
        () -> api.approveLegalFile(JOE_DOE_ID, legalFile1().getId()));
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "LegalFile.not_existing_legal_file is not found\"}",
        () -> api.approveLegalFile(JOE_DOE_ID, NOT_EXISTING_LEGAL_FILE));
  }
}
