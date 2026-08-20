package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.CARTES_BANCAIRES;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.MASTERCARD;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.VISA;
import static app.bpartners.api.endpoint.rest.model.SubscriptionMethodType.CARD;
import static app.bpartners.api.integration.conf.utils.TestUtils.BERNARD_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.UserSubscriptionApi;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.SubscriptionCard;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionPaymentMethod;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.StripeSetupService;
import com.stripe.model.PaymentMethod;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

class UserPaymentMethodIT extends MockedThirdParties {
  private static final String JOE_DOE_STRIPE_CUSTOMER_ID = "cus_REyMbSpHZjHftA";

  @MockBean private StripeSetupService stripeSetupServiceMock;

  private UserSubscriptionApi joeUserSubscriptionApi() {
    return new UserSubscriptionApi(TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort));
  }

  private static PaymentMethod stripeCard(String displayBrand, String last4) {
    var card = new PaymentMethod.Card();
    card.setDisplayBrand(displayBrand);
    card.setLast4(last4);
    card.setExpMonth(9L);
    card.setExpYear(2031L);
    var paymentMethod = new PaymentMethod();
    paymentMethod.setType("card");
    paymentMethod.setCard(card);
    return paymentMethod;
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpUserSubscription(subscriptionService);
  }

  @SneakyThrows
  @Test
  void get_default_payment_method_ok() {
    when(stripePaymentMethodServiceMock.getCardPaymentMethods(JOE_DOE_STRIPE_CUSTOMER_ID, true))
        .thenReturn(List.of(stripeCard("visa", "4242")));

    var actual = joeUserSubscriptionApi().getUserPaymentMethods(JOE_DOE_ID, true);

    assertEquals(
        List.of(
            new UserSubscriptionPaymentMethod()
                .type(CARD)
                .card(
                    new SubscriptionCard()
                        .displayBrand(VISA)
                        .lastFourDigits("4242")
                        .expirationMonth(9L)
                        .expirationYear(2031L))),
        actual);
  }

  @SneakyThrows
  @Test
  void get_all_payment_methods_ok() {
    when(stripePaymentMethodServiceMock.getCardPaymentMethods(JOE_DOE_STRIPE_CUSTOMER_ID, false))
        .thenReturn(
            List.of(stripeCard("cartes_bancaires", "0042"), stripeCard("mastercard", "1111")));

    var actual = joeUserSubscriptionApi().getUserPaymentMethods(JOE_DOE_ID, false);

    assertEquals(
        List.of(CARTES_BANCAIRES, MASTERCARD),
        actual.stream().map(pm -> pm.getCard().getDisplayBrand()).toList());
    assertEquals(
        List.of("0042", "1111"),
        actual.stream().map(pm -> pm.getCard().getLastFourDigits()).toList());
  }

  @SneakyThrows
  @Test
  void get_payment_methods_ok_when_customer_has_none() {
    when(stripePaymentMethodServiceMock.getCardPaymentMethods(JOE_DOE_STRIPE_CUSTOMER_ID, true))
        .thenReturn(List.of());

    var actual = joeUserSubscriptionApi().getUserPaymentMethods(JOE_DOE_ID, true);

    assertEquals(List.of(), actual);
  }

  @SneakyThrows
  @Test
  void get_payment_methods_ko_when_user_has_no_stripe_customer() {
    when(stripePaymentMethodServiceMock.getCardPaymentMethods(JOE_DOE_STRIPE_CUSTOMER_ID, true))
        .thenThrow(
            new BadRequestException(
                "Unable to retrieve payment methods as user is not associated to a stripe customer"
                    + " yet"));

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Unable to retrieve payment methods as user is"
            + " not associated to a stripe customer yet\"}",
        () -> joeUserSubscriptionApi().getUserPaymentMethods(JOE_DOE_ID, true));
  }

  @SneakyThrows
  @Test
  void initiate_payment_method_replacement_ok() {
    var redirectionStatusUrls =
        new RedirectionStatusUrls()
            .successUrl("http://localhost/success")
            .failureUrl("http://localhost/failure");
    when(stripeSetupServiceMock.setupReplacementCheckoutSession(
            JOE_DOE_STRIPE_CUSTOMER_ID, redirectionStatusUrls))
        .thenReturn(
            new Redirection()
                .redirectionUrl("https://checkout.stripe.com/c/pay/seti_1")
                .redirectionStatusUrls(redirectionStatusUrls));

    var actual =
        joeUserSubscriptionApi()
            .initiatePaymentMethodReplacement(JOE_DOE_ID, redirectionStatusUrls);

    assertEquals("https://checkout.stripe.com/c/pay/seti_1", actual.getRedirectionUrl());
  }

  @Test
  void initiate_other_user_payment_method_replacement_ko() {
    assertThrowsForbiddenException(
        () ->
            joeUserSubscriptionApi()
                .initiatePaymentMethodReplacement(
                    BERNARD_DOE_ID, new RedirectionStatusUrls().successUrl("http://localhost")));
  }

  @Test
  void get_other_user_payment_methods_ko() {
    assertThrowsForbiddenException(
        () -> joeUserSubscriptionApi().getUserPaymentMethods(BERNARD_DOE_ID, true));
  }
}
