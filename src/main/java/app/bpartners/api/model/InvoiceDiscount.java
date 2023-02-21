package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apfloat.Aprational;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class InvoiceDiscount {
  private Fraction percentValue;
  private Fraction amountValue;

  public Fraction getPercent(Fraction totalPrice) {
    if (percentValue == null && amountValue != null) {
      return totalPrice.operate(amountValue, Aprational::divide);
    }
    return percentValue;
  }

  public Fraction getAmount(Fraction totalPrice) {
    if (amountValue == null && percentValue != null) {
      return totalPrice.operate(percentValue,
          (price, percent) -> {
            Aprational percentRational = percent.divide(new Aprational(1000));
            return price.multiply(percentRational);
          });
    }
    return amountValue;
  }
}
