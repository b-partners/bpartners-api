package app.bpartners.api.model.subscription;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ResolvedSubscriptionPlan {
  private final String id;
  private final String name;
}
