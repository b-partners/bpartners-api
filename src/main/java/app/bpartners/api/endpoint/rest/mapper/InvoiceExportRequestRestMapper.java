package app.bpartners.api.endpoint.rest.mapper;

import static java.time.Instant.now;

import app.bpartners.api.endpoint.rest.model.InvoiceExportBatch;
import app.bpartners.api.endpoint.rest.model.InvoiceExportRequest;
import app.bpartners.api.file.bucket.BucketComponent;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceExportRequestRestMapper {
  private final BucketComponent bucketComponent;

  public InvoiceExportRequest toRest(
      app.bpartners.api.model.InvoiceExportRequest invoiceExportRequest) {
    return new InvoiceExportRequest()
        .id(invoiceExportRequest.getId())
        .from(invoiceExportRequest.getFrom())
        .to(invoiceExportRequest.getTo())
        .archiveStatus(invoiceExportRequest.getArchiveStatus())
        .statusList(invoiceExportRequest.getStatusList())
        .batchSize(invoiceExportRequest.getBatchSize())
        .totalInvoiceCount(invoiceExportRequest.getTotalInvoiceCount())
        .outputFormat(invoiceExportRequest.getOutputFormat())
        .batchList(
            invoiceExportRequest.getBatchList().stream()
                .map(invoiceExportBatch -> toRest(invoiceExportBatch))
                .toList());
  }

  public InvoiceExportBatch toRest(app.bpartners.api.model.InvoiceExportBatch invoiceExportBatch) {
    var urlExpiration = Duration.ofHours(1L);
    return new InvoiceExportBatch()
        .url(bucketComponent.presign(invoiceExportBatch.getFileKey(), urlExpiration).toString())
        .contentSize(invoiceExportBatch.getContentSize())
        .creationDatetime(invoiceExportBatch.getCreationDatetime())
        .properties(invoiceExportBatch.getProperties())
        .urlExpirationDatetime(now().plus(urlExpiration));
  }
}
