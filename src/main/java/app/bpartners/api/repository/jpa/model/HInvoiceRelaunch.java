package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.RelaunchType;
import java.io.Serializable;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"invoice_relaunch\"")
@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoiceRelaunch implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @Column(name = "\"type\"")
  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private RelaunchType type;

  @ManyToOne
  @JoinColumn(name = "id_invoice")
  private HInvoice invoice;

  private boolean isUserRelaunched;
  @CreationTimestamp private Instant creationDatetime;
  private String object;
  private String emailBody;
  private String attachmentFileId;
}
