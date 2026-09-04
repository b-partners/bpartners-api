package app.bpartners.api.model.credit;

public record CreditUnitPrice(long inCentsWithoutVat, long vatPercent) {
  public long inCentsWithVat() {
    return withVat(inCentsWithoutVat);
  }

  public Long totalInCentsWithoutVat(Long credits) {
    return credits == null ? null : credits * inCentsWithoutVat;
  }

  public Long totalInCentsWithVat(Long credits) {
    return credits == null ? null : withVat(credits * inCentsWithoutVat);
  }

  private long withVat(long inCentsWithoutVat) {
    var numerator = inCentsWithoutVat * (10_000L + vatPercent);
    return (numerator + 5_000L) / 10_000L;
  }
}
