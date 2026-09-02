package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionProduct.DEFAULT_VAT_PERCENT;
import static app.bpartners.api.model.subscription.SubscriptionProduct.priceInCentsWithoutVatFrom;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochSecond;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceRequested;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.service.utils.CustomDateFormatter;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionPaymentService {
  private static final String STRIPE_YEARLY_INTERVAL = "year";
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final UserRepository userRepository;
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final EventProducer eventProducer;
  private final CustomDateFormatter customDateFormatter;

  public Optional<SubscriptionPayment> recordPaidStripeInvoice(Invoice stripeInvoice) {
    if (stripeInvoice.getSubscription() == null) {
      log.info(
          "Stripe Invoice(id={}) is not attached to a subscription, no subscription invoice to"
              + " generate",
          stripeInvoice.getId());
      return Optional.empty();
    }
    var alreadyRecorded =
        subscriptionPaymentRepository.findByStripeInvoiceId(stripeInvoice.getId());
    if (alreadyRecorded.isPresent()) {
      return Optional.of(requestInvoiceIfStillMissing(alreadyRecorded.get()));
    }
    var optionalUser = userRepository.findByStripeCustomerId(stripeInvoice.getCustomer());
    if (optionalUser.isEmpty()) {
      log.warn(
          "No user found for Stripe customer id={}, Stripe Invoice(id={}) not invoiced",
          stripeInvoice.getCustomer(),
          stripeInvoice.getId());
      return Optional.empty();
    }
    var amountInCentsWithVat = amountInCentsWithVatOf(stripeInvoice);
    if (amountInCentsWithVat <= 0L) {
      log.info(
          "Stripe Invoice(id={}) charges nothing, no subscription invoice to generate",
          stripeInvoice.getId());
      return Optional.empty();
    }
    var userId = optionalUser.get().getId();
    var activeSubscription =
        userSubscriptionProductService.findActiveUserSubscriptionProduct(userId).orElse(null);
    var saved =
        subscriptionPaymentRepository.save(
            paidSubscriptionPayment(
                stripeInvoice, userId, activeSubscription, amountInCentsWithVat));
    log.info(
        "SubscriptionPayment(id={}) recorded for User(id={}) from Stripe Invoice(id={})",
        saved.getId(),
        userId,
        stripeInvoice.getId());
    requestInvoice(saved);
    return Optional.of(saved);
  }

  public SubscriptionPayment invoicedBy(SubscriptionPayment subscriptionPayment, String invoiceId) {
    return subscriptionPaymentRepository.save(
        subscriptionPayment.toBuilder().invoiceId(invoiceId).build());
  }

  private SubscriptionPayment requestInvoiceIfStillMissing(SubscriptionPayment alreadyRecorded) {
    if (alreadyRecorded.getInvoiceId() != null) {
      log.info(
          "Stripe Invoice(id={}) is already invoiced by Invoice(id={}), skipping",
          alreadyRecorded.getStripeInvoiceId(),
          alreadyRecorded.getInvoiceId());
      return alreadyRecorded;
    }
    requestInvoice(alreadyRecorded);
    return alreadyRecorded;
  }

  private void requestInvoice(SubscriptionPayment subscriptionPayment) {
    eventProducer.accept(
        List.of(
            SubscriptionPaymentInvoiceRequested.builder()
                .subscriptionPaymentId(subscriptionPayment.getId())
                .build()));
    log.info(
        "Requested subscription invoice for SubscriptionPayment(id={}, stripeInvoiceId={})",
        subscriptionPayment.getId(),
        subscriptionPayment.getStripeInvoiceId());
  }

  private SubscriptionPayment paidSubscriptionPayment(
      Invoice stripeInvoice,
      String userId,
      UserSubscriptionProduct activeSubscription,
      long amountInCentsWithVat) {
    var resolvedPlan = resolvePlanFromStripe(stripeInvoice);
    var subscriptionProduct =
        resolvedPlan == null ? subscriptionProductOf(activeSubscription) : resolvedPlan.product();
    var billingInterval =
        resolvedPlan == null || resolvedPlan.billingInterval() == null
            ? billingIntervalOf(activeSubscription)
            : resolvedPlan.billingInterval();
    var vatPercent = vatPercentOf(stripeInvoice, subscriptionProduct);
    var billedPeriod = billedPeriodOf(stripeInvoice);
    return SubscriptionPayment.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .stripeInvoiceId(stripeInvoice.getId())
        .stripeSubscriptionId(stripeInvoice.getSubscription())
        .subscriptionProduct(subscriptionProduct)
        .billingInterval(billingInterval)
        .label(labelOf(billedPeriod, subscriptionProduct))
        .amountInCentsWithoutVat(
            amountInCentsWithoutVatOf(stripeInvoice, amountInCentsWithVat, vatPercent))
        .amountInCentsWithVat(amountInCentsWithVat)
        .vatPercent(vatPercent)
        .periodStartDatetime(epochSecondOrNull(billedPeriod.start()))
        .periodEndDatetime(epochSecondOrNull(billedPeriod.end()))
        .paymentDatetime(paidAtOf(stripeInvoice))
        .creationDatetime(now())
        .build();
  }

  private BilledPeriod billedPeriodOf(Invoice stripeInvoice) {
    var subscriptionLinePeriod = subscriptionLinePeriodOf(stripeInvoice);
    if (subscriptionLinePeriod != null) {
      return new BilledPeriod(subscriptionLinePeriod.getStart(), subscriptionLinePeriod.getEnd());
    }
    return new BilledPeriod(stripeInvoice.getPeriodStart(), stripeInvoice.getPeriodEnd());
  }

  private InvoiceLineItem.Period subscriptionLinePeriodOf(Invoice stripeInvoice) {
    var lines = stripeInvoice.getLines() == null ? null : stripeInvoice.getLines().getData();
    if (lines == null || lines.isEmpty()) {
      return null;
    }
    return lines.stream()
        .filter(line -> line.getPeriod() != null && line.getPeriod().getEnd() != null)
        .max(Comparator.comparingLong(line -> line.getPeriod().getEnd()))
        .map(InvoiceLineItem::getPeriod)
        .orElse(null);
  }

  private ResolvedPlan resolvePlanFromStripe(Invoice stripeInvoice) {
    var lines = stripeInvoice.getLines() == null ? null : stripeInvoice.getLines().getData();
    if (lines == null) {
      return null;
    }
    for (var line : lines) {
      var productId = stripeProductIdOf(line);
      if (productId == null) {
        continue;
      }
      var plan =
          subscriptionProductRepository
              .findByE2Id(productId)
              .filter(product -> product.getBillingType() != null)
              .orElse(null);
      if (plan != null) {
        return new ResolvedPlan(plan, billingIntervalOf(line));
      }
    }
    return null;
  }

  private String stripeProductIdOf(InvoiceLineItem line) {
    if (line.getPlan() != null && line.getPlan().getProduct() != null) {
      return line.getPlan().getProduct();
    }
    return line.getPrice() == null ? null : line.getPrice().getProduct();
  }

  private BillingInterval billingIntervalOf(InvoiceLineItem line) {
    var interval = stripeRecurringIntervalOf(line);
    if (interval == null) {
      return null;
    }
    return STRIPE_YEARLY_INTERVAL.equals(interval)
        ? BillingInterval.YEARLY
        : BillingInterval.MONTHLY;
  }

  private String stripeRecurringIntervalOf(InvoiceLineItem line) {
    if (line.getPrice() != null
        && line.getPrice().getRecurring() != null
        && line.getPrice().getRecurring().getInterval() != null) {
      return line.getPrice().getRecurring().getInterval();
    }
    return line.getPlan() == null ? null : line.getPlan().getInterval();
  }

  private SubscriptionProduct subscriptionProductOf(UserSubscriptionProduct activeSubscription) {
    return activeSubscription == null ? null : activeSubscription.getSubscriptionProduct();
  }

  private BillingInterval billingIntervalOf(UserSubscriptionProduct activeSubscription) {
    return activeSubscription == null ? null : activeSubscription.getBillingInterval();
  }

  private long amountInCentsWithVatOf(Invoice stripeInvoice) {
    if (stripeInvoice.getTotal() != null) {
      return stripeInvoice.getTotal();
    }
    return stripeInvoice.getAmountPaid() == null ? 0L : stripeInvoice.getAmountPaid();
  }

  private long amountInCentsWithoutVatOf(
      Invoice stripeInvoice, long amountInCentsWithVat, long vatPercent) {
    if (isTaxedByStripe(stripeInvoice) && stripeInvoice.getTotalExcludingTax() != null) {
      return stripeInvoice.getTotalExcludingTax();
    }
    var withoutVat = priceInCentsWithoutVatFrom(amountInCentsWithVat, vatPercent);
    return withoutVat == null ? amountInCentsWithVat : withoutVat;
  }

  private long vatPercentOf(Invoice stripeInvoice, SubscriptionProduct subscriptionProduct) {
    if (isTaxedByStripe(stripeInvoice)) {
      var totalExcludingTax = stripeInvoice.getTotalExcludingTax();
      if (totalExcludingTax != null && totalExcludingTax > 0L) {
        return (stripeInvoice.getTax() * 10_000L + totalExcludingTax / 2) / totalExcludingTax;
      }
    }
    return subscriptionProduct == null || subscriptionProduct.getVatPercent() == null
        ? DEFAULT_VAT_PERCENT
        : subscriptionProduct.getVatPercent();
  }

  private boolean isTaxedByStripe(Invoice stripeInvoice) {
    return stripeInvoice.getTax() != null && stripeInvoice.getTax() > 0L;
  }

  private String labelOf(BilledPeriod period, SubscriptionProduct subscriptionProduct) {
    var base =
        subscriptionProduct == null || subscriptionProduct.getName() == null
            ? "Abonnement"
            : "Abonnement " + subscriptionProduct.getName();
    var periodLabel = periodLabelOf(period);
    return periodLabel == null ? base : base + " " + periodLabel;
  }

  private String periodLabelOf(BilledPeriod period) {
    if (period.start() == null || period.end() == null) {
      return null;
    }
    return "du "
        + customDateFormatter.formatFrenchDate(ofEpochSecond(period.start()))
        + " au "
        + customDateFormatter.formatFrenchDate(ofEpochSecond(period.end()));
  }

  private Instant paidAtOf(Invoice stripeInvoice) {
    var statusTransitions = stripeInvoice.getStatusTransitions();
    var paidAt = statusTransitions == null ? null : statusTransitions.getPaidAt();
    return paidAt == null ? now() : Instant.ofEpochSecond(paidAt);
  }

  private Instant epochSecondOrNull(Long epochSecond) {
    return epochSecond == null ? null : Instant.ofEpochSecond(epochSecond);
  }
}
