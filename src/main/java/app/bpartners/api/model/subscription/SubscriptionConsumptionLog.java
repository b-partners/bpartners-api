package app.bpartners.api.model.subscription;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "subscription_consumption_log")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionConsumptionLog {
  @Id private String id;
  private String userId;
  private Long usageMetric;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private SubscriptionConsumptionType consumptionType;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private SubscriptionConsumptionUnit consumptionUnit;

  private String comment;

  private Instant creationDatetime;
}
