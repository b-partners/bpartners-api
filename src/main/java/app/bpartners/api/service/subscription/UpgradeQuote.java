package app.bpartners.api.service.subscription;

import java.time.LocalDate;
import java.util.List;

public record UpgradeQuote(
    long newChargeInCentsWithVat,
    long creditedInCentsWithVat,
    long amountDueInCentsWithVat,
    LocalDate paidPeriodStart,
    LocalDate paidPeriodEnd,
    long paidAmountInCentsWithVat,
    List<UpgradeQuoteLine> lines) {}
