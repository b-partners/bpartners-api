package app.bpartners.api.model.subscription;

import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity(name = "subscription_product")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionProduct {
  @Id private String id;

  @Column(name = "e2_id")
  private String e2Id;

  private String name;
  private String description;

  @JdbcTypeCode(JSON)
  private List<String> features;

  private String imageUrl;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private SubscriptionType type;

  private Long priceInCents;
  private Instant creationDatetime;
}
