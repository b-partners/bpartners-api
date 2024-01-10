package app.bpartners.api.model;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

import app.bpartners.api.model.exception.NotImplementedException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.function.BinaryOperator;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;

@Data
@Slf4j
@Builder
@EqualsAndHashCode
public class Money implements Serializable {
  public static final String DOUBLE_TYPE_NAME = "java.lang.Double";
  public static final String INTEGER_TYPE_NAME = "java.lang.Integer";
  public static final String STRING_TYPE_NAME = "java.lang.String";
  private Fraction value;

  public Money() {
    value = new Fraction();
  }

  public Money(Fraction value) {
    this.value = value;
  }

  public Integer getTenths() {
    return (getCents() * 100);
  }

  public Integer getCents() {
    Double centsAsDecimal = value.getCentsAsDecimal();
    return (int) (centsAsDecimal * 100.0);
  }

  public Double getApproximatedValue() {
    return value.getApproximatedValue();
  }

  public Money operate(Money money, BinaryOperator<Fraction> operator) {
    return new Money(operator.apply(value, money.getValue()));
  }

  public static Money fromMinor(Object minorValue) {
    Class<?> clazz = minorValue.getClass();
    switch (clazz.getTypeName()) {
      case INTEGER_TYPE_NAME:
        return Money.builder().value(parseFraction((Integer) minorValue * 100)).build();
      case DOUBLE_TYPE_NAME:
        return Money.builder().value(parseFraction((Double) minorValue * 100)).build();
      case STRING_TYPE_NAME:
        return Money.builder()
            .value(
                parseFraction((String) minorValue)
                    .operate(new Fraction(BigInteger.valueOf(100L)), Aprational::multiply))
            .build();
      default:
        throw new NotImplementedException(
            clazz.getTypeName() + " conversion to Money is not supported");
    }
  }

  public static Money fromMajor(Object majorValue) {
    Class<?> clazz = majorValue.getClass();
    switch (clazz.getTypeName()) {
      case DOUBLE_TYPE_NAME:
        return Money.builder().value(parseFraction((Double) majorValue)).build();
      case INTEGER_TYPE_NAME:
        return Money.builder().value(parseFraction((Integer) majorValue)).build();
      case STRING_TYPE_NAME:
        return Money.builder().value(parseFraction((String) majorValue)).build();
      default:
        throw new NotImplementedException(
            clazz.getTypeName() + " conversion to Money is not supported");
    }
  }

  public String stringValue() {
    return String.valueOf(getValue());
  }
}
