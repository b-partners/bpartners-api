package app.bpartners.api.service.subscription;

public record UpgradeQuote(
    long newChargeInCentsWithVat, long creditedInCentsWithVat, long amountDueInCentsWithVat) {}
