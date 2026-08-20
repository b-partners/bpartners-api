package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreditUnitPriceIT extends MockedThirdParties {
  private static final String ESSENTIAL_PLAN_ID = "89f1acdd-c3b9-4717-a21d-355b2021ad58";
  private static final long ESSENTIAL_UNIT_PRICE_WITHOUT_VAT = 500L;
  private static final long ESSENTIAL_UNIT_PRICE_WITH_VAT = 600L;
  private static final long PUBLIC_UNIT_PRICE_WITHOUT_VAT = 1000L;
  private static final long PUBLIC_UNIT_PRICE_WITH_VAT = 1200L;
  private static final long PACK_CREDITS = 10L;

  @Autowired private UserSubscriptionProductService userSubscriptionProductService;
  @Autowired private UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private CreditService subject;

  private final User joeDoe = User.builder().id(JOE_DOE_ID).build();

  @BeforeEach
  void setUp() {
    clearSubscriptionsAndLedger();
  }

  @AfterEach
  void tearDown() {
    clearSubscriptionsAndLedger();
  }

  private void clearSubscriptionsAndLedger() {
    creditTransactionRepository.deleteAll();
    userSubscriptionProductJpaRepository.deleteAll();
  }

  @Test
  void prices_credits_from_the_subscribed_plan() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    var actual = subject.resolveCreditUnitPrice(joeDoe);

    assertEquals(ESSENTIAL_UNIT_PRICE_WITHOUT_VAT, actual.inCentsWithoutVat());
    assertEquals(ESSENTIAL_UNIT_PRICE_WITH_VAT, actual.inCentsWithVat());
    assertEquals(5_000L, actual.totalInCentsWithoutVat(PACK_CREDITS));
    assertEquals(6_000L, actual.totalInCentsWithVat(PACK_CREDITS));
  }

  @Test
  void prices_credits_at_the_public_price_once_the_subscription_is_cancelled() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    userSubscriptionProductService.endActiveSubscriptionProducts(
        JOE_DOE_ID, Instant.now().plus(10, DAYS));

    var actual = subject.resolveCreditUnitPrice(joeDoe);

    assertEquals(PUBLIC_UNIT_PRICE_WITHOUT_VAT, actual.inCentsWithoutVat());
    assertEquals(PUBLIC_UNIT_PRICE_WITH_VAT, actual.inCentsWithVat());
    assertEquals(10_000L, actual.totalInCentsWithoutVat(PACK_CREDITS));
    assertEquals(12_000L, actual.totalInCentsWithVat(PACK_CREDITS));
  }

  @Test
  void prices_credits_at_the_public_price_once_the_subscription_ended() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    userSubscriptionProductService.endActiveSubscriptionProducts(
        JOE_DOE_ID, Instant.now().minus(1, DAYS));

    var actual = subject.resolveCreditUnitPrice(joeDoe);

    assertEquals(PUBLIC_UNIT_PRICE_WITHOUT_VAT, actual.inCentsWithoutVat());
    assertEquals(PUBLIC_UNIT_PRICE_WITH_VAT, actual.inCentsWithVat());
  }

  @Test
  void prices_credits_at_the_public_price_without_any_subscription() {
    var actual = subject.resolveCreditUnitPrice(joeDoe);

    assertEquals(PUBLIC_UNIT_PRICE_WITHOUT_VAT, actual.inCentsWithoutVat());
    assertEquals(PUBLIC_UNIT_PRICE_WITH_VAT, actual.inCentsWithVat());
  }

  @Test
  void prices_credits_from_the_plan_subscribed_again_after_a_cancellation() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    userSubscriptionProductService.endActiveSubscriptionProducts(
        JOE_DOE_ID, Instant.now().plus(10, DAYS));

    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    var actual = subject.resolveCreditUnitPrice(joeDoe);

    assertEquals(ESSENTIAL_UNIT_PRICE_WITHOUT_VAT, actual.inCentsWithoutVat());
    assertEquals(ESSENTIAL_UNIT_PRICE_WITH_VAT, actual.inCentsWithVat());
  }
}
