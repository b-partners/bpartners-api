package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import org.springframework.stereotype.Component;

@Component
public class PaymentReqValidator {
  public void accept(PaymentInitiation paymentInitiation) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (paymentInitiation.getId() == null) {
      exceptionMessageBuilder.append("id is missing. ");
    }
    if (paymentInitiation.getLabel() == null) {
      exceptionMessageBuilder.append("label is missing. ");
    }
    if (paymentInitiation.getReference() == null) {
      exceptionMessageBuilder.append("reference is missing. ");
    }
    if (paymentInitiation.getAmount() == null) {
      exceptionMessageBuilder.append("amount is missing. ");
    }
    if (paymentInitiation.getPayerName() == null) {
      exceptionMessageBuilder.append("payerName is missing. ");
    }
    if (paymentInitiation.getPayerEmail() == null) {
      exceptionMessageBuilder.append("payerEmail is missing. ");
    }

    OnboardingInitiationValidator.verifyRedirectionStatusUrls(exceptionMessageBuilder,
        paymentInitiation.getRedirectionStatusUrls());
  }
}
