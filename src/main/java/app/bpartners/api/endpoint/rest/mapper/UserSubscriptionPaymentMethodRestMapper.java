package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.AMERICAN_EXPRESS;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.CARTES_BANCAIRES;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.DINERS_CLUB;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.DISCOVER;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.EFTPOS_AUSTRALIA;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.INTERAC;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.JCB;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.MASTERCARD;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.OTHER;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.UNIONPAY;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.VISA;
import static app.bpartners.api.endpoint.rest.model.SubscriptionMethodType.CARD;

import app.bpartners.api.endpoint.rest.model.SubscriptionCard;
import app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionPaymentMethod;
import com.stripe.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class UserSubscriptionPaymentMethodRestMapper {

  public UserSubscriptionPaymentMethod toRest(PaymentMethod paymentMethod) {
    return new UserSubscriptionPaymentMethod().type(CARD).card(toRest(paymentMethod.getCard()));
  }

  private SubscriptionCard toRest(PaymentMethod.Card card) {
    if (card == null) {
      return null;
    }
    return new SubscriptionCard()
        .displayBrand(
            displayBrandToRest(
                card.getDisplayBrand() == null ? card.getBrand() : card.getDisplayBrand()))
        .lastFourDigits(card.getLast4())
        .expirationMonth(card.getExpMonth())
        .expirationYear(card.getExpYear());
  }

  private SubscriptionCardDisplayBrand displayBrandToRest(String displayBrand) {
    if (displayBrand == null) {
      return null;
    }
    return switch (displayBrand.toLowerCase()) {
      case "american_express", "amex" -> AMERICAN_EXPRESS;
      case "cartes_bancaires" -> CARTES_BANCAIRES;
      case "diners_club", "diners" -> DINERS_CLUB;
      case "discover" -> DISCOVER;
      case "eftpos_australia", "eftpos_au" -> EFTPOS_AUSTRALIA;
      case "interac" -> INTERAC;
      case "jcb" -> JCB;
      case "mastercard" -> MASTERCARD;
      case "unionpay" -> UNIONPAY;
      case "visa" -> VISA;
      default -> OTHER;
    };
  }
}
