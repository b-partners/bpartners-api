package app.bpartners.api.model;

import static org.hibernate.type.SqlTypes.JSON;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "invoice_export_batch")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class InvoiceExportBatch {
  @Id private String id;

  private Integer contentSize;

  private String fileKey;

  @JdbcTypeCode(JSON)
  private HashMap<String, Object> properties;

  @JoinColumn(name = "invoice_export_request_id")
  @ManyToOne
  private InvoiceExportRequest invoiceExportRequest;

  @Column(updatable = false)
  private Instant creationDatetime;

  @PrePersist
  private void prePersist() {
    creationDatetime = Instant.now();
  }
}
