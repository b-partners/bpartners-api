package app.bpartners.api.unit.money;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.model.Money.fromMajor;
import static app.bpartners.api.model.Money.fromMinor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyTest {
  @Test
  void money_manipulation_ok() {
    Fraction twoThirds = new Fraction(new BigInteger("2"), new BigInteger("3"));
    String twoThirdsStringValue = "2/3";
    Money fromString = fromMinor(twoThirdsStringValue);

    int majorValue = 20000; //Bridge
    Money fromMajor = fromMajor(majorValue);

    Double minorValue = 200.0; //Swan
    Money fromMinor = fromMinor(minorValue);

    int roundedValue = 200;
    assertEquals(
        twoThirds.getApproximatedValue(),
        fromString.getApproximatedValue());

    assertEquals(minorValue, fromMinor.getCents());
    assertEquals(roundedValue, fromMinor.getCents());
    assertEquals(majorValue, fromMinor.getTenths());

    assertEquals(minorValue, fromMajor.getCents());
    assertEquals(roundedValue, fromMinor.getCents());
    assertEquals(majorValue, fromMajor.getTenths());
  }
}
