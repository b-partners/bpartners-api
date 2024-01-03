package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

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
  @Id
  private String id;
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
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private TransactionStatus status;
  //Used to filter transactions summary only for enabled
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_enable_status")
  private EnableStatus enableStatus;
  private Instant paymentDateTime;

  public String describe() {
    return "Transaction("
        + "id='" + id + '\''
        + ", idBridge=" + idBridge
        + ", label='" + label + '\''
        + ')';
  }
}
