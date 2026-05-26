package app.bpartners.api.service.customer;

import java.time.Instant;

public record CustomerExportPayload(
    String internalCustomerName,
    String email,
    String stripeCustomerId,
    String stripeCustomerName,
    boolean unknown,
    Instant stripeCreationDatetime) {}
