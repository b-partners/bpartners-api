package app.bpartners.api.unit.currency;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import app.bpartners.api.service.utils.CurrenyUtils;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyTest {
  @Test
  void currency_manipulation_ok() {
    String string = "2/3";
    int testInteger = 200;
    Fraction fraction = new Fraction(new BigInteger("5"));

    Money fromString = CurrenyUtils.parseMoney(string);
    Money fromInteger = CurrenyUtils.parseMoney(testInteger);
    Money fromFraction = CurrenyUtils.parseMoney(fraction);

    assertEquals(new Fraction(BigInteger.TWO, new BigInteger("3")).getApproximatedValue(),
        fromString.getApproximatedValue());
    assertEquals(testInteger, fromInteger.getApproximatedValue());
    assertEquals(2, fromInteger.getCentsAsDecimal());
    assertEquals(200, fromInteger.getCentsRoundUp());
    assertEquals(5, fromFraction.getApproximatedValue());
  }
}
