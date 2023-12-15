package app.bpartners.api.repository.jpa.model;

import static javax.persistence.GenerationType.IDENTITY;

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

@Entity
@Table(name = "\"user_invoice_relaunch_conf\"")
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HUserInvoiceRelaunchConf {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String idUser;
  private int draftRelaunch;
  private int unpaidRelaunch;
  @UpdateTimestamp private Instant updatedAt;
}
