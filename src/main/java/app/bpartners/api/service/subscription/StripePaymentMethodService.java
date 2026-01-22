package app.bpartners.api.service.subscription;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.param.PaymentMethodListParams;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripePaymentMethodService {
  private final StripeCustomerService stripeCustomerService;

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

  public boolean customerHasValidPaymentMethods(
      String stripeCustomerIdentifier, List<PaymentMethod> paymentMethodList) {
    if (paymentMethodList == null || paymentMethodList.isEmpty()) {
      return false;
    }
    var stripeCustomer =
        stripeCustomerService.getCustomerByStripeCustomerIdentifier(stripeCustomerIdentifier);
    var defaultPaymentMethodId = stripeCustomer.getInvoiceSettings().getDefaultPaymentMethod();
    if (defaultPaymentMethodId == null) {
      return false;
    }
    return paymentMethodList.stream()
        .filter(pm -> defaultPaymentMethodId.equals(pm.getId()))
        .anyMatch(this::isPaymentMethodValid);
  }

  private boolean isPaymentMethodValid(PaymentMethod pm) {
    if ("card".equalsIgnoreCase(pm.getType())) {
      return pm.getCard() != null && isNotExpired(pm.getCard());
    }
    return true;
  }

  private boolean isNotExpired(PaymentMethod.Card card) {
    YearMonth expiration =
        YearMonth.of(card.getExpYear().intValue(), card.getExpMonth().intValue());
    return !expiration.isBefore(YearMonth.now());
  }
}
