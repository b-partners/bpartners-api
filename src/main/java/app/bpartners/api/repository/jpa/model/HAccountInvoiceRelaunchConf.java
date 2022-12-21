package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
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
import org.hibernate.annotations.UpdateTimestamp;
import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"account_invoice_relaunch_conf\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HAccountInvoiceRelaunchConf {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String accountId;
  private int draftRelaunch;
  private int unpaidRelaunch;
  @UpdateTimestamp
  private Instant updatedAt;
}