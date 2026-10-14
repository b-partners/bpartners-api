package app.bpartners.api.service.subscription;

import app.bpartners.api.model.subscription.BillingInterval;
import java.time.LocalDate;

public record SubscriptionMailSummary(
    String planName, BillingInterval billingInterval, LocalDate periodStart, LocalDate periodEnd) {}
