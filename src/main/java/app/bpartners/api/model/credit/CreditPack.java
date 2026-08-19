package app.bpartners.api.model.credit;

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

@Entity(name = "credit_pack")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class CreditPack {
  @Id private String id;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private CreditCode code;

  private String description;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = "credit_purchase_type")
  private CreditPurchaseType creditPurchaseType;

  private Long credits;

  @Column(name = "validity_days")
  private Integer validityDays;

  @Column(name = "most_chosen")
  private boolean mostChosen;

  private boolean deprecated;

  @Column(name = "display_position")
  private Integer displayPosition;

  @Column(updatable = false)
  private Instant creationDatetime;
}
