package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.IN_INSTALMENT;

@Component
public class PaymentRegValidator implements Consumer<CreatePaymentRegulation> {

  public void accept(List<CreatePaymentRegulation> payments, Integer totalPriceWithVat) {
    payments.forEach(this);
    AtomicReference<Integer> totalAmount = new AtomicReference<>(0);
    AtomicReference<Integer> totalPercent = new AtomicReference<>(0);
    if (payments.size() < 2) {
      throw new BadRequestException("Multiple payments request more than one payment");
    }
    payments.forEach(payment -> {
      if (payment.getAmount() != null) {
        totalAmount.set(payment.getAmount() + totalAmount.get());
      }
      if (payment.getPercent() != null) {
        totalPercent.set(payment.getPercent() + totalPercent.get());
      }
    });
    if (totalAmount.get() != 0
        && !Objects.equals(totalAmount.get(), totalPriceWithVat)) {
      throw new BadRequestException("Multiple payments amount " + totalAmount.get()
          + " is not equals to total price with vat " + totalPriceWithVat);
    }
    if (totalPercent.get() != 0
        && totalPercent.get() != 10000) {
      throw new BadRequestException("Multiple payments percent "
          + (double) totalPercent.get() / 100 + "% is not equals to 100%");
    }
  }

  @Override
  public void accept(CreatePaymentRegulation invoicePayment) {
    StringBuilder exceptionBuilder = new StringBuilder();
    Integer percent = invoicePayment.getPercent();
    if (invoicePayment.getMaturityDate() == null) {
      exceptionBuilder.append("Maturity date is mandatory. ");
    }
    if (percent == null && invoicePayment.getAmount() == null) {
      exceptionBuilder.append("Either percent or amount is mandatory");
    }
    if (percent != null
        && (percent < 0 || percent > 10000)
        && invoicePayment.getAmount() == null) {
      exceptionBuilder.append("Percent can not be less than 0 or greater than 100");
    }
    String message = exceptionBuilder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }
}
