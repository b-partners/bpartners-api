package app.bpartners.api.model.subscription;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionProductFeatureSection {
  private String title;
  private List<SubscriptionProductFeatureItem> items;
}
