package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.ImmediateSubscriptionCancellationTriggered;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantTriggered;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceTriggered;
import app.bpartners.api.endpoint.event.model.TransitionalSubscriptionCreditGrantTriggered;
import app.bpartners.api.endpoint.event.model.UpcomingDebitedCustomerExportRequested;
import app.bpartners.api.endpoint.event.model.UserDefaultPaymentMethodBackfillTriggered;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionConsumptionLogRestMapper;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlan;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.SubscriptionInvoiceExportService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.subscription.SubscriptionStripeBackfillService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SubscriptionController {
  private final EventProducer eventProducer;
  private final SubscriptionService service;
  private final SubscriptionConsumptionLogRestMapper subscriptionConsumptionLogRestMapper;
  private final SubscriptionPlanRestMapper subscriptionPlanRestMapper;
  private final SubscriptionStripeBackfillService subscriptionStripeBackfillService;
  private final SubscriptionInvoiceExportService subscriptionInvoiceExportService;

  @GetMapping("/subscriptionInvoices/export")
  public PreSignedURL exportSubscriptionInvoices(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    var preSignedLink = subscriptionInvoiceExportService.generateExportLink(from, to);
    return new PreSignedURL()
        .value(preSignedLink.getValue())
        .expirationDelay(preSignedLink.getExpirationDelay())
        .updatedAt(preSignedLink.getUpdatedAt());
  }

  @GetMapping("/subscriptionPlans")
  public List<SubscriptionPlan> getSubscriptionPlans(
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize) {
    return service.getSubscribablePlans(page, pageSize).stream()
        .map(subscriptionPlanRestMapper::toRest)
        .toList();
  }

  @PostMapping("/monthlyUpcomingDebitedCustomers/{year}/{month}")
  public void upcomingDebitedCustomersExport(@PathVariable int year, @PathVariable int month) {
    if (month < 1 || month > 12) {
      throw new BadRequestException("Month must be between 1 and 12");
    }
    eventProducer.accept(
        List.of(new UpcomingDebitedCustomerExportRequested(YearMonth.of(year, month))));
  }

  @PostMapping("/subscriptions/stripeBackfill")
  public List<SubscriptionStripeBackfillService.UserBackfillReport> backfillSubscriptionsFromStripe(
      @RequestBody List<String> userIds,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paidSince,
      @RequestParam(defaultValue = "true") boolean dryRun,
      @RequestParam(defaultValue = "false") boolean sendsToSubscriber) {
    if (userIds == null || userIds.isEmpty()) {
      throw new BadRequestException("userIds must list the users to backfill");
    }
    return subscriptionStripeBackfillService.backfill(
        userIds,
        paidSince.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant(),
        dryRun,
        sendsToSubscriber);
  }

  @PostMapping("/monthlySubscriptionInvoiceTrigger")
  public String triggerMonthlySubscriptionInvoice() {
    eventProducer.accept(List.of(new MonthlySubscriptionInvoiceTriggered()));
    return "Monthly subscription invoice triggered successfully";
  }

  @PostMapping("/monthlySubscriptionCreditGrantTrigger")
  public String triggerMonthlySubscriptionCreditGrant() {
    eventProducer.accept(List.of(new MonthlySubscriptionCreditGrantTriggered()));
    return "Monthly subscription credit grant triggered successfully";
  }

  @PostMapping("/transitionalSubscriptionCreditGrantTrigger")
  public String triggerTransitionalSubscriptionCreditGrant() {
    eventProducer.accept(List.of(new TransitionalSubscriptionCreditGrantTriggered()));
    return "Transitional subscription credit grant triggered successfully";
  }

  @PostMapping("/subscriptionRemainingPeriodInvoiceTrigger")
  public String triggerSubscriptionRemainingPeriodInvoice() {
    eventProducer.accept(List.of(new SubscriptionRemainingPeriodInvoiceTriggered()));
    return "Subscription remaining period invoice triggered successfully";
  }

  @PostMapping("/immediateSubscriptionCancellationTrigger")
  public String triggerImmediateSubscriptionCancellation() {
    eventProducer.accept(List.of(new ImmediateSubscriptionCancellationTriggered()));
    return "Immediate subscription cancellation triggered successfully";
  }

  @PostMapping("/users/defaultPaymentMethodBackfill")
  public String triggerUserDefaultPaymentMethodBackfill() {
    eventProducer.accept(List.of(new UserDefaultPaymentMethodBackfillTriggered()));
    return "User default payment method backfill triggered successfully";
  }

  @GetMapping("/users/{uId}/subscriptionConsumptionLogs")
  public List<SubscriptionConsumptionLog> getConsumptionLogsByUserId(
      @PathVariable String uId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to) {
    return service.findConsumptionLogsByUserId(uId, from, to).stream()
        .map(subscriptionConsumptionLogRestMapper::toRest)
        .toList();
  }
}
