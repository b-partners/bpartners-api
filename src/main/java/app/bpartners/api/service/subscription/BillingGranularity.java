package app.bpartners.api.service.subscription;

import java.util.Locale;

public enum BillingGranularity {
  DAY,
  WEEK,
  MONTH;

  public static BillingGranularity fromNullable(String value) {
    if (value == null || value.isBlank()) {
      return DAY;
    }
    return BillingGranularity.valueOf(value.trim().toUpperCase(Locale.ROOT));
  }
}
