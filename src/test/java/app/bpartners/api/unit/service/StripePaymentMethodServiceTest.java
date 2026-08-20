package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.StripeSubscriptionService;
import com.stripe.StripeClient;
import com.stripe.exception.CardException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.model.SetupIntent;
import com.stripe.model.Subscription;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.service.CustomerService;
import com.stripe.service.PaymentMethodService;
import com.stripe.service.SetupIntentService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StripePaymentMethodServiceTest {
  StripeCustomerService stripeCustomerServiceMock = mock(StripeCustomerService.class);
  StripeSubscriptionService stripeSubscriptionServiceMock = mock(StripeSubscriptionService.class);
  StripeClient stripeClientMock = mock(StripeClient.class);
  SetupIntentService setupIntentServiceMock = mock(SetupIntentService.class);
  CustomerService customerServiceMock = mock(CustomerService.class);
  PaymentMethodService paymentMethodServiceMock = mock(PaymentMethodService.class);
  com.stripe.service.SubscriptionService stripeSubscriptionApiMock =
      mock(com.stripe.service.SubscriptionService.class);
  StripePaymentMethodService subject =
      new StripePaymentMethodService(
          stripeCustomerServiceMock, stripeSubscriptionServiceMock, stripeClientMock);

  private void givenSetupIntentPaymentMethodIs(String setupIntentId, String paymentMethodId)
      throws Exception {
    var setupIntent = mock(SetupIntent.class);
    when(setupIntent.getPaymentMethod()).thenReturn(paymentMethodId);
    when(stripeClientMock.setupIntents()).thenReturn(setupIntentServiceMock);
    when(setupIntentServiceMock.retrieve(setupIntentId)).thenReturn(setupIntent);
  }

  @Test
  void replacement_sets_the_new_card_as_default_then_detaches_the_previous_ones() throws Exception {
    givenSetupIntentPaymentMethodIs("seti_1", "pm_new");
    var attachedCards = cards(List.of(card(2050L, "pm_new"), card(2050L, "pm_old")));
    when(stripeClientMock.customers()).thenReturn(customerServiceMock);
    when(stripeClientMock.paymentMethods()).thenReturn(paymentMethodServiceMock);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("cus_1"))
        .thenReturn(List.of());
    var customerUpdateCaptor = ArgumentCaptor.forClass(CustomerUpdateParams.class);

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(attachedCards);

      subject.replaceCardPaymentMethodsFromSetupIntent("cus_1", "seti_1");
    }

    var inOrder = inOrder(customerServiceMock, paymentMethodServiceMock);
    inOrder.verify(customerServiceMock).update(eq("cus_1"), customerUpdateCaptor.capture());
    inOrder.verify(paymentMethodServiceMock).detach("pm_old");
    verify(paymentMethodServiceMock, never()).detach("pm_new");
    assertEquals(
        "pm_new", customerUpdateCaptor.getValue().getInvoiceSettings().getDefaultPaymentMethod());
  }

  @Test
  void replacement_detaches_every_other_card_not_only_the_previous_default() throws Exception {
    givenSetupIntentPaymentMethodIs("seti_1", "pm_new");
    var attachedCards =
        cards(
            List.of(
                card(2050L, "pm_new"),
                card(2050L, "pm_old_1"),
                card(2050L, "pm_old_2"),
                card(2020L, "pm_expired")));
    when(stripeClientMock.customers()).thenReturn(customerServiceMock);
    when(stripeClientMock.paymentMethods()).thenReturn(paymentMethodServiceMock);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("cus_1"))
        .thenReturn(List.of());

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(attachedCards);

      subject.replaceCardPaymentMethodsFromSetupIntent("cus_1", "seti_1");
    }

    verify(paymentMethodServiceMock).detach("pm_old_1");
    verify(paymentMethodServiceMock).detach("pm_old_2");
    verify(paymentMethodServiceMock).detach("pm_expired");
    verify(paymentMethodServiceMock, never()).detach("pm_new");
  }

  @Test
  void replacement_sets_the_new_card_as_default_of_every_non_canceled_subscription()
      throws Exception {
    givenSetupIntentPaymentMethodIs("seti_1", "pm_new");
    var attachedCards = cards(List.of(card(2050L, "pm_new")));
    var subscriptions =
        List.of(
            subscription("sub_active", "active", "pm_old"),
            subscription("sub_trialing", "trialing", null),
            subscription("sub_canceled", "canceled", "pm_old"));
    when(stripeClientMock.customers()).thenReturn(customerServiceMock);
    when(stripeClientMock.paymentMethods()).thenReturn(paymentMethodServiceMock);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionApiMock);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("cus_1"))
        .thenReturn(subscriptions);
    var subscriptionUpdateCaptor = ArgumentCaptor.forClass(SubscriptionUpdateParams.class);

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(attachedCards);

      subject.replaceCardPaymentMethodsFromSetupIntent("cus_1", "seti_1");
    }

    var subscriptionIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(stripeSubscriptionApiMock, times(2))
        .update(subscriptionIdCaptor.capture(), subscriptionUpdateCaptor.capture());
    assertEquals(List.of("sub_active", "sub_trialing"), subscriptionIdCaptor.getAllValues());
    assertEquals(
        List.of("pm_new", "pm_new"),
        subscriptionUpdateCaptor.getAllValues().stream()
            .map(SubscriptionUpdateParams::getDefaultPaymentMethod)
            .toList());
  }

  @Test
  void replacement_sets_the_subscriptions_default_before_detaching_the_previous_cards()
      throws Exception {
    givenSetupIntentPaymentMethodIs("seti_1", "pm_new");
    var attachedCards = cards(List.of(card(2050L, "pm_new"), card(2050L, "pm_old")));
    var subscriptions = List.of(subscription("sub_active", "active", "pm_old"));
    when(stripeClientMock.customers()).thenReturn(customerServiceMock);
    when(stripeClientMock.paymentMethods()).thenReturn(paymentMethodServiceMock);
    when(stripeClientMock.subscriptions()).thenReturn(stripeSubscriptionApiMock);
    when(stripeSubscriptionServiceMock.getStripeSubscriptionsFromStripeCustomerId("cus_1"))
        .thenReturn(subscriptions);

    try (var mockedPaymentMethod = mockStatic(PaymentMethod.class)) {
      mockedPaymentMethod
          .when(() -> PaymentMethod.list(any(PaymentMethodListParams.class)))
          .thenReturn(attachedCards);

      subject.replaceCardPaymentMethodsFromSetupIntent("cus_1", "seti_1");
    }

    var inOrder = inOrder(customerServiceMock, stripeSubscriptionApiMock, paymentMethodServiceMock);
    inOrder.verify(customerServiceMock).update(eq("cus_1"), any(CustomerUpdateParams.class));
    inOrder
        .verify(stripeSubscriptionApiMock)
        .update(eq("sub_active"), any(SubscriptionUpdateParams.class));
    inOrder.verify(paymentMethodServiceMock).detach("pm_old");
  }

  @Test
  void replacement_is_skipped_when_the_setup_intent_carries_no_payment_method() throws Exception {
    givenSetupIntentPaymentMethodIs("seti_1", null);

    subject.replaceCardPaymentMethodsFromSetupIntent("cus_1", "seti_1");

    verifyNoInteractions(customerServiceMock, paymentMethodServiceMock, stripeSubscriptionApiMock);
  }

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

  private Subscription subscription(String id, String status, String defaultPaymentMethodId) {
    var subscription = mock(Subscription.class);
    lenient().when(subscription.getId()).thenReturn(id);
    lenient().when(subscription.getStatus()).thenReturn(status);
    lenient().when(subscription.getDefaultPaymentMethod()).thenReturn(defaultPaymentMethodId);
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
