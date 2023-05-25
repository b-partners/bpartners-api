package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.validator.RedirectionValidator.verifyRedirectionStatusUrls;
import static app.bpartners.api.endpoint.rest.validator.UUIDValidator.isValid;
import static app.bpartners.api.service.utils.EmailUtils.EMAIL_PATTERN;
import static app.bpartners.api.service.utils.EmailUtils.isValidEmail;

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
    if (paymentInitiation.getLabel() == null) {
      builder.append("label is mandatory. ");
    }
    if (paymentInitiation.getLabel() != null
        && paymentInitiation.getLabel().isBlank()
        && paymentInitiation.getLabel().isEmpty()) {
      builder.append("label must not be blank or empty. ");
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
    if (paymentInitiation.getPayerEmail() != null
        && !isValidEmail(paymentInitiation.getPayerEmail())) {
      builder.append(
              "payerEmail(").append(paymentInitiation.getPayerEmail())
          .append(") does not have valid email format. Pattern expected is ")
          .append(EMAIL_PATTERN);
    }
    verifyRedirectionStatusUrls(builder, paymentInitiation.getRedirectionStatusUrls());
  }
}
