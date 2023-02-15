package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"transaction\"")
@Setter
@ToString
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class HTransaction {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String idAccount;
  private String idSwan;
  private Integer amount;
  private String currency;
  private String label;
  private String reference;
  private String side;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private TransactionStatus status;
  @ManyToOne
  @JoinColumn(name = "id_invoice")
  private HInvoice invoice;
  private Instant paymentDateTime;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private TransactionTypeEnum type;

}
