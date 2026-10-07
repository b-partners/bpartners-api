package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.SubscriptionBillingApi;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.service.aws.S3Service;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class SubscriptionInvoiceExportIT extends MockedThirdParties {
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";
  private static final String SUBSCRIBER_ID = "subscriber_id";
  private static final String BILLED_CUSTOMER_ID = "it_export_billed_customer_id";
  private static final String OTHER_BILLED_CUSTOMER_ID = "it_export_other_billed_customer_id";
  private static final String UNBILLED_CUSTOMER_ID = "it_export_unbilled_customer_id";
  private static final String SUBSCRIPTION_INVOICE_ID = "it_export_subscription_invoice_id";
  private static final String CREDIT_INVOICE_ID = "it_export_credit_invoice_id";
  private static final String OTHER_SUBSCRIPTION_INVOICE_ID = "it_export_other_invoice_id";
  private static final String DRAFT_INVOICE_ID = "it_export_draft_invoice_id";
  private static final String CREDIT_PURCHASE_ID = "it_export_credit_purchase_id";
  private static final LocalDate EXPORTED_FROM = LocalDate.of(2031, 5, 1);
  private static final LocalDate EXPORTED_TO = LocalDate.of(2031, 5, 31);
  private static final String PRESIGNED_URL = "https://s3/factures-abonnements.zip";

  @MockBean S3Service s3ServiceMock;
  @Autowired JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
    when(userSubscriptionConf.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
    when(s3ServiceMock.downloadFile(eq(INVOICE), anyString(), eq(USER_TO_CREDIT_ID)))
        .thenAnswer(invocation -> pdfNamed(invocation.getArgument(1)));
    when(s3ServiceMock.presignURL(eq(INVOICE_ZIP), anyString(), anyString(), anyLong()))
        .thenReturn(PRESIGNED_URL);

    givenCustomer(BILLED_CUSTOMER_ID, "Toiture Export SARL");
    givenCustomer(OTHER_BILLED_CUSTOMER_ID, "Couvreur Export SAS");
    givenCustomer(UNBILLED_CUSTOMER_ID, "Sans Facture SARL");
    givenInvoice(SUBSCRIPTION_INVOICE_ID, "REF-EXPORT1", BILLED_CUSTOMER_ID, "CONFIRMED");
    givenInvoice(CREDIT_INVOICE_ID, "REF-EXPORT2", BILLED_CUSTOMER_ID, "PAID");
    givenInvoice(OTHER_SUBSCRIPTION_INVOICE_ID, "REF-EXPORT3", OTHER_BILLED_CUSTOMER_ID, "PAID");
    givenInvoice(DRAFT_INVOICE_ID, "REF-EXPORT4", BILLED_CUSTOMER_ID, "DRAFT");
    givenCreditPurchaseInvoicedBy(CREDIT_INVOICE_ID);
  }

  @AfterEach
  void cleanUp() {
    jdbcTemplate.update(
        "update \"user\" set roles = array_remove(roles, 'SUBSCRIPTION_INVOICE_EXPORTER')"
            + " where id = ?",
        JANE_DOE_ID);
    jdbcTemplate.update("delete from credit_purchase where id = ?", CREDIT_PURCHASE_ID);
    jdbcTemplate.update(
        "delete from \"invoice\" where id in (?, ?, ?, ?)",
        SUBSCRIPTION_INVOICE_ID,
        CREDIT_INVOICE_ID,
        OTHER_SUBSCRIPTION_INVOICE_ID,
        DRAFT_INVOICE_ID);
    jdbcTemplate.update(
        "delete from \"customer\" where id in (?, ?, ?)",
        BILLED_CUSTOMER_ID,
        OTHER_BILLED_CUSTOMER_ID,
        UNBILLED_CUSTOMER_ID);
  }

  @Test
  void admin_exports_one_folder_per_billed_client() throws ApiException {
    var api = new SubscriptionBillingApi(TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort));

    var actual = api.exportSubscriptionInvoices(EXPORTED_FROM, EXPORTED_TO);

    assertEquals(PRESIGNED_URL, actual.getValue());
    assertEquals(3600, actual.getExpirationDelay());
    assertEquals(
        List.of(
            "Couvreur Export SAS/Facture abonnement REF-EXPORT3.pdf",
            "Toiture Export SARL/Facture abonnement REF-EXPORT1.pdf",
            "Toiture Export SARL/Facture crédits REF-EXPORT2.pdf"),
        uploadedZipEntries());
  }

  @Test
  void the_subscription_invoice_exporter_role_alone_grants_the_export() throws ApiException {
    grantSubscriptionInvoiceExporterRoleTo(JANE_DOE_ID);
    var api = new SubscriptionBillingApi(TestUtils.anApiClient(JANE_DOE_TOKEN, null, localPort));

    var actual = api.exportSubscriptionInvoices(EXPORTED_FROM, EXPORTED_TO);

    assertEquals(PRESIGNED_URL, actual.getValue());
  }

  @Test
  void a_user_without_the_role_cannot_export() {
    var api = new SubscriptionBillingApi(TestUtils.anApiClient(JANE_DOE_TOKEN, null, localPort));

    assertThrowsForbiddenException(
        () -> api.exportSubscriptionInvoices(EXPORTED_FROM, EXPORTED_TO));
  }

  @Test
  void exporting_a_period_without_any_invoice_is_not_found() {
    var api = new SubscriptionBillingApi(TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort));

    var apiException =
        assertThrows(
            ApiException.class,
            () ->
                api.exportSubscriptionInvoices(
                    LocalDate.of(2031, 6, 1), LocalDate.of(2031, 6, 30)));

    assertEquals(404, apiException.getCode());
  }

  @Test
  void exporting_a_period_ending_before_it_starts_is_a_bad_request() {
    var api = new SubscriptionBillingApi(TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort));

    var apiException =
        assertThrows(
            ApiException.class, () -> api.exportSubscriptionInvoices(EXPORTED_TO, EXPORTED_FROM));

    assertEquals(400, apiException.getCode());
  }

  private List<String> uploadedZipEntries() {
    var zipCaptor = ArgumentCaptor.forClass(File.class);
    verify(s3ServiceMock)
        .uploadFile(eq(INVOICE_ZIP), anyString(), eq(USER_TO_CREDIT_ID), zipCaptor.capture());
    return zipEntriesOf(zipCaptor.getValue());
  }

  @SneakyThrows
  private static List<String> zipEntriesOf(File zipFile) {
    try (var zip = new ZipFile(zipFile)) {
      var entries = new ArrayList<String>();
      zip.stream().forEach(entry -> entries.add(entry.getName()));
      entries.sort(String::compareTo);
      return entries;
    }
  }

  @SneakyThrows
  private static File pdfNamed(String fileId) {
    var file = Files.createTempFile(fileId, ".pdf");
    Files.write(file, ("pdf of " + fileId).getBytes(StandardCharsets.UTF_8));
    return file.toFile();
  }

  private void givenCustomer(String customerId, String name) {
    jdbcTemplate.update(
        "insert into \"customer\" (id, id_user, name, email, phone, customer_type)"
            + " values (?, ?, ?, ?, '+261340000009', 'PROFESSIONAL'::customer_type)",
        customerId,
        USER_TO_CREDIT_ID,
        name,
        customerId + "@email.com");
  }

  private void givenInvoice(String invoiceId, String reference, String customerId, String status) {
    jdbcTemplate.update(
        "insert into \"invoice\" (id, id_user, title, \"ref\", id_customer, file_id, sending_date,"
            + " to_pay_at, status, archive_status, created_datetime, payment_type)"
            + " values (?, ?, 'Facture export', ?, ?, ?, '2031-05-15', '2031-05-15',"
            + " ?::invoice_status, 'ENABLED'::archive_status, '2031-05-15T10:00:00.00Z',"
            + " 'CASH'::payment_type)",
        invoiceId,
        USER_TO_CREDIT_ID,
        reference,
        customerId,
        "file_" + invoiceId,
        status);
  }

  private void grantSubscriptionInvoiceExporterRoleTo(String userId) {
    jdbcTemplate.update(
        "update \"user\" set roles = array_append(roles, 'SUBSCRIPTION_INVOICE_EXPORTER')"
            + " where id = ? and not 'SUBSCRIPTION_INVOICE_EXPORTER' = any (roles)",
        userId);
  }

  private void givenCreditPurchaseInvoicedBy(String invoiceId) {
    jdbcTemplate.update(
        "insert into credit_purchase (id, user_id, type, status, origin, invoice_id)"
            + " values (?, ?, 'PACK'::credit_purchase_type, 'COMPLETED'::credit_purchase_status,"
            + " 'SELF_SERVICE'::credit_purchase_origin, ?)",
        CREDIT_PURCHASE_ID,
        SUBSCRIBER_ID,
        invoiceId);
  }
}
