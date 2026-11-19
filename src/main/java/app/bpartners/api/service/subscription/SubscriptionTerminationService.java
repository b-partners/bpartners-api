package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.CREDIT_NOTE_ISSUED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.FAILED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.SKIPPED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.SUBSCRIPTION_ENDED;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionTermination;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionTerminationJpaRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.subscription.SubscriptionUpgradeInvoicingService.CreditNoteDraft;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionTerminationService {
  private static final Pattern LINE_PERIOD =
      Pattern.compile("du (\\d{2}/\\d{2}/\\d{4}) au (\\d{2}/\\d{2}/\\d{4})$");
  private static final DateTimeFormatter FRENCH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final SubscriptionTerminationJpaRepository repository;
  private final UserRepository userRepository;
  private final UserSubscriptionProductService userSubscriptionProductService;
  private final LiveSubscriptionInvoicePeriodFinder livePeriodFinder;
  private final InvoiceRepository invoiceRepository;
  private final UserSubscriptionConf userSubscriptionConf;
  private final SubscriptionCustomerResolver subscriptionCustomerResolver;
  private final SubscriptionUpgradeInvoiceLines subscriptionUpgradeInvoiceLines;
  private final SubscriptionUpgradeInvoicingService subscriptionUpgradeInvoicingService;
  private final CustomDateFormatter customDateFormatter;

  public record BilledLine(
      Invoice invoice, InvoiceProduct product, LocalDate start, LocalDate end) {}

  public void terminate(String terminationId) {
    var termination =
        repository
            .findById(terminationId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "SubscriptionTermination.id=" + terminationId + " not found"));
    if (termination.getStatus() == CREDIT_NOTE_ISSUED || termination.getStatus() == SKIPPED) {
      log.info(
          "SubscriptionTermination(id={}) is already {}, skipping",
          terminationId,
          termination.getStatus());
      return;
    }
    try {
      process(termination);
    } catch (RuntimeException e) {
      log.error("SubscriptionTermination(id={}) failed", terminationId, e);
      repository.save(
          repository.findById(terminationId).orElse(termination).toBuilder()
              .status(FAILED)
              .errorMessage(String.valueOf(e.getMessage()))
              .updateDatetime(now())
              .build());
    }
  }

  private void process(SubscriptionTermination termination) {
    var user = userOf(termination);
    userSubscriptionProductService.endActiveSubscriptionProducts(
        user.getId(), termination.getSubscriptionEndDatetime());
    var ended =
        repository.save(
            termination.toBuilder()
                .userId(user.getId())
                .status(SUBSCRIPTION_ENDED)
                .errorMessage(null)
                .updateDatetime(now())
                .build());

    var billedLines = billedLinesOf(user.getId());
    if (ended.getAmountInCentsWithVat() > 0) {
      checkBilledAmount(ended, billedLines);
    }
    var linesToCredit = linesToCredit(ended.getCurrentPeriodEndDate(), billedLines);
    if (linesToCredit.isEmpty()) {
      repository.save(ended.toBuilder().status(SKIPPED).updateDatetime(now()).build());
      log.info(
          "User(id={}) has no billed month after the termination, no credit note", user.getId());
      return;
    }

    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());
    var customerToDebit = subscriptionCustomerResolver.apply(userToCredit, user);
    var creditNote =
        subscriptionUpgradeInvoicingService.issueCreditNoteInvoice(
            creditNoteDraftOf(linesToCredit), userToCredit, customerToDebit);
    repository.save(
        ended.toBuilder()
            .status(CREDIT_NOTE_ISSUED)
            .creditNoteInvoiceId(creditNote.getId())
            .updateDatetime(now())
            .build());
    log.info(
        "Credit note Invoice(id={}, ref={}) issued for User(id={}) without any mail",
        creditNote.getId(),
        creditNote.getRef(),
        user.getId());
  }

  public void terminateCancelledSubscription(
      String stripeSubscriptionId,
      User user,
      Instant subscriptionEndDatetime,
      LocalDate currentPeriodEndDate) {
    if (repository.findById(stripeSubscriptionId).isEmpty()) {
      repository.save(
          SubscriptionTermination.builder()
              .id(stripeSubscriptionId)
              .userSubscriptionE2Id(stripeSubscriptionId)
              .userId(user.getId())
              .subscriptionEndDatetime(subscriptionEndDatetime)
              .currentPeriodEndDate(currentPeriodEndDate)
              .amountInCentsWithVat(0)
              .status(PENDING)
              .updateDatetime(now())
              .build());
    }
    terminate(stripeSubscriptionId);
  }

  private User userOf(SubscriptionTermination termination) {
    if (termination.getUserId() != null) {
      return userRepository.getById(termination.getUserId());
    }
    return userRepository
        .findByStripeCustomerId(termination.getUserSubscriptionE2Id())
        .orElseThrow(
            () ->
                new NotFoundException(
                    "User with user_subscription_e2_id="
                        + termination.getUserSubscriptionE2Id()
                        + " not found"));
  }

  private CreditNoteDraft creditNoteDraftOf(List<BilledLine> linesToCredit) {
    var invoiceIdentifier = randomUUID().toString();
    var lines =
        subscriptionUpgradeInvoiceLines.creditLinesOf(
            invoiceIdentifier, linesToCredit.stream().map(BilledLine::product).toList());
    var from = linesToCredit.getFirst().start();
    var to = linesToCredit.getLast().end();
    var references =
        linesToCredit.stream()
            .map(line -> line.invoice().getRef())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    var title =
        "Avoir sur l'abonnement pour la période du "
            + customDateFormatter.formatFrenchDate(from)
            + " au "
            + customDateFormatter.formatFrenchDate(to)
            + (references.isEmpty() ? "" : " (facture " + String.join(", ", references) + ")");
    var yearlyBilled =
        linesToCredit.getFirst().invoice().getSubscriptionBillingInterval() == YEARLY;
    return new CreditNoteDraft(invoiceIdentifier, title, lines, yearlyBilled, from);
  }

  List<BilledLine> billedLinesOf(String userId) {
    return livePeriodFinder.findLive(userId, now()).stream()
        .map(period -> period.getInvoiceId())
        .distinct()
        .map(invoiceRepository::findById)
        .filter(Objects::nonNull)
        .filter(invoice -> invoice.getProducts() != null)
        .flatMap(
            invoice ->
                invoice.getProducts().stream()
                    .map(product -> billedLineOf(invoice, product))
                    .flatMap(Optional::stream))
        .sorted(comparing(BilledLine::start))
        .toList();
  }

  static Optional<BilledLine> billedLineOf(Invoice invoice, InvoiceProduct product) {
    if (product.getDescription() == null) {
      return Optional.empty();
    }
    var matcher = LINE_PERIOD.matcher(product.getDescription());
    if (!matcher.find()) {
      return Optional.empty();
    }
    return Optional.of(
        new BilledLine(
            invoice,
            product,
            LocalDate.parse(matcher.group(1), FRENCH_DATE),
            LocalDate.parse(matcher.group(2), FRENCH_DATE)));
  }

  static void checkBilledAmount(SubscriptionTermination termination, List<BilledLine> billedLines) {
    var periodEnd = termination.getCurrentPeriodEndDate();
    var covering =
        billedLines.stream()
            .filter(line -> !line.start().isAfter(periodEnd) && !line.end().isBefore(periodEnd))
            .toList();
    if (covering.isEmpty()) {
      throw new IllegalStateException(
          "No invoiced line covers the current period end " + periodEnd);
    }
    var matching =
        covering.stream()
            .anyMatch(
                line ->
                    amountInCentsWithVatOf(line.product())
                        == termination.getAmountInCentsWithVat());
    if (!matching) {
      throw new IllegalStateException(
          "Invoiced amount of the line covering "
              + periodEnd
              + " does not match the expected "
              + termination.getAmountInCentsWithVat()
              + " cents with VAT");
    }
  }

  static List<BilledLine> linesToCredit(LocalDate currentPeriodEnd, List<BilledLine> billedLines) {
    var firstCreditedDay = currentPeriodEnd.withDayOfMonth(1).plusMonths(1);
    var toCredit =
        billedLines.stream().filter(line -> !line.start().isBefore(firstCreditedDay)).toList();
    toCredit.forEach(
        line -> {
          var discount = line.invoice().getDiscount();
          if (discount != null
              && discount.getPercentValue() != null
              && discount.getPercentValue().getNumerator().signum() != 0) {
            throw new IllegalStateException(
                "Invoice(ref=" + line.invoice().getRef() + ") has a discount, not supported");
          }
        });
    return toCredit;
  }

  static long amountInCentsWithVatOf(InvoiceProduct product) {
    var unitPrice =
        product.getVatPercent() == null ? product.getUnitPrice() : product.getUnitPriceWithVat();
    return toDecimal(unitPrice.getNumerator(), unitPrice.getDenominator())
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private static BigDecimal toDecimal(BigInteger numerator, BigInteger denominator) {
    return new BigDecimal(numerator).divide(new BigDecimal(denominator), 6, RoundingMode.HALF_UP);
  }
}
