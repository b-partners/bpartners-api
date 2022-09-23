package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class PaymentReqValidator implements Consumer<PaymentInitiation> {
  @Override
  public void accept(PaymentInitiation paymentInitiation) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (paymentInitiation.getId() == null) {
      exceptionMessageBuilder.append("id is mandatory. ");
    }
    if (paymentInitiation.getLabel() == null) {
      exceptionMessageBuilder.append("label is mandatory. ");
    }
    if (paymentInitiation.getReference() == null) {
      exceptionMessageBuilder.append("reference is mandatory. ");
    }
    if (paymentInitiation.getAmount() == null) {
      exceptionMessageBuilder.append("amount is mandatory. ");
    }
    if (paymentInitiation.getPayerName() == null) {
      exceptionMessageBuilder.append("payerName is mandatory. ");
    }
    if (paymentInitiation.getPayerEmail() == null) {
      exceptionMessageBuilder.append("payerEmail is mandatory. ");
    }

    OnboardingInitiationValidator.verifyRedirectionStatusUrls(exceptionMessageBuilder,
        paymentInitiation.getRedirectionStatusUrls());
  }
}
