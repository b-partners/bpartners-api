package app.bpartners.api.service.subscription;

public sealed interface UpgradeCharge {
  String invoiceId();

  record Paid(String invoiceId) implements UpgradeCharge {}

  record ActionRequired(String invoiceId, String paymentUrl) implements UpgradeCharge {}
}
