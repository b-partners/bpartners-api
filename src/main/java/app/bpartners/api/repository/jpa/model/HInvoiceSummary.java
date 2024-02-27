package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "\"invoice_summary\"")
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoiceSummary implements Serializable {
  @Id private String id;
  private String idUser;
  private String paidAmount;
  private String unpaidAmount;
  private String proposalAmount;
  private Instant updatedAt;
}
