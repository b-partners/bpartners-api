package app.bpartners.api.model.credit;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CreditBalance {
  long spendableCredits;
  long grantedCredits;
  long purchasedCredits;
  long creditCostPerAnalysis;
  long estimatedRemainingAnalyses;
  Instant nextGrantDatetime;
  List<CreditExpiration> expirations;
  Instant updatedAt;
}
