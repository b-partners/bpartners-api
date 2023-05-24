package app.bpartners.api.model;

import java.util.function.BinaryOperator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static app.bpartners.api.service.utils.CurrenyUtils.toFraction;

@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Slf4j
public class Money {
  private Fraction value;

  public Money() {
    value = new Fraction();
  }

  public double getApproximatedValue() {
    return value.getApproximatedValue();
  }

  public Integer getCentsRoundUp() {
    return (int) (getCentsAsDecimal() * 100);
  }

  public Double getCentsAsDecimal() {
    return value.getCentsAsDecimal();
  }

  public int compareTo(Money money) {
    Fraction thisMoney = toFraction(this);
    Fraction otherMoney = toFraction(money);
    return thisMoney.compareTo(otherMoney);
  }

  public Money operate(Money money, BinaryOperator<Fraction> operator) {
    Fraction a = toFraction(this);
    Fraction b = toFraction(money);
    Fraction result = operator.apply(a, b);
    return new Money(result);
  }

}