package app.bpartners.api.service.utils;

import app.bpartners.api.model.Money;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

public class MoneyUtils {
  private MoneyUtils() {
  }

  public static Money fromMinor(Double minorValue) {
    return Money.builder()
        .value(parseFraction(minorValue))
        .build();
  }

  public static Money fromMinorString(String minorValue) {
    return Money.builder()
        .value(parseFraction(minorValue))
        .build();
  }

  public static Money fromMajor(Double majorValue) {
    return Money.builder()
        .value(parseFraction(majorValue / 100))
        .build();
  }

  public static Money fromMajor(Integer majorValue) {
    return Money.builder()
        .value(parseFraction(majorValue / 100))
        .build();
  }
}
