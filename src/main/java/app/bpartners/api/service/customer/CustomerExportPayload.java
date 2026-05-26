package app.bpartners.api.service.customer;

import java.time.Instant;
import java.util.Objects;

public record CustomerExportPayload(
    String internalCustomerName,
    String email,
    String stripeCustomerId,
    String stripeCustomerName,
    boolean unknown,
    Instant stripeCreationDatetime) {

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CustomerExportPayload that)) return false;
    return Objects.equals(stripeCustomerId, that.stripeCustomerId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(stripeCustomerId);
  }
}
