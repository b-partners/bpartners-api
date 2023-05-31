package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apfloat.Aprational;

/*
  /!\ IMPORTANT !
  Money value is in minor so persisted value must always be in minor
 */
@Getter
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class Money {
  private Fraction value;

  public Money() {
    value = new Fraction();
  }

  public double getApproximatedValue() {
    return value.getApproximatedValue();
  }

  public Integer getCentsRoundUp() {
    return (int) (getRoundedValue() * 100);
  }

  //TODO: delete when old values are correctly mapped
  public Integer getIntValue() {
    return value.getIntValue();
  }

  public Double getRoundedValue() {
    return value.getCentsAsDecimal();
  }

  public Money add(Money money) {
    return new Money(value.operate(money.getValue(), Aprational::add));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}