package app.bpartners.api.model.credit;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "credit_transaction")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class CreditTransaction {
  @Id private String id;

  @Column(name = "user_id")
  private String userId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private CreditTransactionType type;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "movement_type")
  private CreditTransactionMovementType movementType;

  private Long credits;

  @Column(name = "expiration_datetime")
  private Instant expirationDatetime;

  @Column(updatable = false)
  private Instant creationDatetime;

  public boolean isCredit() {
    return CREDIT.equals(movementType);
  }

  public boolean isExpiredAt(Instant when) {
    return expirationDatetime != null && !expirationDatetime.isAfter(when);
  }

  public long creditsOrZero() {
    return credits == null ? 0L : credits;
  }
}
