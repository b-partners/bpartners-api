package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"monthly_transactions_summary\"")
@Getter
@Setter
@ToString
@AllArgsConstructor
@Builder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode
public class HMonthlyTransactionsSummary {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String idAccount;
  private String idUser;

  @Column(name = "\"year\"")
  private int year;

  @Column(name = "\"month\"")
  private int month;

  private String income;
  private String outcome;
  private String cashFlow;
  @CreationTimestamp private Instant updatedAt;
}
