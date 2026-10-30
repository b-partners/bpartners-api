package app.bpartners.api.model.subscription;

public enum SubscriptionUpgradeStatus {
  PENDING,
  PROCESSING,
  AWAITING_PAYMENT,
  PAID,
  INVOICED,
  EXPIRED,
  ABANDONED
}
