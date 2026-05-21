package app.bpartners.api.service.customer;

public record CustomerExportPayload(
    String name, String email, String stripeCustomerId, boolean unknown) {}
