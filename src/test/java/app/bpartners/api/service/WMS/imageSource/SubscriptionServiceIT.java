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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
class SubscriptionServiceIT extends MockedThirdParties {
  @Autowired SubscriptionService subject;
  @Autowired UserRepository userRepository;

  @Test
  void create_list_delete_customers() {
    var user = userRepository.findByEmail("jane@email.com").orElseThrow();

    var createdUserSubscription =
        subject.createUserSubscription(
            user.toBuilder().mobilePhoneNumber("0622334455").build());
    var updatedUser = createdUserSubscription.getUser();
    var updatedUserSubscription = subject.updateUserSubscription(updatedUser);

    assertNotNull(updatedUserSubscription);
    assertEquals(
        subject.findUserSubscriptionByCriteria(updatedUserSubscription.getUser().getUserSubscriptionId()), updatedUserSubscription);
    assertNotNull(subject.cancelUserSubscription(updatedUser));
    assertNull(userRepository.getById(user.getId()).getUserSubscriptionId());
  }

  @Test
  void user_has_free_trial_subscription_period() {
    var user = userRepository.findByEmail("bernard@email.com").orElseThrow();


    var actualSubscriptions = subject.findUserSubscriptionByCriteria(user.getUserSubscriptionId()).getSubscriptions();

    assertFalse(actualSubscriptions.isEmpty());
    var subscription = actualSubscriptions.getFirst();
    assertTrue(subscription.hasFreeTrialPeriod());
    assertEquals(14L, subscription.getFreeTrialDays());
    assertEquals(LocalDate.of(2024, 11, 19),
            subscription.getFreeTrialStart()
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDate());
    assertEquals(LocalDate.of(2024, 12, 3),
            subscription.getFreeTrialEnd()
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDate());
    assertEquals(LocalDate.of(2024, 11, 19),
            subscription.getStartDatetime()
            .atZone(ZoneId.of("Europe/Paris"))
            .toLocalDate());
    assertEquals(LocalDate.of(2025, 1, 3),
            subscription.getEndDatetime()
                    .atZone(ZoneId.of("Europe/Paris"))
                    .toLocalDate());
  }

  @Test
  void add_product_and_initiate_subscriptions() {
    var existingUser = userRepository.findByEmail("joe@email.com").orElseThrow();
    var createdUserSubscription =
            subject.createUserSubscription(existingUser.toBuilder()
                            .email("joe" + new Random().nextInt() + "@email.com")
                    .build());
    var user = createdUserSubscription.getUser();

    var actualSubscriptionProduct =
            subject.createSubscriptionProduct(
                    SubscriptionProduct.builder()
                            .name("L'Abonnement Essentiel")
                            .description("Sans engagement - Idéal pour les artisans couvreurs. "
                                    + String.join(" ", subscriptionProductFeatures()))
                            .features(subscriptionProductFeatures())
                            .imageUrl(getDefaultProductImage())
                            .priceInCents(4900L)
                            .type(MONTHLY)
                            .build());
    var actualSubscriptionRedirection = subject.initiateSubscription(user,
            Subscription.builder()
            .subscriptionProduct(actualSubscriptionProduct)
            .endDatetime(Instant.now().plus(30L, DAYS))
            .freeTrialDays(14L)
            .build(),
            new RedirectionStatusUrls()
                    .failureUrl("http://loclahost/cancelUrl")
                    .successUrl("http://loclahost/successUrl"));

    assertEquals(
            SubscriptionProduct.builder()
                    .id(actualSubscriptionProduct.getId())
                    .e2Id(actualSubscriptionProduct.getE2Id())
                    .name("L'Abonnement Essentiel")
                    .description("Sans engagement - Idéal pour les artisans couvreurs. "
                            + String.join(" ", subscriptionProductFeatures()))
                    .features(subscriptionProductFeatures())
                    .imageUrl(getDefaultProductImage())
                    .type(MONTHLY)
                    .priceInCents(4900L)
                    .creationDatetime(actualSubscriptionProduct.getCreationDatetime())
                    .build(),
            actualSubscriptionProduct);
    assertNotNull(actualSubscriptionProduct.getId());
    assertNotNull(actualSubscriptionProduct.getE2Id());
    assertNotNull(actualSubscriptionRedirection);
    assertNotNull(actualSubscriptionRedirection.getRedirectionUrl());
    assertTrue(actualSubscriptionRedirection.getRedirectionUrl().contains("https://checkout.stripe.com/c/pay"));
    assertNotNull(actualSubscriptionRedirection.getRedirectionStatusUrls());
    assertNotNull(actualSubscriptionRedirection.getRedirectionStatusUrls().getSuccessUrl());
    assertNotNull(actualSubscriptionRedirection.getRedirectionStatusUrls().getFailureUrl());
    log.info("Redirection stripe checkout url = {}", actualSubscriptionRedirection.getRedirectionUrl());
  }

