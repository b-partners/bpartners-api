package app.bpartners.api.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.function.BinaryOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.apfloat.Aprational;

import static app.bpartners.api.service.utils.FractionUtils.toAprational;

@Getter
@Setter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Fraction implements Serializable {
  private BigInteger numerator;
  private BigInteger denominator;

  /*
   *NoArgsConstructor that returns 0
   */
  public Fraction() {
    this.numerator = BigInteger.ZERO;
    this.denominator = BigInteger.ONE;
  }

  /*
   *Constructs a fraction with 1 as denominator
   */
  public Fraction(BigInteger numerator) {
    this.numerator = numerator;
    this.denominator = BigInteger.ONE;
  }

  /*
   * @param fraction another fraction to apply a basic operation
   * @param operator any operation you can do with Aprational
   * @return the result of the operation
   * @Note Fraction sum = someFraction.operate(anotherFraction, (a,b) -> a.add(b))
   */
  public Fraction operate(Fraction fraction, BinaryOperator<Aprational> operator) {
    Aprational a = toAprational(getNumerator(), getDenominator());
    Aprational b = toAprational(fraction.getNumerator(), fraction.getDenominator());
    Aprational result = operator.apply(a, b);
    return new Fraction(result.numerator().toBigInteger(), result.denominator().toBigInteger());
  }

  public double getApproximatedValue() {
    return (numerator.doubleValue()) / (denominator.doubleValue());
  }

  public String getCentsOfApproximatedValue() {
    BigDecimal bigDecimal = BigDecimal
        .valueOf(numerator.doubleValue() / denominator.doubleValue())
        .setScale(2, RoundingMode.HALF_UP);
    return String.valueOf(bigDecimal);
  }

  public String getCentsHalfUp() {
    BigDecimal bigDecimal = BigDecimal
        .valueOf(numerator.doubleValue() / denominator.doubleValue() / 100)
        .setScale(2, RoundingMode.HALF_UP);
    return String.valueOf(bigDecimal).replace(".", ",");
  }

  public int getCents() {
    return (int) (Math.round(getApproximatedValue()) * 100);
  }


  @Override
  public String toString() {
    return numerator + "/" + denominator;
  }
}