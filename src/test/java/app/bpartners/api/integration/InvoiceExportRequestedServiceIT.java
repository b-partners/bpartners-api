package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceExportOutputFormat.ZIP;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.InvoiceExportRequested;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.InvoiceExportRequest;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.event.InvoiceExportRequestedService;
import app.bpartners.api.service.invoice.InvoiceExportRequestService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class InvoiceExportRequestedServiceIT extends MockedThirdParties {
  // joe_doe_id has 3 CONFIRMED and non archived invoices sent in September 2022
  private static final LocalDate FROM = LocalDate.of(2022, 9, 1);
  private static final LocalDate TO = LocalDate.of(2022, 9, 30);

  @Autowired InvoiceExportRequestedService subject;
  @Autowired InvoiceExportRequestService invoiceExportRequestService;
  @MockBean S3Service s3ServiceMock;
  @MockBean BucketComponent bucketComponentMock;

  @BeforeEach
  void setUp() throws IOException {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    // the downloader marks whatever S3 hands back as deleteOnExit, so never hand it a fixture
    var invoicePdf = File.createTempFile("invoice", ".pdf");
    when(s3ServiceMock.downloadFile(any(), any(), any())).thenReturn(invoicePdf);
  }

  @Test
  void create_then_export_all_batches_ok() {
    var request = createRequest(2);
    assertEquals(3, request.getTotalInvoiceCount());
    assertEquals(2, request.getTotalBatchCount());
    assertTrue(request.getBatchList().isEmpty());

    subject.accept(exportOf(request, 0));

    var afterFirstBatch = invoiceExportRequestService.getById(request.getId());
    assertEquals(ZIP, afterFirstBatch.getOutputFormat());
    assertEquals(List.of(CONFIRMED), afterFirstBatch.getStatusList());
    assertEquals(ENABLED, afterFirstBatch.getArchiveStatus());
    assertEquals(FROM, afterFirstBatch.getFrom());
    assertEquals(TO, afterFirstBatch.getTo());
    assertEquals(1, afterFirstBatch.getBatchList().size());
    var firstBatch = afterFirstBatch.getBatchList().getFirst();
    assertEquals(2, firstBatch.getContentSize());
    assertNotNull(firstBatch.getFileKey());
    assertEquals(0, firstBatch.getProperties().get("page"));
    verify(bucketComponentMock).upload(any(File.class), any(), any(Boolean.class));

    subject.accept(exportOf(request, 1));

    var afterLastBatch = invoiceExportRequestService.getById(request.getId());
    assertEquals(2, afterLastBatch.getBatchList().size());
    // last batch only holds the invoice left over by the first one
    assertEquals(
        List.of(1, 2),
        afterLastBatch.getBatchList().stream()
            .map(app.bpartners.api.model.InvoiceExportBatch::getContentSize)
            .sorted()
            .toList());
  }

  @Test
  void export_batch_chains_next_page_until_last_one() {
    var request = createRequest(2);

    subject.accept(exportOf(request, 0));

    verify(eventProducer).accept(List.of(exportOf(request, 1)));

    subject.accept(exportOf(request, 1));

    // last page: nothing left to chain
    verify(eventProducer, never()).accept(List.of(exportOf(request, 2)));
  }

  @Test
  void export_batch_is_idempotent_when_event_is_retried() {
    var request = createRequest(2);

    subject.accept(exportOf(request, 0));
    subject.accept(exportOf(request, 0));

    assertEquals(1, invoiceExportRequestService.getById(request.getId()).getBatchList().size());
  }

  private InvoiceExportRequest createRequest(int batchSize) {
    return invoiceExportRequestService
        .createInvoiceExportRequestList(
            List.of(
                InvoiceExportRequest.builder()
                    .id(randomUUID().toString())
                    .userId(JOE_DOE_ID)
                    .from(FROM)
                    .to(TO)
                    .statusList(List.of(CONFIRMED))
                    .archiveStatus(ENABLED)
                    .outputFormat(ZIP)
                    .batchSize(batchSize)
                    .build()))
        .getFirst();
  }

  private InvoiceExportRequested exportOf(InvoiceExportRequest request, int page) {
    return InvoiceExportRequested.builder()
        .invoiceExportRequestId(request.getId())
        .page(page)
        .build();
  }
}
