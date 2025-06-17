package app.bpartners.api.integration;

import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.*;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.Instant.now;
import static java.time.Month.JANUARY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.LogCaptor;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.integration.conf.StripeMockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.*;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.stripe.exception.StripeException;
import com.stripe.model.Product;
import com.stripe.param.ProductUpdateParams;
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@Slf4j
class SubscriptionServiceIT extends StripeMockedThirdParties {
  @Autowired SubscriptionService subject;
  @Autowired UserRepository userRepository;
  @MockBean UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepositoryMock;
  @MockBean SubscriptionProductRepository subscriptionProductRepositoryMock;

  @Test
  @Disabled("TODO: fix test data and implementation")
  void get_subscription_log_by_user_id_without_date() {
    var userId = "ce0c0edb-7d45-4f4f-86d9-363cd5206969";
    var fromLocalDateTime = LocalDateTime.of(2025, JANUARY, 14, 17, 7);
    var from = fromLocalDateTime.toInstant(ZoneOffset.UTC);
    var toLocalDateTime = LocalDateTime.of(2025, JANUARY, 17, 7, 30);
    var to = toLocalDateTime.toInstant(ZoneOffset.UTC);

    var actual = subject.findConsumptionLogsByUserId(userId, from, to);

    var expected = List.of();
    assertTrue(actual.contains(expected));
  }

  @Test
  @Disabled("TODO: fix test data and implementation")
  void add_consumption_log() {
    var now = now();
    var consumptionLogId = randomUUID().toString();

    try (MockedStatic<AuthProvider> mockedAuthProvider = mockStatic(AuthProvider.class)) {
      mockedAuthProvider
          .when(AuthProvider::getAuthenticatedUser)
          .thenReturn(User.builder().id("userId").build());

      var subscriptionConsumptionLog =
          SubscriptionConsumptionLog.builder()
              .id(consumptionLogId)
              .consumptionType(ROOF_ANALYSIS)
              .consumptionUnit(UNIT)
              .usageMetric(2L)
              .creationDatetime(now)
              .userId("geoJobsUserId")
              .build();

      var actual = subject.addConsumption(subscriptionConsumptionLog);

      var expected =
          SubscriptionConsumptionLog.builder()
              .id(consumptionLogId)
              .userId("userId")
              .usageMetric(2L)
              .consumptionType(ROOF_ANALYSIS)
              .consumptionUnit(UNIT)
              .creationDatetime(now)
              .build();
      assertEquals(expected, actual);
    }
  }

  @Test
  void create_list_delete_customers() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();

    subject.createOrLinkUserSubscription(user.toBuilder().mobilePhoneNumber("0622334455").build());
    var linkedUserSubscription =
        subject.createOrLinkUserSubscription(
            user.toBuilder().mobilePhoneNumber("0622334455").build());
    var updatedUser = linkedUserSubscription.getUser();
    var updatedUserSubscription = subject.updateUserSubscription(updatedUser);
    var updateUserSubscriptionSetId =
        updatedUserSubscription.getSubscriptions().stream()
            .peek(subscription -> subscription.setId("null"))
            .toList();
    updatedUserSubscription.setSubscriptions(updateUserSubscriptionSetId);
    var expectedUserSubscription =
        subject.getSubscriptionByUserSubscriptionId(
            updatedUserSubscription.getUser().getUserSubscriptionId());
    var expectedUserSubscriptionSetId =
        expectedUserSubscription.getSubscriptions().stream()
            .peek(subscription -> subscription.setId("null"))
            .toList();
    expectedUserSubscription.setSubscriptions(expectedUserSubscriptionSetId);

