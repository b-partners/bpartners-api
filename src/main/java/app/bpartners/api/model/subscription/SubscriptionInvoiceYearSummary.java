package app.bpartners.api.model.subscription;

import app.bpartners.api.model.Invoice;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public record SubscriptionInvoiceYearSummary(
    int year, List<Integer> availableYears, Map<YearMonth, List<Invoice>> invoicesByMonth) {}
