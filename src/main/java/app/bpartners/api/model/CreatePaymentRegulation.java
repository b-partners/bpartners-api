package app.bpartners.api.model;

import static app.bpartners.api.service.utils.FileUtils.base64Image;
import static org.apfloat.Apcomplex.ZERO;

import app.bpartners.api.service.utils.QrCodeUtils;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apfloat.Aprational;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CreatePaymentRegulation implements Serializable {
  private PaymentRequest paymentRequest;
  private Fraction percent;
  private LocalDate maturityDate;
  private String comment;
  private Instant initiatedDatetime;

  public Fraction getAmountOrPercent(Fraction totalAmount) {
    if (paymentRequest.getAmount() != null && paymentRequest.getAmount().getCentsRoundUp() != 0) {
      return paymentRequest.getAmount();
    }
    return percent.operate(
        totalAmount,
        (percentValue, amountValue) -> {
          Aprational percentAprational = ZERO.add(percentValue.divide(new Aprational(10000)));
          return amountValue.multiply(percentAprational);
        });
  }

  public String getPaymentUrlAsQrCode() {
    if (paymentRequest.getPaymentUrl() == null) {
      return null;
    }
    return base64Image(QrCodeUtils.generateQrCode(paymentRequest.getPaymentUrl()));
  }

  public Date getFormattedMaturityDate() {
    return maturityDate == null
        ? null
        : Date.from(maturityDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
