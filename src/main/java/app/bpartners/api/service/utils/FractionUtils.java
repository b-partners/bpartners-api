package app.bpartners.api.service.utils;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.exception.ApiException;
import java.math.BigInteger;
import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

@Component
public class FractionUtils {
  private FractionUtils() {
  }

  public static Aprational toAprational(BigInteger numerator, BigInteger denominator) {
    return new Aprational(new Apint(numerator), new Apint(denominator));
  }

  public static Fraction parseFraction(String fraction) {
    if (fraction == null) {
      return null;
    }
    if (!fraction.contains("/")) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION,
          "Invalid fraction format");
    }
    String[] array = fraction.split("/");
    return new Fraction(BigInteger.valueOf(Long.parseLong(array[0])),
        BigInteger.valueOf(Long.parseLong(array[1])));
  }

  public static Fraction parseFraction(Aprational aprational) {
    return new Fraction(aprational.numerator().toBigInteger(),
        aprational.denominator().toBigInteger());
  }

  public static Fraction parseFraction(Double number) {
    Aprational aprational = new Aprational(number);
    return parseFraction(aprational);
  }
}