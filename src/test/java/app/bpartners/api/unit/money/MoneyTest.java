package app.bpartners.api.unit.money;

import static app.bpartners.api.model.Money.fromMajor;
import static app.bpartners.api.model.Money.fromMinor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

public class MoneyTest {
  @Test
  void money_manipulation_ok() {
    Fraction twoThirds = new Fraction(new BigInteger("2"), new BigInteger("3"));
    String stringValue1 = "2/3";
    int actualValue1 = 200; // Bridge
    Double actualValue2 = 20000.0; // Swan

    Money stringMajor = fromMajor(stringValue1);
    Money stringMinor = fromMinor(stringValue1);
    Money fromMinor = fromMinor(actualValue1);
    Money fromMajor = fromMajor(actualValue2);

    assertEquals(1, stringMajor.getCents());
    assertEquals((int) ((2.0 / 3.0) * 100) + 1, stringMinor.getCents());
    assertEquals(20000, fromMinor.getCents());
    assertEquals(20000, fromMajor.getCents());
  }
}
