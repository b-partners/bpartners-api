package app.bpartners.api.service.subscription;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChargedFirstMonthResolver {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  private final SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final InvoiceRepository invoiceRepository;
  private final CustomDateFormatter customDateFormatter;

  public Optional<ChargedFirstMonth> resolve(String userId, LocalDate upgradeDate) {
    var refundedInvoiceIds =
        Set.copyOf(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(userId));
    return subscriptionInvoicePeriodRepository.findByUserId(userId).stream()
        .filter(period -> !refundedInvoiceIds.contains(period.getInvoiceId()))
        .filter(period -> period.getPeriodStartDatetime() != null)
        .filter(period -> isPartialMonthCovering(startOf(period), upgradeDate))
        .sorted(
            comparing(SubscriptionInvoicePeriod::getPeriodStartDatetime)
                .thenComparing(
                    SubscriptionInvoicePeriod::getCreationDatetime, nullsLast(Instant::compareTo)))
        .map(this::chargedFirstMonthOf)
        .flatMap(Optional::stream)
        .findFirst();
  }

  private Optional<ChargedFirstMonth> chargedFirstMonthOf(SubscriptionInvoicePeriod period) {
    var invoice = invoiceRepository.findById(period.getInvoiceId());
    if (invoice == null || invoice.getProducts() == null) {
      return Optional.empty();
    }
    var start = startOf(period);
    var end = start.withDayOfMonth(start.lengthOfMonth());
    return firstMonthLineOf(invoice, start, end)
        .filter(line -> line.getUnitPrice().getNumerator().signum() > 0)
        .map(
            line ->
                new ChargedFirstMonth(
                    start,
                    end,
                    line.getUnitPrice(),
                    line.getVatPercent() == null
                        ? new Fraction(BigInteger.ZERO)
                        : line.getVatPercent()));
  }

  private Optional<InvoiceProduct> firstMonthLineOf(
      Invoice invoice, LocalDate start, LocalDate end) {
    var periodSuffix =
        " du "
            + customDateFormatter.formatFrenchDate(start)
            + " au "
            + customDateFormatter.formatFrenchDate(end);
    return invoice.getProducts().stream()
        .filter(
            line -> line.getDescription() != null && line.getDescription().endsWith(periodSuffix))
        .findFirst();
  }

  private static boolean isPartialMonthCovering(LocalDate periodStart, LocalDate upgradeDate) {
    var monthEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
    return periodStart.getDayOfMonth() != 1
        && !upgradeDate.isBefore(periodStart)
        && !upgradeDate.isAfter(monthEnd);
  }

  private static LocalDate startOf(SubscriptionInvoicePeriod period) {
    return period.getPeriodStartDatetime().atZone(PARIS).toLocalDate();
  }
}