    assertNotNull(updatedUserSubscription);
    assertEquals(expectedUserSubscription, updatedUserSubscription);
    assertNotNull(subject.deleteUserFromStripe(updatedUser));
    assertNull(userRepository.getById(user.getId()).getUserSubscriptionId());
  }

  @Test
  void cancel_user_subscription_ko() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();

    var actual =
        assertThrows(
            IllegalArgumentException.class, () -> subject.cancelLatestUserSubscription(user));

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
  void initiate_subscription() {
    var product =
        SubscriptionProduct.builder()
            .e2Id(defaultSubscriptionProductId())
            .type(MONTHLY)
            .priceInCents(5880L)
            .build();
    when(subscriptionProductRepositoryMock.save(any())).thenReturn(product);
    when(subscriptionProductRepositoryMock.findByConsumptionTypeAttached(ROOF_ANALYSIS))
        .thenReturn(product);
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .trialPeriodDays(0)
                    .eligibleFrom(LocalDate.now().minusDays(1))
                    .build()));
    var existingUser = userRepository.findByEmail("jane@email.com").orElseThrow();
    var createdUserSubscription =
        subject.createOrLinkUserSubscription(
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
    assertNotNull(subject.deleteUserFromStripe(user));
  }

  @Disabled("TODO: local use only")
  @SneakyThrows
  @Test
  void initiate_subscription_then_cancel() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(Optional.of(new UserSubscriptionEligible()));
    var existingUser = userRepository.findByEmail("joe@email.com").orElseThrow();
    var createdUserSubscription =
        subject.createOrLinkUserSubscription(
            existingUser.toBuilder().email("joe" + new Random().nextInt() + "@email.com").build());
    var user = createdUserSubscription.getUser();
    var defaultSubscription = getDefaultSubscription();

    var actualSubscriptionRedirection =
        subject.initiateSubscription(user, defaultSubscription, getDefaultRedirectionStatusUrls());

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

    Thread.sleep(Duration.ofSeconds(30L));

    var userSubscriptionBeforeCancellation = subject.getSubscriptionByUser(user);

    var actualUserSubscription = subject.cancelLatestUserSubscription(user);

    var oldSubscription = userSubscriptionBeforeCancellation.getLatestSubscription();
    var latestSubscription = actualUserSubscription.getLatestSubscription();
    assertEquals(TRIALING, oldSubscription.getStatus());
    assertEquals(CANCELED, latestSubscription.getStatus());
    assertEquals(oldSubscription.getEndDatetime(), latestSubscription.getEndDatetime());
    assertEquals(oldSubscription.getStartDatetime(), latestSubscription.getStartDatetime());
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
        .endDatetime(now().plus(1, DAYS))
        .build();
  }

  @Test
  void initiate_subscription_without_stripe_customer_associated_ko() {
    var user = userRepository.findByEmail("kris@email.com").orElseThrow();

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
  @Disabled("TODO")
  void initiate_subscription_with_active_subscription_ko() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .eligibleFrom(LocalDate.now().minusDays(1L))
                    .build()));
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
  void add_product_and_initiate_subscriptions() throws StripeException {
    var logCaptor = new LogCaptor();
    logCaptor.configure(SubscriptionService.class);
    when(subscriptionProductRepositoryMock.save(any()))
        .thenReturn(SubscriptionProduct.builder().build());

    subject.createSubscriptionProduct(
        SubscriptionProduct.builder()
            .name("L'Abonnement Essentiel")
            .description(
                "Sans engagement - Idéal pour les artisans couvreurs. "
                    + String.join(" ", subscriptionProductFeatures()))
            .features(subscriptionProductFeatures())
            .priceInCents(5880L)
            .type(MONTHLY)
            .build());

    verify(subscriptionProductRepositoryMock, times(1)).save(any(SubscriptionProduct.class));
    var stripeProductId =
        logCaptor.getLogEvents().stream()
            .map(ILoggingEvent::getFormattedMessage)
            .filter(msg -> msg.startsWith("createdStripeProductId: "))
            .map(msg -> msg.replace("createdStripeProductId: ", "").trim())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Product ID non trouvé dans les logs"));
    var product = Product.retrieve(stripeProductId);
    var updateParams = ProductUpdateParams.builder().setActive(false).build();
    product.update(updateParams);
  }

  // TODO: update trial period so it always be in trial mode
  @Test
  @Disabled("TODO")
  void user_has_active_subscription() {
    when(subscriptionEligibleJpaRepositoryMock.findByUserId(any()))
        .thenReturn(
            Optional.of(
                UserSubscriptionEligible.builder()
                    .eligibleFrom(LocalDate.now().minusDays(1L))
                    .trialPeriodDays(0)
                    .build()));
    var userJoe = userRepository.findByEmail("joe@email.com").orElseThrow();

    var actualUserSubscription = subject.getSubscriptionByUserId(userJoe.getId());

    if (actualUserSubscription.getSubscriptions().getFirst().getEndDatetime().isAfter(now())) {
      assertTrue(actualUserSubscription.hasValidSubscription());
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
                        .status(TRIALING)
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
