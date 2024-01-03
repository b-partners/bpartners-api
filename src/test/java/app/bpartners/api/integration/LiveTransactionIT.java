package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.FileInfo;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.TRANSACTION1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.TRANSACTION3_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.restTransaction1;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = LiveTransactionIT.ContextInitializer.class)
class LiveTransactionIT extends MockedThirdParties {

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        DbEnvContextInitializer.getHttpServerPort());
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    when(bridgeApi.findTransactionsUpdatedByToken(any()))
        .thenReturn(List.of());
  }

  @Test
  void read_transactions_supporting_documents_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<FileInfo> actual1 =
        api.getTransactionSupportingDocuments(JOE_DOE_ACCOUNT_ID, TRANSACTION1_ID);
    List<FileInfo> actual2 =
        api.getTransactionSupportingDocuments(JOE_DOE_ACCOUNT_ID, TRANSACTION1_ID);

    assertEquals(1, actual1.size());
    assertTrue(actual2.isEmpty());

    assertTrue(
        actual1.stream()
            .anyMatch(fileInfo ->
                fileInfo.getId() != null
                    && fileInfo.getId().equals("transaction_supporting_doc1_id"))
    );
  }

  @Test
  void read_transactions_supporting_documents_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsForbiddenException(
        () -> api.getTransactionSupportingDocuments(JANE_ACCOUNT_ID, TRANSACTION1_ID));
  }

  //TODO: add KO test
  @Test
  void add_then_delete_transactions_supporting_documents_ok() throws ApiException, IOException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);
    Resource jpegFile = new ClassPathResource("files/upload.jpg");
    List<FileInfo> initial =
        api.getTransactionSupportingDocuments(JOE_DOE_ACCOUNT_ID, TRANSACTION3_ID);

    List<FileInfo> actual =
        api.addTransactionSupportingDocuments(JOE_DOE_ACCOUNT_ID, TRANSACTION3_ID,
            jpegFile.getFile());
    List<FileInfo> dropped =
        api.deleteTransactionSupportingDocuments(JOE_DOE_ACCOUNT_ID, TRANSACTION3_ID,
            actual.stream()
                .map(FileInfo::getId)
                .toList());

    List<FileInfo> lastState =
        api.getTransactionSupportingDocuments(JOE_DOE_ACCOUNT_ID, TRANSACTION3_ID);
    assertTrue(initial.isEmpty());
    assertEquals(1, actual.size());
    assertEquals(actual, dropped);
    assertTrue(lastState.isEmpty());
  }

  @Test
  void read_transactions_by_label() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual = api.getTransactions(JOE_DOE_ACCOUNT_ID, "Création", null, null,
        null, null);

    assertEquals(1, actual.size());
    assertEquals(restTransaction1(), actual.get(0));
  }

  static class ContextInitializer extends DbEnvContextInitializer {
  }

  @Test
  void read_filtered_transaction_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actualFilteredByStatus =
        api.getTransactions(JOE_DOE_ACCOUNT_ID, null, TransactionStatus.PENDING,
            null, null, null);
    List<Transaction> actualFilteredByCategory = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null,
        "Sponsoring", null, null);

    assertEquals(1, actualFilteredByStatus.size());
    assertEquals(1, actualFilteredByCategory.size());
    assertTrue(actualFilteredByStatus.contains(restTransaction1()));
  }
}
