package app.bpartners.api.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.*;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceExportOutputFormat;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "invoice_export_request")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class InvoiceExportRequest {
  @Id private String id;

  private String userId;

  @Column(name = "begin_date")
  private LocalDate from;

  @Column(name = "end_date")
  private LocalDate to;

  @JdbcTypeCode(NAMED_ENUM)
  private InvoiceExportOutputFormat outputFormat;

  private Integer batchSize;

  private Integer totalBatchCount;

  private Integer totalInvoiceCount;

  @OneToMany(mappedBy = "invoiceExportRequest", fetch = EAGER, cascade = ALL, orphanRemoval = true)
  private List<InvoiceExportBatch> batchList;

  @JdbcTypeCode(JSON)
  private List<InvoiceStatus> statusList;

  @JdbcTypeCode(NAMED_ENUM)
  private ArchiveStatus archiveStatus;

  @Column(updatable = false)
  private Instant creationDatetime;

  @PrePersist
  private void prePersist() {
    creationDatetime = Instant.now();
  }
}
