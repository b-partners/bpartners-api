package app.bpartners.api.unit;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceTriggered;
import app.bpartners.api.endpoint.rest.controller.SubscriptionController;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionConsumptionLogRestMapper;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingType;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlan;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionControllerTest {
  SubscriptionConsumptionLogRestMapper consumptionLogRestMapper =
      new SubscriptionConsumptionLogRestMapper();
  SubscriptionPlanRestMapper subscriptionPlanRestMapper =
      new SubscriptionPlanRestMapper(
          mock(app.bpartners.api.repository.jpa.UserIgnoredTrialPeriodJpaRepository.class));
  SubscriptionService subscriptionServiceMock = mock(SubscriptionService.class);
  EventProducer eventProducerMock = mock(EventProducer.class);
  TemporalUtils temporalUtils = new TemporalUtils();
  app.bpartners.api.service.user.UserService userServiceMock = mock();
  app.bpartners.api.service.subscription.SubscriptionUpgradeService upgradeServiceMock = mock();

  SubscriptionController subject =
      new SubscriptionController(
          eventProducerMock,
          subscriptionServiceMock,
          consumptionLogRestMapper,
          subscriptionPlanRestMapper,
          mock(app.bpartners.api.service.subscription.SubscriptionStripeBackfillService.class),
          mock(app.bpartners.api.service.subscription.SubscriptionInvoiceExportService.class),
          userServiceMock,
          new app.bpartners.api.endpoint.rest.validator.CreateSubscriptionUpgradeRestValidator(),
          upgradeServiceMock,
          new app.bpartners.api.endpoint.rest.mapper.SubscriptionUpgradeRestMapper());

  @Test
  void triggers_the_subscription_remaining_period_invoice_event() {
    var returned = subject.triggerSubscriptionRemainingPeriodInvoice();

    assertEquals("Subscription remaining period invoice triggered successfully", returned);
    verify(eventProducerMock).accept(List.of(new SubscriptionRemainingPeriodInvoiceTriggered()));
  }

  @Test
  void get_subscription_plans() {
    var page = new app.bpartners.api.model.PageFromOne(1);
    var pageSize = new app.bpartners.api.model.BoundedPageSize(100);
    when(subscriptionServiceMock.getSubscribablePlans(page, pageSize))
        .thenReturn(
            List.of(
                SubscriptionProduct.builder()
                    .id("essential")
                    .name("Essentiel")
                    .description("desc")
                    .features(List.of("f1"))
                    .billingType(
                        app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT)
                    .priceInCentsWithoutVat(4900L)
                    .vatPercent(2000L)
                    .includedCreditsPerBillingPeriod(10L)
                    .creditCostPerAnalysis(2L)
                    .freeUsageThreshold(20L)
                    .overageUnitPriceInCents(200L)
                    .trialPeriodDays(7)
                    .mostChosen(true)
                    .deprecated(true)
                    .displayPosition(3)
                    .build()));

    var actual = subject.getSubscriptionPlans(page, pageSize);

    assertEquals(
        List.of(
            new SubscriptionPlan()
                .id("essential")
                .name("Essentiel")
                .description("desc")
                .features(List.of("f1"))
                .inheritedFromPlanName(null)
                .billingType(SubscriptionBillingType.COMMITMENT)
                .priceInCentsWithVat(5880L)
                .priceInCentsWithoutVat(4900L)
                .vatPercent(2000L)
                .includedCreditsPerBillingPeriod(10L)
                .creditCostPerAnalysis(2L)
                .freeUsageThreshold(20L)
                .overageUnitPriceInCents(200L)
                .trialPeriodDays(7)
                .isMostChosen(true)
                .isDeprecated(true)
                .displayPosition(3)),
        actual);
  }

  @Test
  void get_consumptions_log_by_user_id() {
    var userId = randomUUID().toString();
    var startOfMonth = temporalUtils.startOfMonth();
    var endOfMonth = temporalUtils.endOfMonth();
    when(subscriptionServiceMock.findConsumptionLogsByUserId(userId, startOfMonth, endOfMonth))
        .thenReturn(
            List.of(
                SubscriptionConsumptionLog.builder()
                    .consumptionType(ROOF_ANALYSIS)
                    .consumptionUnit(UNIT)
                    .build()));
    var expected =
        List.of(
            new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
                .consumptionType(ConsumptionType.ROOF_ANALYSIS)
                .consumptionUnit(ConsumptionUnit.UNIT));

    var actual = subject.getConsumptionLogsByUserId(userId, startOfMonth, endOfMonth);

    assertEquals(expected, actual);
  }

  @Test
  void upgrades_the_subscription_of_the_user_to_the_requested_plan() {
    var user = app.bpartners.api.model.User.builder().id("user_id").build();
    when(userServiceMock.getUserById("user_id")).thenReturn(user);
    when(upgradeServiceMock.upgrade(
            user, "pro_id", app.bpartners.api.model.subscription.BillingInterval.YEARLY))
        .thenReturn(
            app.bpartners.api.model.subscription.UserSubscriptionUpgrade.builder()
                .id("upgrade_id")
                .fromSubscriptionProductId("essential_id")
                .toSubscriptionProductId("pro_id")
                .fromBillingInterval(app.bpartners.api.model.subscription.BillingInterval.MONTHLY)
                .toBillingInterval(app.bpartners.api.model.subscription.BillingInterval.YEARLY)
                .prorationAmountInCentsWithVat(104_129L)
                .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID)
                .build());

    var actual =
        subject.upgradeUserSubscription(
            "user_id",
            new app.bpartners.api.endpoint.rest.model.CreateSubscriptionUpgrade()
                .subscriptionPlanIdentifier("pro_id")
                .billingInterval(app.bpartners.api.endpoint.rest.model.BillingInterval.YEARLY));

    assertEquals("upgrade_id", actual.getId());
    assertEquals("pro_id", actual.getToSubscriptionPlanIdentifier());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.BillingInterval.YEARLY,
        actual.getToBillingInterval());
    assertEquals(104_129L, actual.getProrationAmountInCentsWithVat());
  }

  @Test
  void rejects_an_upgrade_without_plan_identifier() {
    var withoutPlanIdentifier =
        new app.bpartners.api.endpoint.rest.model.CreateSubscriptionUpgrade();

    org.junit.jupiter.api.Assertions.assertThrows(
        app.bpartners.api.model.exception.BadRequestException.class,
        () -> subject.upgradeUserSubscription("user_id", withoutPlanIdentifier));
  }

  @Test
  void lists_the_upgrades_of_the_user_filtered_on_their_status() {
    when(upgradeServiceMock.findUpgrades(
            "user_id", app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING))
        .thenReturn(
            List.of(
                app.bpartners.api.model.subscription.UserSubscriptionUpgrade.builder()
                    .id("pending_upgrade_id")
                    .fromBillingInterval(
                        app.bpartners.api.model.subscription.BillingInterval.MONTHLY)
                    .toBillingInterval(app.bpartners.api.model.subscription.BillingInterval.MONTHLY)
                    .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING)
                    .build()));

    var actual =
        subject.getUserSubscriptionUpgrades(
            "user_id", app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeStatus.PENDING);

    assertEquals(1, actual.size());
    assertEquals("pending_upgrade_id", actual.getFirst().getId());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeStatus.PENDING,
        actual.getFirst().getStatus());
  }

  @Test
  void lists_all_the_upgrades_of_the_user_without_status_filter() {
    when(upgradeServiceMock.findUpgrades("user_id", null)).thenReturn(List.of());

    assertEquals(0, subject.getUserSubscriptionUpgrades("user_id", null).size());
  }

  @Test
  void upgrades_keeping_the_current_billing_interval_when_none_is_requested() {
    var user = app.bpartners.api.model.User.builder().id("user_id").build();
    when(userServiceMock.getUserById("user_id")).thenReturn(user);
    when(upgradeServiceMock.upgrade(user, "pro_id", null))
        .thenReturn(
            app.bpartners.api.model.subscription.UserSubscriptionUpgrade.builder()
                .id("upgrade_id")
                .fromBillingInterval(app.bpartners.api.model.subscription.BillingInterval.MONTHLY)
                .toBillingInterval(app.bpartners.api.model.subscription.BillingInterval.MONTHLY)
                .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID)
                .build());

    var actual =
        subject.upgradeUserSubscription(
            "user_id",
            new app.bpartners.api.endpoint.rest.model.CreateSubscriptionUpgrade()
                .subscriptionPlanIdentifier("pro_id"));

    assertEquals("upgrade_id", actual.getId());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.BillingInterval.MONTHLY,
        actual.getToBillingInterval());
  }
}
