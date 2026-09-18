package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static java.util.Comparator.comparingLong;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.service.user.UserService;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionInvoicePaymentUrlService {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private final UserService userService;
  private final StripeInvoiceService stripeInvoiceService;

  public Map<String, String> getPaymentUrlByInvoiceId(String userId, List<Invoice> invoices) {
    var unpaidInvoices =
        invoices.stream().filter(invoice -> invoice.paymentStatus() == UNPAID).toList();
    if (unpaidInvoices.isEmpty()) {
      return Map.of();
    }
    var stripeCustomerId = userService.getUserById(userId).getUserSubscriptionId();
    if (stripeCustomerId == null) {
      return Map.of();
    }
    var pool = new ArrayList<>(stripeInvoiceService.getUnpaidStripeInvoices(stripeCustomerId));
    if (pool.isEmpty()) {
      return Map.of();
    }
    return resolvePaymentUrls(unpaidInvoices, pool);
  }

  private Map<String, String> resolvePaymentUrls(
      List<Invoice> unpaidInvoices, List<com.stripe.model.Invoice> pool) {
    var candidates = amountMatchCandidatesSortedByPeriodDistance(unpaidInvoices, pool);
    var paymentUrlByInvoiceId = new HashMap<String, String>();
    var assignedInvoiceIds = new HashSet<String>();
    var assignedStripeIndexes = new HashSet<Integer>();
    for (var candidate : candidates) {
      if (assignedInvoiceIds.contains(candidate.invoice().getId())
          || assignedStripeIndexes.contains(candidate.stripeIndex())) {
        continue;
      }
      assignedInvoiceIds.add(candidate.invoice().getId());
      assignedStripeIndexes.add(candidate.stripeIndex());
      putIfHostedUrlPresent(
          paymentUrlByInvoiceId, candidate.invoice(), pool.get(candidate.stripeIndex()));
    }
    if (assignedInvoiceIds.isEmpty() && unpaidInvoices.size() == 1 && pool.size() == 1) {
      putIfHostedUrlPresent(paymentUrlByInvoiceId, unpaidInvoices.getFirst(), pool.getFirst());
    }
    return paymentUrlByInvoiceId;
  }

  private List<Candidate> amountMatchCandidatesSortedByPeriodDistance(
      List<Invoice> unpaidInvoices, List<com.stripe.model.Invoice> pool) {
    var candidates = new ArrayList<Candidate>();
    for (var invoice : unpaidInvoices) {
      var totalInCents = totalInCentsOf(invoice);
      var period = periodOf(invoice);
      for (var stripeIndex = 0; stripeIndex < pool.size(); stripeIndex++) {
        var stripeInvoice = pool.get(stripeIndex);
        if (Objects.equals(stripeInvoice.getTotal(), totalInCents)) {
          candidates.add(
              new Candidate(invoice, stripeIndex, periodDistanceInMonths(period, stripeInvoice)));
        }
      }
    }
    candidates.sort(
        comparingLong(Candidate::distance)
            .thenComparing(candidate -> periodOf(candidate.invoice()), nullsLast(naturalOrder()))
            .thenComparingInt(Candidate::stripeIndex));
    return candidates;
  }

  private void putIfHostedUrlPresent(
      Map<String, String> paymentUrlByInvoiceId,
      Invoice invoice,
      com.stripe.model.Invoice stripeInvoice) {
    if (stripeInvoice.getHostedInvoiceUrl() != null) {
      paymentUrlByInvoiceId.put(invoice.getId(), stripeInvoice.getHostedInvoiceUrl());
    }
  }

  private Long totalInCentsOf(Invoice invoice) {
    return invoice.getTotalPriceWithVat() == null
        ? null
        : Long.valueOf(invoice.getTotalPriceWithVat().getCentsRoundUp());
  }

  private YearMonth periodOf(Invoice invoice) {
    return invoice.getSendingDate() == null ? null : YearMonth.from(invoice.getSendingDate());
  }

  private long periodDistanceInMonths(
      YearMonth invoicePeriod, com.stripe.model.Invoice stripeInvoice) {
    if (invoicePeriod == null) {
      return Long.MAX_VALUE;
    }
    var epochSeconds =
        stripeInvoice.getPeriodEnd() != null && stripeInvoice.getPeriodEnd() > 0
            ? stripeInvoice.getPeriodEnd()
            : stripeInvoice.getCreated();
    if (epochSeconds == null) {
      return Long.MAX_VALUE;
    }
    var stripePeriod =
        YearMonth.from(Instant.ofEpochSecond(epochSeconds).atZone(PARIS).toLocalDate());
    return Math.abs(ChronoUnit.MONTHS.between(invoicePeriod, stripePeriod));
  }

  private record Candidate(Invoice invoice, int stripeIndex, long distance) {}
}
