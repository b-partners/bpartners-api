package app.bpartners.api.model.credit;

public enum CreditOrigin {
  SUBSCRIPTION_GRANT,
  PURCHASE,
  ADJUSTMENT;

  public static CreditOrigin ofTransactionType(CreditTransactionType type) {
    return switch (type) {
      case SUBSCRIPTION_GRANT -> SUBSCRIPTION_GRANT;
      case ADJUSTMENT -> ADJUSTMENT;
      default -> PURCHASE;
    };
  }
}
