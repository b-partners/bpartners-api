package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.validator.RedirectionValidator.verifyRedirectionStatusUrls;
import static app.bpartners.api.endpoint.rest.validator.UUIDValidator.isValid;

@Component
public class PaymentInitValidator implements Consumer<PaymentInitiation> {
  @Override
  public void accept(PaymentInitiation paymentInitiation) {
    StringBuilder builder = new StringBuilder();
    if (paymentInitiation.getId() == null) {
      builder.append("id is mandatory. ");
    }
    if (paymentInitiation.getId() != null && !isValid(paymentInitiation.getId())) {
      builder.append("id must be a valid UUID. ");
    }
    if (paymentInitiation.getAmount() == null) {
      builder.append("amount is mandatory. ");
    }
    if (paymentInitiation.getPayerName() == null) {
      builder.append("payerName is mandatory. ");
    }
    if (paymentInitiation.getPayerEmail() == null) {
      builder.append("payerEmail is mandatory. ");
    }
    verifyRedirectionStatusUrls(builder, paymentInitiation.getRedirectionStatusUrls());
  }
}
