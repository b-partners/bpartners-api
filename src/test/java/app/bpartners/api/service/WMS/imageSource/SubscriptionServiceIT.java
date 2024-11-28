package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.integration.conf.StripeMockedThirdParties;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class SubscriptionServiceIT extends StripeMockedThirdParties {
  @Autowired SubscriptionService subject;
  @Autowired UserRepository userRepository;
  @MockBean UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock;

  @Test
  void create_list_delete_customers() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();

    var createdUserSubscription =
        subject.createUserSubscription(user.toBuilder().mobilePhoneNumber("0622334455").build());
    var updatedUser = createdUserSubscription.getUser();
    var updatedUserSubscription = subject.updateUserSubscription(updatedUser);

    assertNotNull(updatedUserSubscription);
    assertEquals(
        subject.getSubscriptionByUserSubscriptionId(
            updatedUserSubscription.getUser().getUserSubscriptionId()),
        updatedUserSubscription);
    assertNotNull(subject.cancelUserSubscription(updatedUser));
    assertNull(userRepository.getById(user.getId()).getUserSubscriptionId());
  }

  @Test
  void cancel_user_subscription_ko() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();

    var actual =
        assertThrows(IllegalArgumentException.class, () -> subject.cancelUserSubscription(user));

    assertEquals(
        "User.userSubscriptionId is required to cancel subscription, "
            + "otherwise User.id="
            + user.getId()
            + " does not have userSubscriptionId",
        actual.getMessage());
  }

  @Test
  void update_user_subscription_ko() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();

    var actual =
        assertThrows(IllegalArgumentException.class, () -> subject.updateUserSubscription(user));

    assertEquals(
        "User.userSubscriptionId is required to update subscription, "
            + "otherwise User.id="
            + user.getId()
            + " does not have userSubscriptionId",
        actual.getMessage());
  }

  @Test
  void user_has_free_trial_subscription_period() {
    var user = userRepository.findByEmail("bernard@email.com").orElseThrow();

    var actualSubscriptions =
        subject
            .getSubscriptionByUserSubscriptionId(user.getUserSubscriptionId())
            .getSubscriptions();

    assertFalse(actualSubscriptions.isEmpty());
    var subscription = actualSubscriptions.getFirst();
    assertTrue(subscription.hasFreeTrialPeriod());
    assertEquals(14L, subscription.getFreeTrialDays());
    assertEquals(
        LocalDate.of(2024, 11, 19),
        subscription.getFreeTrialStart().atZone(ZoneId.of("Europe/Paris")).toLocalDate());
    assertEquals(
        LocalDate.of(2024, 12, 3),
        subscription.getFreeTrialEnd().atZone(ZoneId.of("Europe/Paris")).toLocalDate());
    assertEquals(
        LocalDate.of(2024, 11, 19),
        subscription.getStartDatetime().atZone(ZoneId.of("Europe/Paris")).toLocalDate());
    assertEquals(
        LocalDate.of(2024, 12, 3),
        subscription.getEndDatetime().atZone(ZoneId.of("Europe/Paris")).toLocalDate());
  }

  @Test
  void initiate_subscription() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(new UserSubscriptionEligible()));
    var existingUser = userRepository.findByEmail("joe@email.com").orElseThrow();
    var createdUserSubscription =
        subject.createUserSubscription(
            existingUser.toBuilder().email("joe" + new Random().nextInt() + "@email.com").build());
    var user = createdUserSubscription.getUser();

    var actualSubscriptionRedirection =
        subject.initiateSubscription(
            user, getDefaultSubscription(), getDefaultRedirectionStatusUrls());

    assertNotNull(actualSubscriptionRedirection);
    assertNotNull(actualSubscriptionRedirection.getRedirectionUrl());
    assertTrue(
        actualSubscriptionRedirection
            .getRedirectionUrl()
            .contains("https://checkout.stripe.com/c/pay"));
    assertNotNull(actualSubscriptionRedirection.getRedirectionStatusUrls());
    assertNotNull(actualSubscriptionRedirection.getRedirectionStatusUrls().getSuccessUrl());
    assertNotNull(actualSubscriptionRedirection.getRedirectionStatusUrls().getFailureUrl());
    log.info(
        "Redirection stripe checkout url = {}", actualSubscriptionRedirection.getRedirectionUrl());
    assertNotNull(subject.cancelUserSubscription(user));
  }

  private RedirectionStatusUrls getDefaultRedirectionStatusUrls() {
    return new RedirectionStatusUrls()
        .failureUrl("http://loclahost/cancelUrl")
        .successUrl("http://loclahost/successUrl");
  }

  private Subscription getDefaultSubscription() {
    return Subscription.builder()
        .subscriptionProduct(
            subject.getSubscriptionProductByE2Id(
                randomUUID().toString(), defaultSubscriptionProductId()))
        .endDatetime(now().plus(30L, DAYS))
        .freeTrialDays(14L)
        .build();
  }

  @Test
  void initiate_subscription_without_stripe_customer_associated_ko() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();
    var defaultSubscription = getDefaultSubscription();
    var defaultRedirectionStatusUrls = getDefaultRedirectionStatusUrls();

    var actual =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.initiateSubscription(
                    user, defaultSubscription, defaultRedirectionStatusUrls));

    assertEquals(
        "User.id=" + user.getId() + " is not associated to a stripe customer yet",
        actual.getMessage());
  }

  @Test
  void initiate_subscription_with_active_subscription_ko() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(new UserSubscriptionEligible()));
    var user = userRepository.findByEmail("joe@email.com").orElseThrow();
    var defaultSubscription = getDefaultSubscription();
    var defaultRedirectionStatusUrls = getDefaultRedirectionStatusUrls();
    var actualSubscription = subject.getSubscriptionByUser(user);

    if (actualSubscription.getLatestSubscription().getEndDatetime().isAfter(now())) {
      var actual =
          assertThrows(
              BadRequestException.class,
              () ->
                  subject.initiateSubscription(
                      user, defaultSubscription, defaultRedirectionStatusUrls));
      assertEquals(
          "User.id="
              + user.getId()
              + " has active subscription until "
              + actualSubscription.getLatestSubscription().getEndDatetime(),
          actual.getMessage());
    }
    assertTrue(true); // jut skip test if subscription not active anymore
  }

  @Test
  @Disabled("TODO: remove new product in stripe dashboard after running")
  void add_product_and_initiate_subscriptions() {
    var actualSubscriptionProduct =
        subject.createSubscriptionProduct(
            SubscriptionProduct.builder()
                .name("L'Abonnement Essentiel")
                .description(
                    "Sans engagement - Idéal pour les artisans couvreurs. "
                        + String.join(" ", subscriptionProductFeatures()))
                .features(subscriptionProductFeatures())
                .priceInCents(4900L)
                .type(MONTHLY)
                .build());

    var expectedSubscriptionProduct =
        SubscriptionProduct.builder()
            .id(actualSubscriptionProduct.getId())
            .e2Id(actualSubscriptionProduct.getE2Id())
            .name("L'Abonnement Essentiel")
            .description(
                "Sans engagement - Idéal pour les artisans couvreurs. "
                    + String.join(" ", subscriptionProductFeatures()))
            .features(subscriptionProductFeatures())
            .type(MONTHLY)
            .priceInCents(4900L)
            .creationDatetime(actualSubscriptionProduct.getCreationDatetime())
            .build();
    assertEquals(expectedSubscriptionProduct, actualSubscriptionProduct);
    assertNotNull(actualSubscriptionProduct.getId());
    assertNotNull(actualSubscriptionProduct.getE2Id());
  }

  // TODO: update trial period so it always be in trial mode
  @Test
  void user_has_active_subscription() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(new UserSubscriptionEligible()));
    var userBernard = userRepository.findByEmail("bernard@email.com").orElseThrow();
    var userJoe = userRepository.findByEmail("joe@email.com").orElseThrow();

    var actualTrialingUserSubscription = subject.getSubscriptionByUserId(userBernard.getId());
    var actualUserSubscription = subject.getSubscriptionByUserId(userJoe.getId());

    if (actualTrialingUserSubscription
        .getSubscriptions()
        .getFirst()
        .getFreeTrialEnd()
        .isAfter(now())) {
      assertTrue(actualTrialingUserSubscription.hasValidSubscription());
    }
    if (actualUserSubscription.getSubscriptions().getFirst().getEndDatetime().isAfter(now())) {
      assertTrue(actualUserSubscription.hasValidSubscription());
      assertTrue(
          actualUserSubscription.getSubscriptions().stream()
              .noneMatch(Subscription::hasFreeTrialPeriod));
    }
    assertTrue(true); // skip test once trial expired
  }

  @Test
  void user_is_not_eligible_to_subscription_but_active() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any())).thenReturn(Optional.empty());
    var userJane = userRepository.findByEmail("jane@email.com").orElseThrow();

    var actual = subject.getSubscriptionByUserId(userJane.getId());

    var expected =
        UserSubscription.builder()
            .user(userJane)
            .subscriptions(
                List.of(
                    Subscription.builder()
                        .active(true)
                        .startDatetime(actual.getSubscriptions().getFirst().getStartDatetime())
                        .endDatetime(actual.getSubscriptions().getFirst().getEndDatetime())
                        .build()))
            .build();
    assertEquals(expected, actual);
  }

  private static List<String> subscriptionProductFeatures() {
    String[] array = {
      "Activation de notre intelligence artificielle qui analyse les toitures.",
      "20 analyses de toitures incluses.",
      "Outil de devis-facturation personnalisé (acomptes et relance).",
      "Encaissement des paiements instantanement par QR code, mails ou SMS en 1 clic.",
      "Aggregation de votre compte bancaire.",
      "Support 7 jours sur 7."
    };
    return Arrays.stream(array).toList();
  }

  private static String defaultSubscriptionProductId() {
    return "prod_RFgyd9ExtdsCw8";
  }
}
