package app.bpartners.api.model;

import app.bpartners.api.service.utils.QrCodeUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apfloat.Aprational;

import static app.bpartners.api.service.utils.FileUtils.base64Image;
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

  public String getPaymentUrlAsQrCode() {
    if (paymentUrl == null) {
      return null;
    }
    return base64Image(QrCodeUtils.generateQrCode(paymentUrl));
  }

  public Date getFormattedMaturityDate() {
    return Date.from(maturityDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }
}
