package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.StripeDefaultPaymentMethodService;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.StripeSubscriptionService;
import com.stripe.StripeClient;
import com.stripe.model.Charge;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeCollection;
import com.stripe.model.Subscription;
import com.stripe.param.ChargeListParams;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.service.ChargeService;
import com.stripe.service.CustomerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StripeDefaultPaymentMethodServiceTest {
  static final String CUSTOMER_ID = "cus_1";

  StripeClient stripeClientMock = mock(StripeClient.class);
  StripeCustomerService stripeCustomerServiceMock = mock(StripeCustomerService.class);
  StripeSubscriptionService stripeSubscriptionServiceMock = mock(StripeSubscriptionService.class);
  StripePaymentMethodService stripePaymentMethodServiceMock =
      mock(StripePaymentMethodService.class);
  CustomerService customerServiceMock = mock(CustomerService.class);
  ChargeService chargeServiceMock = mock(ChargeService.class);
  StripeDefaultPaymentMethodService subject =
      new StripeDefaultPaymentMethodService(
          stripeClientMock,
          stripeCustomerServiceMock,
          stripeSubscriptionServiceMock,
          stripePaymentMethodServiceMock);

  @BeforeEach
  void setUp() throws Exception {
    lenient().when(stripeClientMock.customers()).thenReturn(customerServiceMock);
    lenient().when(stripeClientMock.charges()).thenReturn(chargeServiceMock);
    lenient()
        .when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId(CUSTOMER_ID))
        .thenReturn(List.of());
    givenSucceededChargesPaymentMethodsAre(List.of());
  }

  @Test
  void does_nothing_when_the_customer_already_has_a_default_payment_method() throws Exception {
    givenCustomerDefaultPaymentMethodIs("pm_already_default");

    var actual = subject.ensureDefaultPaymentMethod(CUSTOMER_ID);

    assertEquals("pm_already_default", actual.orElseThrow());
    verifyNoInteractions(customerServiceMock);
  }

  @Test
  void does_nothing_when_no_payment_method_is_attached() throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    givenAttachedPaymentMethodsAre(List.of());

    var actual = subject.ensureDefaultPaymentMethod(CUSTOMER_ID);

    assertTrue(actual.isEmpty());
    verifyNoInteractions(customerServiceMock);
  }

  @Test
  void elects_the_last_successfully_used_payment_method() throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    givenAttachedPaymentMethodsAre(List.of(card(2050L, "pm_other"), card(2050L, "pm_last_used")));
    givenSucceededChargesPaymentMethodsAre(List.of("pm_last_used", "pm_other"));

    var actual = subject.ensureDefaultPaymentMethod(CUSTOMER_ID);

    assertEquals("pm_last_used", actual.orElseThrow());
    assertEquals("pm_last_used", capturedDefaultPaymentMethod());
  }

  @Test
  void ignores_a_payment_method_used_successfully_but_no_longer_attached() throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    givenAttachedPaymentMethodsAre(List.of(card(2050L, "pm_attached")));
    givenSucceededChargesPaymentMethodsAre(List.of("pm_detached"));

    var actual = subject.ensureDefaultPaymentMethod(CUSTOMER_ID);

    assertEquals("pm_attached", actual.orElseThrow());
    assertEquals("pm_attached", capturedDefaultPaymentMethod());
  }

  @Test
  void falls_back_to_the_subscriptions_default_payment_method() throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    givenAttachedPaymentMethodsAre(
        List.of(card(2050L, "pm_never_used"), card(2050L, "pm_subscription")));
    var subscriptions =
        List.of(
            subscription("canceled", "pm_never_used"), subscription("active", "pm_subscription"));
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId(CUSTOMER_ID))
        .thenReturn(subscriptions);

    var actual = subject.ensureDefaultPaymentMethod(CUSTOMER_ID);

    assertEquals("pm_subscription", actual.orElseThrow());
    assertEquals("pm_subscription", capturedDefaultPaymentMethod());
  }

  @Test
  void falls_back_to_the_first_non_expired_attached_payment_method() throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    givenAttachedPaymentMethodsAre(List.of(card(2020L, "pm_expired"), card(2050L, "pm_valid")));

    var actual = subject.ensureDefaultPaymentMethod(CUSTOMER_ID);

    assertEquals("pm_valid", actual.orElseThrow());
    assertEquals("pm_valid", capturedDefaultPaymentMethod());
  }

  @Test
  void falls_back_to_the_only_attached_payment_method_even_when_expired() throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    givenAttachedPaymentMethodsAre(List.of(card(2020L, "pm_expired")));

    var actual = subject.ensureDefaultPaymentMethod(CUSTOMER_ID);

    assertEquals("pm_expired", actual.orElseThrow());
    assertEquals("pm_expired", capturedDefaultPaymentMethod());
  }

  private Object capturedDefaultPaymentMethod() throws Exception {
    var captor = ArgumentCaptor.forClass(CustomerUpdateParams.class);
    verify(customerServiceMock).update(eq(CUSTOMER_ID), captor.capture());
    return captor.getValue().getInvoiceSettings().getDefaultPaymentMethod();
  }

  private void givenCustomerDefaultPaymentMethodIs(String paymentMethodId) {
    var invoiceSettings = mock(Customer.InvoiceSettings.class);
    when(invoiceSettings.getDefaultPaymentMethod()).thenReturn(paymentMethodId);
    var customer = mock(Customer.class);
    when(customer.getInvoiceSettings()).thenReturn(invoiceSettings);
    when(stripeCustomerServiceMock.getCustomerByStripeCustomerIdentifier(CUSTOMER_ID))
        .thenReturn(customer);
  }

  private void givenAttachedPaymentMethodsAre(List<PaymentMethod> paymentMethods) throws Exception {
    when(stripePaymentMethodServiceMock.getPaymentMethodsAttachedToCustomer(CUSTOMER_ID))
        .thenReturn(paymentMethods);
  }

  @SuppressWarnings("unchecked")
  private void givenSucceededChargesPaymentMethodsAre(List<String> paymentMethodIds)
      throws Exception {
    var succeededCharges = paymentMethodIds.stream().map(this::succeededCharge).toList();
    StripeCollection<Charge> charges = mock(StripeCollection.class);
    when(charges.getData()).thenReturn(succeededCharges);
    lenient().when(chargeServiceMock.list(any(ChargeListParams.class))).thenReturn(charges);
  }

  private Charge succeededCharge(String paymentMethodId) {
    var charge = mock(Charge.class);
    when(charge.getStatus()).thenReturn("succeeded");
    lenient().when(charge.getPaymentMethod()).thenReturn(paymentMethodId);
    return charge;
  }

  private PaymentMethod card(long expYear, String id) {
    var cardDetails = mock(PaymentMethod.Card.class);
    lenient().when(cardDetails.getExpYear()).thenReturn(expYear);
    lenient().when(cardDetails.getExpMonth()).thenReturn(12L);
    var paymentMethod = mock(PaymentMethod.class);
    lenient().when(paymentMethod.getType()).thenReturn("card");
    lenient().when(paymentMethod.getCard()).thenReturn(cardDetails);
    lenient().when(paymentMethod.getId()).thenReturn(id);
    return paymentMethod;
  }

  private Subscription subscription(String status, String defaultPaymentMethodId) {
    var subscription = mock(Subscription.class);
    lenient().when(subscription.getStatus()).thenReturn(status);
    lenient().when(subscription.getDefaultPaymentMethod()).thenReturn(defaultPaymentMethodId);
    return subscription;
  }
}