  private static List<String> subscriptionProductFeatures() {
    String[] array = {"Activation de notre intelligence artificielle qui analyse les toitures.",
    "20 analyses de toitures incluses.",
    "Outil de devis-facturation personnalisé (acomptes et relance).",
    "Encaissement des paiements instantanement par QR code, mails ou SMS en 1 clic.",
    "Aggregation de votre compte bancaire.",
    "Support 7 jours sur 7."};
    return Arrays.stream(array).toList();
  }

  private static @NotNull String getDefaultProductImage() {
    return "https://stripe-dashboard.s3.eu-west-3.amazonaws.com/bpartners%20stripe%20product%20visuel.jpg?response-content-disposition=inline&X-Amz-Content-Sha256=UNSIGNED-PAYLOAD&X-Amz-Security-Token=IQoJb3JpZ2luX2VjENv%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaCWV1LXdlc3QtMyJGMEQCIEZGwwywAnL%2FtS74LK1RfxbjjfVWc%2FtxUYsCY%2B95hSIWAiBZ0oUaSFpsrWRHZ1FIpRsWwjfX3rtlqJsu1s15At8ZNSrgAwh0EAIaDDY4ODYwNTg3OTcxOCIM1PHIXqP%2FC%2Bd3O2ZIKr0DKekk9Xm06dw405NlqmMMIbdksW%2B0UsJRJoeQS8c4lkaKwI0DoCmgPVWWrL0ZZjUElO2kdLWlwgBIEy%2F4sthm8D%2FmdDeymKTJTaQf630pk615NHrSkop%2FLk4KS1%2F%2BR9l40DY7aU96%2FaMXyAzsxI65FE6ulxSf2bnkbVOX0tDBKx8QT30twE4Q3GaxB08SsnUvPvsAmAZAg7%2FlfY9%2FaMfibEq%2FPUe06g6j4Gf%2BuqfY3mXGSIC5Z6EEFlVmhV2pnkgSLRUzL5b%2FybqCaDbQTPSp2LA%2By5g3I%2FPn9xBSw%2FYE7r9YoI1DZw4RcfenBfKT9CfNNe78kq2Y7OnPlb1Wrfi1lWMSXdv2Xpl35NpJ8O6QlX%2B1PNZoJy4XiQcZTenDAmrUPxR7NccDe0lyzqyWI258%2BTVFYvrcs%2FItJiibcAOJkj2YbBmqlfzGQEuf7MFMEwg3zYV%2FQMCI85X0jnfl8c%2F%2F012qnATD2AiaIMhc5kQA1YNISHwe%2BRTuFmgXXimbFG8h%2BjvYN6tLbVXgO6Pu3ljkmr9sn09FzvkZKh2GCKjP7ACQaKQKLbiUc468%2FNyAbfoiNTIRUj7AYDiTwv4ohDD40fG5BjrlApC45MDVpsWlNtfpeRTAIFt1F9zFxQIaB491Qn%2BWQ9wsb5SQI7LCXB35r8qY4EfHXzGs5H0t8sVV6VGVgq6iaXJQaXh9KiobpIHCb1ykeC42npk18pKvWV6oVHq8DUHHFhnpVTkfzQDTVH5zqSDKwnpAl1I2rUn5nTQbP5ikh%2FhW05Dz%2BsLnShM4OWum2atnf%2F7WTQrvnm0W2Q3gWXgvCH3iIThrRPoxoybxbSPp3BifQXrzpWWGTrCgtihlAz%2FaPs6KlIeyuzLBIjslCHoBXC5JXmwfQiBuL0sSiKsyGFhb8obg2Mp7tRFKXE2RV4zXkgX7AmgVls%2FqQCVyeo2IMKMBI2tFtqi9JLCCYu9oq1B8ZJLHtbqA18uFc%2FeQ1SSSVXz36I%2F0T1m30JgUaY17Updh8Rc%2F8VJcHg285%2BJJ796ppEOTsDgaHBUrQh6%2FXze7dg09Z10FGIprvY%2BMSWUYIp56w570uA%3D%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIA2AVA33WTLWISWARV%2F20241119%2Feu-west-3%2Fs3%2Faws4_request&X-Amz-Date=20241119T105558Z&X-Amz-Expires=43200&X-Amz-SignedHeaders=host&X-Amz-Signature=27f5ecce37faac9778a8854acdd636ce1a811841c7c1c568d260af7e3a745a77";
  }
}
