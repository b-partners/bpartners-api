package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionConsumptionLogRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
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

  @PostMapping("/monthlySubscriptionInvoiceTrigger")
  public String triggerMonthlySubscriptionInvoice() {
    eventProducer.accept(List.of(new MonthlySubscriptionInvoiceTriggered()));
    return "Monthly subscription invoice triggered successfully";
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
  @PostMapping("/users/{uId}/subscriptionConsumptionLogs")
  public SubscriptionConsumptionLog addSubscriptionConsumptionLogs(
          @PathVariable("uId") String uId,
          @RequestBody SubscriptionConsumptionLog subscriptionConsumptionLog) {
    app.bpartners.api.model.subscription.SubscriptionConsumptionLog consumptionLog =
            service.addConsumption(
                    subscriptionConsumptionLogRestMapper.toDomain(subscriptionConsumptionLog));
    return subscriptionConsumptionLogRestMapper.toRest(consumptionLog);
  }
}
