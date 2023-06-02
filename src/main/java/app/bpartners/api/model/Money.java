package app.bpartners.api.model;

import java.math.BigInteger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apfloat.Aprational;

/*
  TODO: /!\ IMPORTANT !
  Money value is in minor so persisted value must always be in minor
  Actually, persisted value is in major
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder(toBuilder = true)
public class Money {
  private Fraction value;

  public Money(Long longValue) {
    value = new Fraction(BigInteger.valueOf(longValue));
  }

  public Money() {
    value = new Fraction();
  }

  public double getApproximatedValue() {
    return value.getApproximatedValue();
  }

  public Integer getCentsRoundUp() {
    return value.getCentsRoundUp();
  }

  public Double getRoundedValue() {
    return value.getCentsAsDecimal();
  }

  public Money add(Money money) {
    return new Money(value.operate(money.getValue(), Aprational::add));
  }

  public Money multiply(Money money) {
    return new Money(value.operate(money.getValue(), Aprational::multiply));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}