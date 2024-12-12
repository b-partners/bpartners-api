package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SubscriptionController {
    private final EventProducer eventProducer;

    @PostMapping("/monthlySubscriptionInvoiceTrigger")
    public String triggerMonthlySubscriptionInvoice() {
        eventProducer.accept(List.of(new MonthlySubscriptionInvoiceTriggered()));
        return "Monthly subscription invoice triggered successfully";
    }
}
