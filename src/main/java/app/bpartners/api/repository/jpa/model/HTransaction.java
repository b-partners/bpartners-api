package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

@Entity
@Table(name = "\"transaction\"")
@Setter
@ToString
@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode
public class HTransaction {
  @Id private String id;
  private String idAccount;
  private Long idBridge;

  @ManyToOne
  @JoinColumn(name = "id_invoice")
  private HInvoice invoice;

  private String amount;
  private String currency;
  private String label;
  private String reference;
  private String side;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private TransactionStatus status;

  // Used to filter transactions summary only for enabled
  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_enable_status")
  private EnableStatus enableStatus;

  private Instant paymentDateTime;

  public String describe() {
    return "Transaction("
        + "id='"
        + id
        + '\''
        + ", idBridge="
        + idBridge
        + ", label='"
        + label
        + '\''
        + ')';
  }
}
