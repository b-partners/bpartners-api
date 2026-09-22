package app.bpartners.api.service.subscription;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SubscriptionBillingTimeSeries {
  BillingGranularity granularity;
  List<String> labels;
  List<Long> activeMonthlySubscriptions;
  List<Long> activeAnnualSubscriptions;
  List<Long> newMonthlySubscriptions;
  List<Long> newAnnualSubscriptions;
  List<Long> billedMonthlyInstalments;
  List<Long> billedAnnualInstalments;
  List<Long> requestsWithoutPlan;
  List<PlanSeries> overageRequestsByPlan;
  List<Long> paidInvoicesCount;
  List<Long> paidInvoicesAmountInCentsWithVat;

  @Value
  @Builder
  public static class PlanSeries {
    String planName;
    List<Long> values;
  }
}
