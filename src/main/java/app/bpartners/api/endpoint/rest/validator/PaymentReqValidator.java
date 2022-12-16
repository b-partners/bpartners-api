package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class PaymentReqValidator implements Consumer<PaymentInitiation> {
  @Override
  public void accept(PaymentInitiation paymentInitiation) {
    StringBuilder builder = new StringBuilder();
    if (paymentInitiation.getId() == null) {
      builder.append("id is mandatory. ");
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

    //TODO: put this verification method into the appropriate utils
    OnboardingInitiationValidator
        .verifyRedirectionStatusUrls(builder, paymentInitiation.getRedirectionStatusUrls());
  }
}
