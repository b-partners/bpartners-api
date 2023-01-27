package app.bpartners.api.model;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apfloat.Aprational;

import static org.apfloat.Apcomplex.ZERO;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CreatePaymentRegulation {
  private String endToEndId;
  private String paymentUrl;
  private String reference;
  private String payerName;
  private String payerEmail;
  private Fraction amount;
  private Fraction percent;
  private LocalDate maturityDate;
  private String comment;
  private Instant initiatedDatetime;

  public Fraction getAmountOrPercent(Fraction totalAmount) {
    if (amount != null && amount.getCentsRoundUp() != 0) {
      return amount;
    }
    return percent.operate(totalAmount,
        (percentValue, amountValue) -> {
          Aprational percentAprational = ZERO.add(percentValue.divide(new Aprational(10000)));
          return amountValue.multiply(percentAprational);
        });
  }
}
