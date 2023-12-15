package app.bpartners.api.unit.fraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.utils.FractionUtils;
import java.math.BigInteger;
import lombok.AllArgsConstructor;
import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;

@AllArgsConstructor
class FractionTest {
  @Test
  void fraction_manipulation_ok() {
    String string = "2/3";
    int testInteger = 2;
    Aprational anAprational = new Aprational(new Apint(10), new Apint(100));

    Fraction fromString = FractionUtils.parseFraction(string);
    Fraction fromInteger = FractionUtils.parseFraction(testInteger);
    Fraction fromAprational = FractionUtils.parseFraction(anAprational);

    assertEquals(new Fraction(BigInteger.TWO, BigInteger.valueOf(3)), fromString);
    assertEquals(testInteger, fromInteger.getApproximatedValue());
    assertEquals(new Fraction(BigInteger.ONE, BigInteger.TEN), fromAprational);
  }

  @Test
  void parseFraction_ko() {
    assertThrows(
        ApiException.class, () -> FractionUtils.parseFraction("2"), "Invalid fraction format");
  }
}
