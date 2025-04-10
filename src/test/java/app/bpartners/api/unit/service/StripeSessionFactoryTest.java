package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.subscription.StripeSessionFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static java.time.Month.APRIL;
import static java.time.Month.MAY;
import static org.mockito.Mockito.mock;

class StripeSessionFactoryTest {
  TemporalUtils temporalUtilsMock = mock(TemporalUtils.class);
  StripeSessionFactory subject = new StripeSessionFactory(temporalUtilsMock);

  @Test
  void create_session_if_trial_end_is_before_next_billing_date() {
    var trialEnd = LocalDate.of(2025, APRIL, 30);
    var stripeCustomer = mock(Customer.class);
    var subscriptionProduct = SubscriptionProduct.builder().build();
    var price = mock(Price.class);
    var rediredionUrls = mock(RedirectionStatusUrls.class);
    var billingCycleAnchor = LocalDate.of(2025, MAY, 5);
    var actual = subject.createSession(
            trialEnd,
            stripeCustomer,
            subscriptionProduct,
            price,
            rediredionUrls,
            billingCycleAnchor,
            subscription);
  }
}