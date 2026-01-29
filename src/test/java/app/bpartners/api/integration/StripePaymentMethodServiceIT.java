package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.integration.conf.StripeMockedThirdParties;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import com.stripe.model.PaymentMethod;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Disabled("TODO: local use only")
@Slf4j
class StripePaymentMethodServiceIT extends StripeMockedThirdParties {
  @Autowired StripePaymentMethodService subject;

  @SneakyThrows
  @Test
  void get_payment_method_from_both_customer_and_subscription() {
    var actual = subject.getPaymentMethod(System.getenv("CUSTOMER_ID"));

    assertNotNull(actual);
    assertFalse(actual.isEmpty());
    log.info(
        "Payment method card retrieved from both customer and subscription ({} size): {}",
        actual.size(),
        actual.stream().map(PaymentMethod::getCard).toList());
  }
}
