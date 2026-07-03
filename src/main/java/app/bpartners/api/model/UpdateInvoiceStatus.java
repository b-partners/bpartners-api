package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;

public record UpdateInvoiceStatus(
    String invoiceIdentifier,
    String invoiceReference,
    InvoiceStatus status,
    String userOwnerIdentifier) {}
