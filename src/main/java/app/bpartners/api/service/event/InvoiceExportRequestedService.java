package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static java.util.UUID.nameUUIDFromBytes;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.InvoiceExportRequested;
import app.bpartners.api.file.BucketKeyRetriever;
import app.bpartners.api.file.FileZipper;
import app.bpartners.api.file.InvoicesFileDownloader;
import app.bpartners.api.file.bucket.CustomBucketComponent;
import app.bpartners.api.model.InvoiceExportBatch;
import app.bpartners.api.model.InvoiceExportRequest;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceExportBatchRepository;
import app.bpartners.api.repository.jpa.InvoiceExportRequestRepository;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class InvoiceExportRequestedService implements Consumer<InvoiceExportRequested> {
  private final InvoiceExportRequestRepository invoiceExportRequestRepository;
  private final InvoiceExportBatchRepository invoiceExportBatchRepository;
  private final InvoiceRepository invoiceRepository;
  private final InvoicesFileDownloader invoicesFileDownloader;
  private final FileZipper fileZipper;
  private final CustomBucketComponent customBucketComponent;
  private final BucketKeyRetriever bucketKeyRetriever;
  private final EventProducer<InvoiceExportRequested> eventProducer;

  @Override
  public void accept(InvoiceExportRequested event) {
    var requestId = event.getInvoiceExportRequestId();
    var page = event.getPage();
    var request =
        invoiceExportRequestRepository
            .findById(requestId)
            .orElseThrow(
                () -> new NotFoundException("InvoiceExportRequest.id=" + requestId + " not found"));

    if (page >= request.getTotalBatchCount()) {
      log.warn(
          "Ignoring page={} of InvoiceExportRequest.id={} as it only has {} batch(es)",
          page,
          requestId,
          request.getTotalBatchCount());
      return;
    }

    var invoices =
        invoiceRepository.findAllByIdUserAndSendingDateBetweenAndCriteriaAndPaginate(
            request.getUserId(),
            request.getFrom(),
            request.getTo(),
            request.getStatusList(),
            request.getArchiveStatus(),
            page,
            request.getBatchSize());

    // deterministic so that a retried event overwrites its own batch instead of duplicating it
    var batchId = batchIdOf(requestId, page);
    var userId = request.getUserId();
    var zipFile = fileZipper.apply(invoicesFileDownloader.apply(userId, invoices));
    var fileKey = bucketKeyRetriever.apply(INVOICE_ZIP, batchId + ".zip", userId);

    customBucketComponent.upload(zipFile, fileKey, true);

    invoiceExportBatchRepository.save(
        InvoiceExportBatch.builder()
            .id(batchId)
            .invoiceExportRequest(request)
            .fileKey(fileKey)
            .contentSize(invoices.size())
            .properties(propertiesOf(page, zipFile))
            .build());

    chainNextPage(request, page);
  }

  private void chainNextPage(InvoiceExportRequest request, int page) {
    var nextPage = page + 1;
    if (nextPage < request.getTotalBatchCount()) {
      eventProducer.accept(
          List.of(
              InvoiceExportRequested.builder()
                  .invoiceExportRequestId(request.getId())
                  .page(nextPage)
                  .build()));
    }
  }

  private HashMap<String, Object> propertiesOf(int page, File zipFile) {
    var properties = new HashMap<String, Object>();
    properties.put("page", page);
    properties.put("fileSizeInBytes", zipFile.length());
    return properties;
  }

  private String batchIdOf(String requestId, int page) {
    return nameUUIDFromBytes((requestId + "-" + page).getBytes(StandardCharsets.UTF_8)).toString();
  }
}
