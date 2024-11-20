package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class SubscriptionServiceIT extends MockedThirdParties {
  @Autowired SubscriptionService subject;
  @Autowired UserRepository userRepository;

  @Test
  void create_list_delete_customers() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();

    var createdUserSubscription =
        subject.createUserSubscription(user.toBuilder().mobilePhoneNumber("0622334455").build());
    var updatedUser = createdUserSubscription.getUser();
    var updatedUserSubscription = subject.updateUserSubscription(updatedUser);

    assertNotNull(updatedUserSubscription);
    assertEquals(
        subject.findUserSubscriptionByCriteria(
            updatedUserSubscription.getUser().getUserSubscriptionId()),
        updatedUserSubscription);
    assertNotNull(subject.cancelUserSubscription(updatedUser));
    assertNull(userRepository.getById(user.getId()).getUserSubscriptionId());
  }

  @Test
  void user_has_free_trial_subscription_period() {
    var user = userRepository.findByEmail("bernard@email.com").orElseThrow();

    var actualSubscriptions =
        subject.findUserSubscriptionByCriteria(user.getUserSubscriptionId()).getSubscriptions();

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
    var existingUser = userRepository.findByEmail("joe@email.com").orElseThrow();
    var createdUserSubscription =
        subject.createUserSubscription(
            existingUser.toBuilder().email("joe" + new Random().nextInt() + "@email.com").build());
    var user = createdUserSubscription.getUser();

    var actualSubscriptionRedirection =
        subject.initiateSubscription(
            user,
            Subscription.builder()
                .subscriptionProduct(
                    subject.getSubscriptionProductByE2Id(defaultSubscriptionProductId()))
                .endDatetime(Instant.now().plus(30L, DAYS))
                .freeTrialDays(14L)
                .build(),
            new RedirectionStatusUrls()
                .failureUrl("http://loclahost/cancelUrl")
                .successUrl("http://loclahost/successUrl"));

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

  @Test
  @Disabled
  void add_product_and_initiate_subscriptions() {
    var actualSubscriptionProduct =
        subject.createSubscriptionProduct(
            SubscriptionProduct.builder()
                .name("L'Abonnement Essentiel")
                .description(
                    "Sans engagement - Idéal pour les artisans couvreurs. "
                        + String.join(" ", subscriptionProductFeatures()))
                .features(subscriptionProductFeatures())
                .imageUrl(getDefaultProductImage())
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
            .imageUrl(getDefaultProductImage())
            .type(MONTHLY)
            .priceInCents(4900L)
            .creationDatetime(actualSubscriptionProduct.getCreationDatetime())
            .build();
    assertEquals(expectedSubscriptionProduct, actualSubscriptionProduct);
    assertNotNull(actualSubscriptionProduct.getId());
    assertNotNull(actualSubscriptionProduct.getE2Id());
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

  private static @NotNull String getDefaultProductImage() {
    return "https://stripe-dashboard.s3.eu-west-3.amazonaws.com/bpartners%20stripe%20product%20visuel.jpg?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEPP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCWV1LXdlc3QtMyJHMEUCIEOUpQ8xA7WDnU0HTx9n1%2BaGmqYtj3FNbYwOyhplolZCAiEAqG8reTXYJ6gesD6lJM7lSTq%2FhHJ2vRiIKhAfqd8UX78q6QMIjP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FARACGgw2ODg2MDU4Nzk3MTgiDAbtmu0hjsq%2BYBY53Sq9A%2BC3jQPp3WGENKcwsW5JlWdDljho67DWGOt7Wt8bJQwQ%2FTb4hqo9gyNuXIe%2FixkE4OIR05x7qwS7OOlZol%2Fw44HaYHiHACHTSuLP3N6AZUVnMcF3SyFfqkpKCqa%2FJLDyZyu2cupvo43hl%2BxfpS0ED15MXS1epYBwPuvJn8yCWD9wo1MH0qK9rCSdC1SAvyYj4XFyDv%2BCr1%2ByYM6EPRXY3waWiJ9001%2FZVcwJahg8%2BqSpYn%2FdBKhG%2FT5ok%2BM3O3nfVOYeSmtG37g2rzfeL1nWrkEEpDkiqh2K0LipJSh8u672x%2BkGQ%2Br3BeZuoq9c6fpdFk1jemi5s2jGxolXELev%2B2vHO6qZ%2FtKgKRpuA%2Fi5fgw9YIP5GGXf%2BqM52sS62367ni9L56es898ySEjQ0tYuqlTudBDlrwDtMupJ8tNDgf0QdBCYxRq9BYUtd%2BIwlds2jBDkB8rA4rIXm59clSpPMJR%2FwU7B9UiI2L%2BVa35HAu3TkGjPP%2B6GIcSnPUPdtAYC8dCDIHNaVuKduZ1lTjlcGgwJ1WIV7jL9aV%2FQBucsmtMmR71qHaKOouSfTYjyMsemIM62%2BI7aVwD%2BGE8zo%2BQwzv%2F2uQY65AJUtR%2FkEg8w8L77HpNoAYztTUbE16WWifW1sEtpss5aGxK2eYttCJ0Epr5F%2B3zdhg2keDgR0amXs%2BOPf%2F%2B3RAeSzkXnxu7BUzTMBRXKe2yaaX3cTrtL05YBBhZTCngTy%2FCi7646pjFUx9s1coBX65lOP%2FCTGzbYcYEUxmnkAyGo2biJxJNc2mol1lmdR75%2F7JogSWaxACfROI8tkwwcqLTgB2Cf7zGjAHib4W3Us8Avg%2BtT4tIYvpPI2kiQb%2BYhxV1jdxsZp24%2F7sVFOFq209ysUNRqFeIgPMwS1AGZ%2FWVZgKvsHu0AIfwAteqo3Qd6ERoUPTTWQidWv9Za86lBgK9exgZMJGnGeDBn1w5hJmCnojNY9K3CnY9SgCBcMNp0TVGWa9RsmnzpCgz7Yea4dzg%2FAiIT4YFZBVsxSmTYssOgu%2FNMZ6CBW8rUT%2BjVahzaRV3UuBQIOZqyLBEV1t5j0E3TzoTxWA%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIA2AVA33WTPJWTJZCI%2F20241120%2Feu-west-3%2Fs3%2Faws4_request&X-Amz-Date=20241120T105554Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=7cb1484ccfdf15cc8836a0236739870bfecf5dd7a298f86000f3048d4fe299cb";
  }

  private static String defaultSubscriptionProductId() {
    return "prod_RFgyd9ExtdsCw8";
  }
}
