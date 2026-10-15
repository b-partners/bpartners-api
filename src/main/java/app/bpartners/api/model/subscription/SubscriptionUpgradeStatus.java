package app.bpartners.api.model.subscription;

public enum SubscriptionUpgradeStatus {
  PENDING,
  AWAITING_PAYMENT,
  PAID,
  INVOICED,
  EXPIRED,
  ABANDONED
}
