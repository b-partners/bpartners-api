package app.bpartners.api.model;

import java.util.function.BinaryOperator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Money {
  private Fraction value;

  public Money(Double fromMinor) {
    value = parseFraction(fromMinor);
  }

  public Money(Integer fromMajor) {
    value = parseFraction(fromMajor * 100);
  }

  public Money(String fromMinorString) {
    value = parseFraction(fromMinorString);
  }

  public double getApproximatedValue() {
    return value.getApproximatedValue();
  }

  public Integer getCentsRoundUp() {
    return (int) (getRoundedValue() * 100);
  }

  public Double getRoundedValue() {
    return value.getCentsAsDecimal();
  }

  public int compareTo(Money money) {
    return value.compareTo(money.getValue());
  }

  public Money operate(Money money, BinaryOperator<Fraction> operator) {
    return new Money(operator.apply(value, money.getValue()));
  }

}