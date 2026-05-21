package app.bpartners.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.HashMap;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity(name = "customer_export_requested")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class CustomerExportHistory {
  @Id private String id;

  private String userOwnerIdentifier;

  private String fileKey;

  @JdbcTypeCode(SqlTypes.JSON)
  private HashMap<String, Object> additionalProperties;

  private Instant creationDatetime;
}
