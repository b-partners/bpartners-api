package app.bpartners.api.model;

import java.io.Serializable;
import java.util.function.BinaryOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class Money implements Serializable {
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

  public Double getRoundedValue() {
    return value.getCentsAsDecimal() * 100;
  }

  public int compareTo(Money money) {
    return value.compareTo(money.getValue());
  }

  public Money operate(Money money, BinaryOperator<Fraction> operator) {
    return new Money(operator.apply(value, money.getValue()));
  }
}