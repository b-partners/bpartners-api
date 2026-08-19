package app.bpartners.api.unit.service;

import static app.bpartners.api.model.credit.CreditPurchaseCharge.NO_CHARGEABLE_CARD;
import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.service.subscription.StripeCreditPurchaseService;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class StripeCreditPurchaseServiceTest {
  StripePaymentMethodService stripePaymentMethodService = mock(StripePaymentMethodService.class);
  StripeCreditPurchaseService subject = new StripeCreditPurchaseService(stripePaymentMethodService);

  private CreditPurchase purchase() {
    return CreditPurchase.builder()
        .id("purchase_1")
        .userId("user_id")
        .type(CUSTOM)
        .credits(7L)
        .amountInCentsWithVat(8400L)
        .build();
  }

  private PaymentMethod card(String id) {
    var paymentMethod = mock(PaymentMethod.class);
    when(paymentMethod.getId()).thenReturn(id);
    return paymentMethod;
  }

  @Test
  void charge_off_session_charges_the_chargeable_card() throws Exception {
    var paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getStatus()).thenReturn("succeeded");
    when(paymentIntent.getId()).thenReturn("pi_1");
    var paramsCaptor = ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
    var chargeableCard = card("pm_valid");
    when(stripePaymentMethodService.chargeableCard("cus_1"))
        .thenReturn(Optional.of(chargeableCard));

    try (var mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
      mockedPaymentIntent
          .when(
              () ->
                  PaymentIntent.create(
                      any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(paymentIntent);

      var actual = subject.chargeOffSession("cus_1", purchase());

      assertTrue(actual.succeeded());
      assertEquals("pi_1", actual.paymentIntentId());
      mockedPaymentIntent.verify(
          () -> PaymentIntent.create(paramsCaptor.capture(), any(RequestOptions.class)));
    }
    var params = paramsCaptor.getValue();
    assertEquals("cus_1", params.getCustomer());
    assertEquals("pm_valid", params.getPaymentMethod());
    assertEquals(8400L, params.getAmount());
    assertEquals(true, params.getConfirm());
    assertEquals(true, params.getOffSession());
    assertEquals(
        "purchase_1", ((java.util.Map<?, ?>) params.getMetadata()).get("credit_purchase_id"));
  }

  @Test
  void charge_off_session_without_any_chargeable_card_does_not_reach_stripe() throws Exception {
    when(stripePaymentMethodService.chargeableCard("cus_1")).thenReturn(Optional.empty());

    try (var mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
      var actual = subject.chargeOffSession("cus_1", purchase());

      assertFalse(actual.succeeded());
      assertEquals(NO_CHARGEABLE_CARD, actual.failureCode());
      mockedPaymentIntent.verifyNoInteractions();
    }
  }

  @Test
  void charge_off_session_maps_a_card_error_to_its_code() throws Exception {
    var cardException = mock(CardException.class);
    when(cardException.getCode()).thenReturn("authentication_required");
    var chargeableCard = card("pm_valid");
    when(stripePaymentMethodService.chargeableCard("cus_1"))
        .thenReturn(Optional.of(chargeableCard));

    try (var mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
      mockedPaymentIntent
          .when(
              () ->
                  PaymentIntent.create(
                      any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenThrow(cardException);

      var actual = subject.chargeOffSession("cus_1", purchase());

      assertFalse(actual.succeeded());
      assertEquals("authentication_required", actual.failureCode());
    }
  }

  @Test
  void charge_off_session_maps_a_non_succeeded_intent_to_its_status() throws Exception {
    var paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getStatus()).thenReturn("requires_action");
    var chargeableCard = card("pm_valid");
    when(stripePaymentMethodService.chargeableCard("cus_1"))
        .thenReturn(Optional.of(chargeableCard));

    try (var mockedPaymentIntent = mockStatic(PaymentIntent.class)) {
      mockedPaymentIntent
          .when(
              () ->
                  PaymentIntent.create(
                      any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
          .thenReturn(paymentIntent);

      var actual = subject.chargeOffSession("cus_1", purchase());

      assertFalse(actual.succeeded());
      assertEquals("requires_action", actual.failureCode());
    }
  }

  @Test
  void charge_off_session_gives_up_when_cards_can_not_be_listed() throws Exception {
    var cardException = mock(CardException.class);
    when(cardException.getCode()).thenReturn("api_error");
    when(stripePaymentMethodService.chargeableCard("cus_1")).thenThrow(cardException);

    var actual = subject.chargeOffSession("cus_1", purchase());

    assertFalse(actual.succeeded());
    assertEquals("api_error", actual.failureCode());
  }

  @Test
  void checkout_session_carries_the_amount_the_urls_and_the_purchase_metadata() {
    var session = mock(Session.class);
    when(session.getUrl()).thenReturn("https://pay.stripe.com/session");
    var paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);

    try (var mockedSession = mockStatic(Session.class)) {
      mockedSession.when(() -> Session.create(any(SessionCreateParams.class))).thenReturn(session);

      var actual =
          subject.checkoutSessionUrl(
              "cus_1",
              purchase().toBuilder()
                  .creditPack(CreditPack.builder().description("10 analyses de toiture").build())
                  .build(),
              "https://birdia.fr/success",
              "https://birdia.fr/failure");

      assertEquals("https://pay.stripe.com/session", actual);
      mockedSession.verify(() -> Session.create(paramsCaptor.capture()));
    }
    var params = paramsCaptor.getValue();
    assertEquals("cus_1", params.getCustomer());
    assertEquals("purchase_1", params.getClientReferenceId());
    assertEquals("https://birdia.fr/success", params.getSuccessUrl());
    assertEquals("https://birdia.fr/failure", params.getCancelUrl());
    assertEquals(8400L, params.getLineItems().getFirst().getPriceData().getUnitAmount());
    assertEquals(
        "10 analyses de toiture",
        params.getLineItems().getFirst().getPriceData().getProductData().getName());
  }
}
