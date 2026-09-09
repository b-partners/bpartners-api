package app.bpartners.api.model.subscription;

public record SubscriptionTrialEligibility(
    String subscriptionProductId, boolean eligible, TrialIneligibilityReason reason) {

  public static SubscriptionTrialEligibility of(
      String subscriptionProductId, TrialIneligibilityReason reason) {
    return new SubscriptionTrialEligibility(
        subscriptionProductId, reason == TrialIneligibilityReason.ELIGIBLE, reason);
  }
}
