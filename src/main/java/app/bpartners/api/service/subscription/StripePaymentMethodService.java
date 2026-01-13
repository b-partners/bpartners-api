package app.bpartners.api.service.subscription;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodListParams;
import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentMethodService {

  @SneakyThrows
  public List<PaymentMethod> getPaymentMethod(String stripeCustomerIdentifier) {
    List<PaymentMethod> paymentMethods = new ArrayList<>();
    for (PaymentMethodListParams.Type type :
        List.of(
            PaymentMethodListParams.Type.CARD,
            // PaymentMethodListParams.Type.PAYPAL, TODO: uncomment if necessary
            PaymentMethodListParams.Type.SEPA_DEBIT)) {
      paymentMethods.addAll(getPaymentMethods(stripeCustomerIdentifier, type));
    }
    return paymentMethods;
  }

  private List<PaymentMethod> getPaymentMethods(
      String stripeCustomerIdentifier, PaymentMethodListParams.Type type) throws StripeException {
    PaymentMethodListParams params =
        PaymentMethodListParams.builder()
            .setCustomer(stripeCustomerIdentifier)
            .setType(type)
            .build();
    return PaymentMethod.list(params).getData();
  }
}
