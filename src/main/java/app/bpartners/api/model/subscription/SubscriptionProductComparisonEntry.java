package app.bpartners.api.model.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionProductComparisonEntry {
  private String sectionTitle;
  private String label;
  private SubscriptionProductComparisonCellKind kind;
  private String text;
}
