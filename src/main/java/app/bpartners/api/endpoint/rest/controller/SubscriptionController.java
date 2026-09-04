package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantTriggered;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.endpoint.event.model.SubscriptionProductStripeVatBackfillTriggered;
import app.bpartners.api.endpoint.event.model.TransitionalSubscriptionCreditGrantTriggered;
import app.bpartners.api.endpoint.event.model.UpcomingDebitedCustomerExportRequested;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillTriggered;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionConsumptionLogRestMapper;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlan;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
import java.time.YearMonth;
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

  // TODO: temporary endpoint to backfill historical Essential UserSubscriptionProduct for users who
  @PostMapping("/users/subscriptionProductBackfill")
  public String triggerUserSubscriptionProductBackfill() {
    eventProducer.accept(List.of(new UserSubscriptionProductBackfillTriggered()));
    return "UserSubscriptionProduct backfill triggered successfully";
  }

  // TODO: temporary endpoint to backfill the VAT rate onto existing Stripe products' metadata.
  @PostMapping("/subscriptionProducts/stripeVatBackfill")
  public String triggerSubscriptionProductStripeVatBackfill() {
    eventProducer.accept(List.of(new SubscriptionProductStripeVatBackfillTriggered()));
    return "SubscriptionProduct Stripe VAT metadata backfill triggered successfully";
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
