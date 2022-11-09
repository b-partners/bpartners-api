package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"transaction_category\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HTransactionCategory {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String idTransaction;
  private String idAccount;
  @Column(name = "id_transaction_category_tmpl")
  private String idCategoryTemplate;
  @Column(name = "\"type\"")
  private String type;
  @CreationTimestamp
  private Instant createdDatetime;
  private String comment;
}
