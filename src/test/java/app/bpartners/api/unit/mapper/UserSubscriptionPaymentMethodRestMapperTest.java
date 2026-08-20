package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.AMERICAN_EXPRESS;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.CARTES_BANCAIRES;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.OTHER;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.VISA;
import static app.bpartners.api.endpoint.rest.model.SubscriptionMethodType.CARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.UserSubscriptionPaymentMethodRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionCard;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionPaymentMethod;
import com.stripe.model.PaymentMethod;
import org.junit.jupiter.api.Test;

class UserSubscriptionPaymentMethodRestMapperTest {
  UserSubscriptionPaymentMethodRestMapper subject = new UserSubscriptionPaymentMethodRestMapper();

  private PaymentMethod paymentMethod(PaymentMethod.Card card) {
    var paymentMethod = mock(PaymentMethod.class);
    when(paymentMethod.getCard()).thenReturn(card);
    return paymentMethod;
  }

  private PaymentMethod.Card card(String displayBrand, String brand) {
    var card = mock(PaymentMethod.Card.class);
    when(card.getDisplayBrand()).thenReturn(displayBrand);
    when(card.getBrand()).thenReturn(brand);
    when(card.getLast4()).thenReturn("4242");
    when(card.getExpMonth()).thenReturn(3L);
    when(card.getExpYear()).thenReturn(2030L);
    return card;
  }

  @Test
  void map_card_payment_method() {
    var actual = subject.toRest(paymentMethod(card("visa", "visa")));

    assertEquals(
        new UserSubscriptionPaymentMethod()
            .type(CARD)
            .card(
                new SubscriptionCard()
                    .displayBrand(VISA)
                    .lastFourDigits("4242")
                    .expirationMonth(3L)
                    .expirationYear(2030L)),
        actual);
  }

  @Test
  void map_display_brand_from_brand_when_display_brand_is_missing() {
    var actual = subject.toRest(paymentMethod(card(null, "amex")));

    assertEquals(AMERICAN_EXPRESS, actual.getCard().getDisplayBrand());
  }

  @Test
  void map_snake_cased_display_brand() {
    var actual = subject.toRest(paymentMethod(card("cartes_bancaires", null)));

    assertEquals(CARTES_BANCAIRES, actual.getCard().getDisplayBrand());
  }

  @Test
  void map_unknown_display_brand_to_other() {
    var actual = subject.toRest(paymentMethod(card("some_unsupported_brand", null)));

    assertEquals(OTHER, actual.getCard().getDisplayBrand());
  }

  @Test
  void map_payment_method_without_card_details() {
    var actual = subject.toRest(paymentMethod(null));

    assertEquals(CARD, actual.getType());
    assertNull(actual.getCard());
  }
}
