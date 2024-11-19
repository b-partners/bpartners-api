package app.bpartners.api.model.subscription;

import java.time.Instant;
import java.util.List;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionProduct {
  private String id;
  private String e2Id;
  private String name;
  private String description;
  private List<String> features;
  private String imageUrl;
  private SubscriptionType type;
  private Long priceInCents;
  private Instant creationDatetime;
}
