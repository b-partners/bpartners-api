package app.bpartners.api.integration;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionType.MONTHLY;
import static app.bpartners.api.payment.StripeConf.defaultCurrency;
import static java.time.Month.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.integration.conf.StripeMockedThirdParties;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.service.subscription.StripeSessionFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PriceCreateParams;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class StripeSessionFactoryIT extends StripeMockedThirdParties {
  TemporalUtils temporalUtilsMock = mock(TemporalUtils.class);
  StripeSessionFactory subject = new StripeSessionFactory(temporalUtilsMock);
  @Autowired SubscriptionProductRepository subscriptionProductRepository;
  @Autowired StripeClient stripeClient;
  Customer testCustomer;
  Price testPrice;
  SubscriptionProduct subscriptionProductRoofAnalysis;

  @BeforeEach
  void setUp() throws StripeException {
    testCustomer =
        Customer.create(CustomerCreateParams.builder().setEmail("test@example.com").build());
    subscriptionProductRoofAnalysis =
        subscriptionProductRepository.findByConsumptionTypeAttached(ROOF_ANALYSIS);
    testPrice =
        Price.create(
            PriceCreateParams.builder()
                .setCurrency(defaultCurrency())
                .setProduct(subscriptionProductRoofAnalysis.getE2Id())
                .setUnitAmount(200L)
                .setRecurring(
                    PriceCreateParams.Recurring.builder()
                        .setUsageType(PriceCreateParams.Recurring.UsageType.METERED)
                        .setInterval(PriceCreateParams.Recurring.Interval.MONTH)
                        .build())
                .build());
  }

  @Test
  void create_session_if_trial_end_is_before_end_of_actual_month_not_between_1_and_4_of_month()
      throws StripeException {
    var trialEnd = LocalDate.of(2025, APRIL, 20);
    var billingCycleAnchorLocalDate = LocalDate.of(2025, MAY, 5);
    var startOfNextMonth = LocalDate.of(2025, MAY, 1);
    var fourthOfNextMonth = LocalDate.of(2025, MAY, 4);
    var endOfActualMonth = LocalDate.of(2025, APRIL, 30);
    var startOfActualMonth = LocalDate.of(2025, APRIL, 1);
    var fourthOfActualMonth = LocalDate.of(2025, APRIL, 4);
    var billingCycleAnchor =
        Date.from(billingCycleAnchorLocalDate.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant())
                .getTime()
            / 1000L;
    var stripeCustomer = mock(Customer.class);
    var subscriptionProduct = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var redirectionUrls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    when(stripeCustomer.getId()).thenReturn(testCustomer.getId());
    when(subscription.getSubscriptionProduct()).thenReturn(subscriptionProduct);
    when(subscriptionProduct.getE2Id()).thenReturn(subscriptionProductRoofAnalysis.getE2Id());
    when(subscriptionProduct.getPriceInCents()).thenReturn(1000L);
    when(subscriptionProduct.getType()).thenReturn(MONTHLY);
    when(price.getId()).thenReturn(testPrice.getId());
    when(redirectionUrls.getSuccessUrl()).thenReturn("https://success.url");
    when(redirectionUrls.getFailureUrl()).thenReturn("https://cancel.url");
    when(temporalUtilsMock.startOfNextMonth()).thenReturn(startOfNextMonth);
    when(temporalUtilsMock.fourthOfNextMonth()).thenReturn(fourthOfNextMonth);
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(endOfActualMonth);
    when(temporalUtilsMock.startOfActualMonth()).thenReturn(startOfActualMonth);
    when(temporalUtilsMock.fourthOfActualMonth()).thenReturn(fourthOfActualMonth);

    var actual =
        subject.createSession(
            trialEnd,
            stripeCustomer,
            subscriptionProduct,
            price,
            redirectionUrls,
            billingCycleAnchor,
            subscription);

    log.info("Session: {}", actual);
    assertEquals("subscription", actual.getMode());
    assertEquals("https://success.url", actual.getSuccessUrl());
  }

  @Test
  void create_session_if_trial_end_is_between_1_to_4_next_month() throws StripeException {
    var trialEnd = LocalDate.of(2025, MAY, 1);
    var billingCycleAnchorLocalDate = LocalDate.of(2025, JUNE, 5);
    var startOfNextMonth = LocalDate.of(2025, MAY, 1);
    var fourthOfNextMonth = LocalDate.of(2025, MAY, 4);
    var endOfActualMonth = LocalDate.of(2025, APRIL, 30);
    var startOfActualMonth = LocalDate.of(2025, APRIL, 1);
    var fourthOfActualMonth = LocalDate.of(2025, APRIL, 4);
    var billingCycleAnchor =
        Date.from(billingCycleAnchorLocalDate.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant())
                .getTime()
            / 1000L;
    var stripeCustomer = mock(Customer.class);
    var subscriptionProduct = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var redirectionUrls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    when(stripeCustomer.getId()).thenReturn(testCustomer.getId());
    when(subscription.getSubscriptionProduct()).thenReturn(subscriptionProduct);
    when(subscriptionProduct.getE2Id()).thenReturn(subscriptionProductRoofAnalysis.getE2Id());
    when(subscriptionProduct.getPriceInCents()).thenReturn(1000L);
    when(subscriptionProduct.getType()).thenReturn(MONTHLY);
    when(price.getId()).thenReturn(testPrice.getId());
    when(redirectionUrls.getSuccessUrl()).thenReturn("https://success.url");
    when(redirectionUrls.getFailureUrl()).thenReturn("https://cancel.url");
    when(temporalUtilsMock.startOfNextMonth()).thenReturn(startOfNextMonth);
    when(temporalUtilsMock.fourthOfNextMonth()).thenReturn(fourthOfNextMonth);
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(endOfActualMonth);
    when(temporalUtilsMock.startOfActualMonth()).thenReturn(startOfActualMonth);
    when(temporalUtilsMock.fourthOfActualMonth()).thenReturn(fourthOfActualMonth);

    var actual =
        subject.createSession(
            trialEnd,
            stripeCustomer,
            subscriptionProduct,
            price,
            redirectionUrls,
            billingCycleAnchor,
            subscription);

    log.info("Session: {}", actual);
    assertEquals("setup", actual.getMode());
    assertEquals("https://success.url", actual.getSuccessUrl());
  }

  @Test
  void create_session_if_trial_end_is_between_1_to_4_actual_month() throws StripeException {
    var trialEnd = LocalDate.of(2025, APRIL, 1);
    var billingCycleAnchorLocalDate = LocalDate.of(2025, MAY, 5);
    var startOfNextMonth = LocalDate.of(2025, MAY, 1);
    var fourthOfNextMonth = LocalDate.of(2025, MAY, 4);
    var endOfActualMonth = LocalDate.of(2025, APRIL, 30);
    var startOfActualMonth = LocalDate.of(2025, APRIL, 1);
    var fourthOfActualMonth = LocalDate.of(2025, APRIL, 4);
    var billingCycleAnchor =
        Date.from(billingCycleAnchorLocalDate.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant())
                .getTime()
            / 1000L;
    var stripeCustomer = mock(Customer.class);
    var subscriptionProduct = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var redirectionUrls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    when(stripeCustomer.getId()).thenReturn(testCustomer.getId());
    when(subscription.getSubscriptionProduct()).thenReturn(subscriptionProduct);
    when(subscriptionProduct.getE2Id()).thenReturn(subscriptionProductRoofAnalysis.getE2Id());
    when(subscriptionProduct.getPriceInCents()).thenReturn(1000L);
    when(subscriptionProduct.getType()).thenReturn(MONTHLY);
    when(price.getId()).thenReturn(testPrice.getId());
    when(redirectionUrls.getSuccessUrl()).thenReturn("https://success.url");
    when(redirectionUrls.getFailureUrl()).thenReturn("https://cancel.url");
    when(temporalUtilsMock.startOfNextMonth()).thenReturn(startOfNextMonth);
    when(temporalUtilsMock.fourthOfNextMonth()).thenReturn(fourthOfNextMonth);
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(endOfActualMonth);
    when(temporalUtilsMock.startOfActualMonth()).thenReturn(startOfActualMonth);
    when(temporalUtilsMock.fourthOfActualMonth()).thenReturn(fourthOfActualMonth);

    var actual =
        subject.createSession(
            trialEnd,
            stripeCustomer,
            subscriptionProduct,
            price,
            redirectionUrls,
            billingCycleAnchor,
            subscription);

    log.info("Session: {}", actual);
    assertEquals("setup", actual.getMode());
    assertEquals("https://success.url", actual.getSuccessUrl());
  }

  @Test
  void create_session_if_trial_end_is_after_4_of_next_month() throws StripeException {
    var trialEnd = LocalDate.of(2025, MAY, 5);
    var billingCycleAnchorLocalDate = LocalDate.of(2025, JUNE, 5);
    var startOfNextMonth = LocalDate.of(2025, MAY, 1);
    var fourthOfNextMonth = LocalDate.of(2025, MAY, 4);
    var endOfActualMonth = LocalDate.of(2025, APRIL, 30);
    var startOfActualMonth = LocalDate.of(2025, APRIL, 1);
    var fourthOfActualMonth = LocalDate.of(2025, APRIL, 4);
    var billingCycleAnchor =
        Date.from(billingCycleAnchorLocalDate.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant())
                .getTime()
            / 1000L;
    var stripeCustomer = mock(Customer.class);
    var subscriptionProduct = mock(SubscriptionProduct.class);
    var price = mock(Price.class);
    var redirectionUrls = mock(RedirectionStatusUrls.class);
    var subscription = mock(Subscription.class);
    when(stripeCustomer.getId()).thenReturn(testCustomer.getId());
    when(subscription.getSubscriptionProduct()).thenReturn(subscriptionProduct);
    when(subscriptionProduct.getE2Id()).thenReturn(subscriptionProductRoofAnalysis.getE2Id());
    when(subscriptionProduct.getPriceInCents()).thenReturn(1000L);
    when(subscriptionProduct.getType()).thenReturn(MONTHLY);
    when(price.getId()).thenReturn(testPrice.getId());
    when(redirectionUrls.getSuccessUrl()).thenReturn("https://success.url");
    when(redirectionUrls.getFailureUrl()).thenReturn("https://cancel.url");
    when(temporalUtilsMock.startOfNextMonth()).thenReturn(startOfNextMonth);
    when(temporalUtilsMock.fourthOfNextMonth()).thenReturn(fourthOfNextMonth);
    when(temporalUtilsMock.endOfActualMonth()).thenReturn(endOfActualMonth);
    when(temporalUtilsMock.startOfActualMonth()).thenReturn(startOfActualMonth);
    when(temporalUtilsMock.fourthOfActualMonth()).thenReturn(fourthOfActualMonth);

    var actual =
        subject.createSession(
            trialEnd,
            stripeCustomer,
            subscriptionProduct,
            price,
            redirectionUrls,
            billingCycleAnchor,
            subscription);

    log.info("Session: {}", actual);
    assertEquals("setup", actual.getMode());
    assertEquals("https://success.url", actual.getSuccessUrl());
  }

  @AfterEach
  void tearDown() throws StripeException {
    testCustomer.delete();
    testPrice.setDeleted(true);
  }
}
