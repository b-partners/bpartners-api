package app.bpartners.api.unit.currency;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import app.bpartners.api.service.utils.MoneyUtils;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyTest {
  @Test
  void money_manipulation_ok() {
    Fraction twoThirds = new Fraction(new BigInteger("2"), new BigInteger("3"));
    String twoThirdsStringValue = "2/3";
    Money fromString = MoneyUtils.fromMinorString(twoThirdsStringValue);

    int majorValue = 20000; //Bridge
    Money fromMajor = MoneyUtils.fromMajor(majorValue);

    Double minorValue = 200.0; //Swan
    Money fromMinor = MoneyUtils.fromMinor(minorValue);

    int roundedValue = 200;
    assertEquals(new Money().getValue(), new Fraction());
    assertEquals(
        twoThirds.getApproximatedValue(),
        fromString.getApproximatedValue());

    assertEquals(minorValue, fromMinor.getApproximatedValue());
    //TODO: * 100 because for now persisted fraction is in cent but should be its real value
    assertEquals(roundedValue, fromMinor.getRoundedValue() * 100);
    assertEquals(majorValue, fromMinor.getCentsRoundUp() * 100);

    assertEquals(minorValue, fromMajor.getApproximatedValue());
    assertEquals(roundedValue, fromMinor.getRoundedValue() * 100);
    assertEquals(majorValue, fromMajor.getCentsRoundUp() * 100);
  }
}
