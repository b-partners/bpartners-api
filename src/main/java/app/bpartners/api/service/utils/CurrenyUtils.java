package app.bpartners.api.service.utils;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class CurrenyUtils {

  private CurrenyUtils() {
  }

  public static Fraction toFraction(Money money) {
    return money.getValue();
  }

  public static Money parseMoney(Double money) {
    if (money == null) {
      return new Money();
    }

    return new Money(parseFraction(money));
  }

  public static Money parseMoney(String fraction) {
    if (fraction == null) {
      return new Money();
    }

    return new Money(parseFraction(fraction));
  }

  public static Money parseMoney(Fraction fraction) {
    if (fraction == null) {
      return new Money();
    }
    return new Money(fraction);
  }

  public static Money parseMoney(Integer number) {
    if (number == null) {
      return new Money();
    }
    return new Money(parseFraction(number));
  }

}