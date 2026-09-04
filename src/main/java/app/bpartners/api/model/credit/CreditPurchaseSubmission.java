package app.bpartners.api.model.credit;

public record CreditPurchaseSubmission(
    String purchaseId,
    CreditPurchaseType type,
    String creditPackId,
    Integer quantity,
    Long credits,
    String successUrl,
    String failureUrl) {
  public int quantityOrDefault() {
    return quantity == null ? 1 : quantity;
  }
}
