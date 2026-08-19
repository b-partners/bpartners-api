package app.bpartners.api.model.credit;

import static app.bpartners.api.model.credit.CreditOrigin.SUBSCRIPTION_GRANT;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

import java.time.Instant;
import java.util.Comparator;
import lombok.Getter;

@Getter
public final class Lot {
  public static final Comparator<Lot> BY_EXPIRY_SOONEST_FIRST =
      comparing(Lot::getExpirationDatetime, nullsLast(naturalOrder()));

  private final CreditOrigin origin;
  private final Instant expirationDatetime;
  private final long remaining;

  public Lot(CreditTransaction transaction) {
    this(
        CreditOrigin.ofTransactionType(transaction.getType()),
        transaction.getExpirationDatetime(),
        transaction.creditsOrZero());
  }

  private Lot(CreditOrigin origin, Instant expirationDatetime, long remaining) {
    this.origin = origin;
    this.expirationDatetime = expirationDatetime;
    this.remaining = remaining;
  }

  public boolean isGranted() {
    return origin == SUBSCRIPTION_GRANT;
  }

  public boolean hasRemaining() {
    return remaining > 0;
  }

  public boolean expires() {
    return expirationDatetime != null;
  }

  public Lot reduceBy(long amount) {
    return new Lot(origin, expirationDatetime, Math.max(0L, remaining - amount));
  }

  public CreditExpiration toExpiration() {
    return CreditExpiration.builder()
        .credits(remaining)
        .expirationDatetime(expirationDatetime)
        .origin(origin)
        .build();
  }
}
