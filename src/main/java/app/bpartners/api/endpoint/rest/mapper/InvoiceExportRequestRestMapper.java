package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.InvoiceExportOutputFormat.ZIP;
import static app.bpartners.api.service.invoice.InvoiceExportRequestService.DEFAULT_BATCH_SIZE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceExportRequest;
import app.bpartners.api.endpoint.rest.model.InvoiceExportBatch;
import app.bpartners.api.endpoint.rest.model.InvoiceExportRequest;
import app.bpartners.api.file.bucket.CustomBucketComponent;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceExportRequestRestMapper {
  private static final Duration URL_EXPIRATION = Duration.ofHours(1L);
  private final CustomBucketComponent customBucketComponent;

  public app.bpartners.api.model.InvoiceExportRequest toDomain(
      String uId, CreateInvoiceExportRequest rest) {
    return app.bpartners.api.model.InvoiceExportRequest.builder()
        .id(rest.getId() == null ? randomUUID().toString() : rest.getId())
        .userId(uId)
        .from(rest.getFrom())
        .to(rest.getTo())
        .archiveStatus(rest.getArchiveStatus())
        .statusList(rest.getStatusList())
        .outputFormat(rest.getOutputFormat() == null ? ZIP : rest.getOutputFormat())
        .batchSize(rest.getBatchSize() == null ? DEFAULT_BATCH_SIZE : rest.getBatchSize())
        .build();
  }

  public InvoiceExportRequest toRest(
      app.bpartners.api.model.InvoiceExportRequest invoiceExportRequest) {
    var batchList =
        invoiceExportRequest.getBatchList() == null
            ? List.<app.bpartners.api.model.InvoiceExportBatch>of()
            : invoiceExportRequest.getBatchList();
    return new InvoiceExportRequest()
        .id(invoiceExportRequest.getId())
        .from(invoiceExportRequest.getFrom())
        .to(invoiceExportRequest.getTo())
        .archiveStatus(invoiceExportRequest.getArchiveStatus())
        .statusList(invoiceExportRequest.getStatusList())
        .batchSize(invoiceExportRequest.getBatchSize())
        .totalBatchCount(invoiceExportRequest.getTotalBatchCount())
        .totalInvoiceCount(invoiceExportRequest.getTotalInvoiceCount())
        .outputFormat(invoiceExportRequest.getOutputFormat())
        .batchList(batchList.stream().map(this::toRest).toList());
  }

  public InvoiceExportBatch toRest(app.bpartners.api.model.InvoiceExportBatch invoiceExportBatch) {
    var presignedUrl =
        customBucketComponent.presign(invoiceExportBatch.getFileKey(), URL_EXPIRATION);
    return new InvoiceExportBatch()
        .url(presignedUrl == null ? null : presignedUrl.toString())
        .contentSize(invoiceExportBatch.getContentSize())
        .creationDatetime(invoiceExportBatch.getCreationDatetime())
        .properties(invoiceExportBatch.getProperties())
        .urlExpirationDatetime(now().plus(URL_EXPIRATION));
  }
}
