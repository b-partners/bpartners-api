package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.api.service.subscription.StripeCustomerService;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.StripeSubscriptionService;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.PaymentMethodListParams;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StripePaymentMethodServiceTest {
  StripePaymentMethodService subject =
      new StripePaymentMethodService(
          mock(StripeCustomerService.class), mock(StripeSubscriptionService.class));

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
