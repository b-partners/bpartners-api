package app.bpartners.api.model.credit;

public record CreditPurchaseCharge(String paymentIntentId, String failureCode) {
  public static final String NO_CHARGEABLE_CARD = "no_chargeable_card";

  public static CreditPurchaseCharge succeeded(String paymentIntentId) {
    return new CreditPurchaseCharge(paymentIntentId, null);
  }

  public static CreditPurchaseCharge failed(String failureCode) {
    return new CreditPurchaseCharge(null, failureCode);
  }

  public static CreditPurchaseCharge noChargeableCard() {
    return failed(NO_CHARGEABLE_CARD);
  }

  public boolean succeeded() {
    return paymentIntentId != null;
  }
}
