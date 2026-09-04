package app.bpartners.api.model.credit;

import static app.bpartners.api.model.credit.CreditOrigin.SUBSCRIPTION_GRANT;

import java.time.Instant;
import lombok.Getter;

@Getter
public final class Lot {
  private final CreditOrigin origin;
  private final Instant expirationDatetime;
  private final long remaining;

  private Lot(CreditOrigin origin, Instant expirationDatetime, long remaining) {
    this.origin = origin;
    this.expirationDatetime = expirationDatetime;
    this.remaining = remaining;
  }

  static Lot of(CreditOrigin origin, Instant expirationDatetime, long remaining) {
    return new Lot(origin, expirationDatetime, remaining);
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

  public CreditExpiration toExpiration() {
    return CreditExpiration.builder()
        .credits(remaining)
        .expirationDatetime(expirationDatetime)
        .origin(origin)
        .build();
  }
}
