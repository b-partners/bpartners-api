package app.bpartners.api.service.subscription;

import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.UpgradeLineKind;
import java.time.LocalDate;

public record UpgradeQuoteLine(
    UpgradeLineKind kind,
    String subscriptionProductId,
    BillingInterval billingInterval,
    LocalDate periodStart,
    LocalDate periodEnd,
    int days,
    int referencePeriodDays,
    long referencePriceInCentsWithVat,
    long amountInCentsWithVat) {}
