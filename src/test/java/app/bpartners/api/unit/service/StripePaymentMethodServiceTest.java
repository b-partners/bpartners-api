package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.StripeSubscriptionService;
import com.stripe.exception.CardException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.Subscription;
import com.stripe.param.PaymentMethodListParams;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StripePaymentMethodServiceTest {
  StripeCustomerService stripeCustomerServiceMock = mock(StripeCustomerService.class);
  StripeSubscriptionService stripeSubscriptionServiceMock = mock(StripeSubscriptionService.class);
  StripePaymentMethodService subject =
      new StripePaymentMethodService(stripeCustomerServiceMock, stripeSubscriptionServiceMock);

  private PaymentMethod card(long expYear, String id) {
    var cardDetails = mock(PaymentMethod.Card.class);
    when(cardDetails.getExpYear()).thenReturn(expYear);
    when(cardDetails.getExpMonth()).thenReturn(12L);
    var paymentMethod = mock(PaymentMethod.class);
    when(paymentMethod.getType()).thenReturn("card");
    when(paymentMethod.getCard()).thenReturn(cardDetails);
    when(paymentMethod.getId()).thenReturn(id);
    return paymentMethod;
  }

  private PaymentMethodCollection cards(List<PaymentMethod> paymentMethods) {
    var collection = mock(PaymentMethodCollection.class);
    when(collection.getData()).thenReturn(paymentMethods);
    return collection;
  }

  private void givenCustomerDefaultPaymentMethodIs(String paymentMethodId) {
    var invoiceSettings = mock(Customer.InvoiceSettings.class);
    when(invoiceSettings.getDefaultPaymentMethod()).thenReturn(paymentMethodId);
    var customer = mock(Customer.class);
    when(customer.getInvoiceSettings()).thenReturn(invoiceSettings);
    when(stripeCustomerServiceMock.getCustomerByStripeCustomerIdentifier("cus_1"))
        .thenReturn(customer);
  }

  private Subscription subscription(String status, String defaultPaymentMethodId) {
    var subscription = mock(Subscription.class);
    when(subscription.getStatus()).thenReturn(status);
    when(subscription.getDefaultPaymentMethod()).thenReturn(defaultPaymentMethodId);
    return subscription;
  }

  @Test
  void get_default_card_payment_method_from_customer_invoice_settings() throws Exception {
    givenCustomerDefaultPaymentMethodIs("pm_default");
    var defaultCard = card(2050L, "pm_default");

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod.when(() -> PaymentMethod.retrieve("pm_default")).thenReturn(defaultCard);

      var actual = subject.getCardPaymentMethods("cus_1", true);

      assertEquals(List.of("pm_default"), actual.stream().map(PaymentMethod::getId).toList());
    }
    verifyNoInteractions(stripeSubscriptionServiceMock);
  }

  @Test
  void get_default_card_payment_method_falls_back_on_non_canceled_subscription() throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    var subscriptions =
        List.of(subscription("canceled", "pm_canceled"), subscription("active", "pm_subscription"));
    var subscriptionCard = card(2050L, "pm_subscription");
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("cus_1"))
        .thenReturn(subscriptions);

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.retrieve("pm_subscription"))
          .thenReturn(subscriptionCard);

      var actual = subject.getCardPaymentMethods("cus_1", true);

      assertEquals(List.of("pm_subscription"), actual.stream().map(PaymentMethod::getId).toList());
    }
  }

  @Test
  void no_default_card_payment_method_when_neither_customer_nor_subscription_has_one()
      throws Exception {
    givenCustomerDefaultPaymentMethodIs(null);
    var subscriptions = List.of(subscription("active", null));
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("cus_1"))
        .thenReturn(subscriptions);

    assertEquals(List.of(), subject.getCardPaymentMethods("cus_1", true));
  }

  @Test
  void get_all_card_payment_methods_when_default_payment_method_not_asked() throws Exception {
    var availableCards = cards(List.of(card(2050L, "pm_1"), card(2020L, "pm_2")));
    var paramsCaptor = ArgumentCaptor.forClass(PaymentMethodListParams.class);

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(availableCards);

      var actual = subject.getCardPaymentMethods("cus_1", false);

      assertEquals(List.of("pm_1", "pm_2"), actual.stream().map(PaymentMethod::getId).toList());
      mockedPaymentMethod.verify(() -> PaymentMethod.list(paramsCaptor.capture()));
    }
    assertEquals("cus_1", paramsCaptor.getValue().getCustomer());
    assertEquals(PaymentMethodListParams.Type.CARD, paramsCaptor.getValue().getType());
    verifyNoInteractions(stripeCustomerServiceMock, stripeSubscriptionServiceMock);
  }

  @Test
  void get_card_payment_methods_ko_when_user_has_no_stripe_customer() {
    assertThrows(BadRequestException.class, () -> subject.getCardPaymentMethods(null, true));
  }

  @Test
  void chargeable_card_skips_expired_cards() throws Exception {
    var availableCards = cards(List.of(card(2020L, "pm_expired"), card(2050L, "pm_valid")));
    var paramsCaptor = ArgumentCaptor.forClass(PaymentMethodListParams.class);

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(availableCards);

      assertEquals("pm_valid", subject.chargeableCard("cus_1").orElseThrow().getId());

      mockedPaymentMethod.verify(() -> PaymentMethod.list(paramsCaptor.capture()));
    }
    assertEquals("cus_1", paramsCaptor.getValue().getCustomer());
    assertEquals(PaymentMethodListParams.Type.CARD, paramsCaptor.getValue().getType());
  }

  @Test
  void no_chargeable_card_when_every_card_is_expired() throws Exception {
    var availableCards = cards(List.of(card(2020L, "pm_expired")));

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(availableCards);

      assertTrue(subject.chargeableCard("cus_1").isEmpty());
    }
  }

  @Test
  void no_chargeable_card_when_the_customer_has_none() throws Exception {
    var noCard = cards(List.of());

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(noCard);

      assertTrue(subject.chargeableCard("cus_1").isEmpty());
    }
  }

  @Test
  void chargeable_card_propagates_stripe_failures() {
    var cardException = mock(CardException.class);

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenThrow(cardException);

      assertThrows(CardException.class, () -> subject.chargeableCard("cus_1"));
    }
  }
}
