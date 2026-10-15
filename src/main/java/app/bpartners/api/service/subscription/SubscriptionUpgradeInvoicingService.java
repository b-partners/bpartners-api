package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceWithoutDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalDiscountAmount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalPriceWithVatAndDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalVatWithDiscount;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.service.subscription.SubscriptionInvoiceMailer.SUBSCRIPTION_INVOICE_MAIL_TEMPLATE;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.invoice.ReferenceGenerator;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeInvoicingService {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final int MAX_REFERENCE_ATTEMPTS = 120;

  private final UserSubscriptionUpgradeJpaRepository upgradeRepository;
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository;
  private final UserRepository userRepository;
  private final UserSubscriptionConf userSubscriptionConf;
  private final SubscriptionCustomerResolver subscriptionCustomerResolver;
  private final InvoiceService invoiceService;
  private final InvoiceRepository invoiceRepository;
  private final CustomDateFormatter customDateFormatter;
  private final SubscriptionUpgradeInvoiceLines subscriptionUpgradeInvoiceLines;
  private final SubscriptionInvoiceMailer subscriptionInvoiceMailer;
  private final ChargedFirstMonthResolver chargedFirstMonthResolver;
  private final LiveSubscriptionInvoicePeriodFinder livePeriodFinder;

  public Optional<UserSubscriptionUpgrade> invoice(String upgradeIdentifier) {
    var upgrade =
        upgradeRepository
            .findById(upgradeIdentifier)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "UserSubscriptionUpgrade.id=" + upgradeIdentifier + " not found"));
    if (upgrade.getStatus() != SubscriptionUpgradeStatus.PAID
        && upgrade.getStatus() != SubscriptionUpgradeStatus.INVOICED) {
      log.warn(
          "UserSubscriptionUpgrade(id={}) is {}, its invoices are not issued",
          upgrade.getId(),
          upgrade.getStatus());
      return Optional.empty();
    }
    if (upgrade.getCreditNoteInvoiceId() != null && upgrade.getNewInvoiceId() != null) {
      log.info("UserSubscriptionUpgrade(id={}) is already invoiced, skipping", upgrade.getId());
      return Optional.empty();
    }
    var fromProduct = productOf(upgrade.getFromSubscriptionProductId());
    var toProduct = productOf(upgrade.getToSubscriptionProductId());
    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());
    var userToDebit = userRepository.getById(upgrade.getUserId());
    var customerToDebit = subscriptionCustomerResolver.apply(userToCredit, userToDebit);

    var creditNote = Optional.<Invoice>empty();
    if (upgrade.getCreditNoteInvoiceId() == null) {
      creditNote =
          Optional.of(issueCreditNote(upgrade, fromProduct, userToCredit, customerToDebit));
      upgrade =
          upgradeRepository.save(
              upgrade.toBuilder().creditNoteInvoiceId(creditNote.get().getId()).build());
    }
    var newInvoice = Optional.<Invoice>empty();
    if (upgrade.getNewInvoiceId() == null) {
      newInvoice = Optional.of(issueNewInvoice(upgrade, toProduct, userToCredit, customerToDebit));
      upgrade =
          upgradeRepository.save(
              upgrade.toBuilder().newInvoiceId(newInvoice.get().getId()).build());
    }
    upgrade =
        upgradeRepository.save(
            upgrade.toBuilder().status(SubscriptionUpgradeStatus.INVOICED).build());
    var upgradeDate = dateOf(upgrade.getUpgradeDatetime());
    var creditNoteSummary =
        new SubscriptionMailSummary(
            fromProduct.getName(),
            upgrade.getFromBillingInterval(),
            upgradeDate,
            dateOf(upgrade.getOldCoverageEndDatetime()));
    var newInvoiceSummary =
        new SubscriptionMailSummary(
            toProduct.getName(),
            upgrade.getToBillingInterval(),
            upgradeDate,
            newCoverageEndOf(upgrade));
    creditNote.ifPresent(invoice -> notifySubscriber(invoice, creditNoteSummary));
    newInvoice.ifPresent(invoice -> notifySubscriber(invoice, newInvoiceSummary));
    return Optional.of(upgrade);
  }

  public LocalDate newCoverageEndOf(UserSubscriptionUpgrade upgrade) {
    var upgradeDate = dateOf(upgrade.getUpgradeDatetime());
    if (upgrade.getToBillingInterval() == YEARLY) {
      return upgradeDate.plusYears(1).minusDays(1);
    }
    return lastFullMonthEnd(dateOf(upgrade.getNewCommitmentEndDatetime()));
  }

  public static LocalDate lastFullMonthEnd(LocalDate commitmentEndExclusive) {
    var lastServedDay = commitmentEndExclusive.minusDays(1);
    return lastServedDay.getDayOfMonth() == lastServedDay.lengthOfMonth()
        ? lastServedDay
        : lastServedDay.withDayOfMonth(1).minusDays(1);
  }

  private Invoice issueCreditNote(
      UserSubscriptionUpgrade upgrade,
      SubscriptionProduct fromProduct,
      User userToCredit,
      Customer customerToDebit) {
    var from = dateOf(upgrade.getUpgradeDatetime());
    var to = dateOf(upgrade.getOldCoverageEndDatetime());
    var invoiceIdentifier = randomUUID().toString();
    var chargedFirstMonth =
        upgrade.getFromBillingInterval() == YEARLY
            ? Optional.<ChargedFirstMonth>empty()
            : chargedFirstMonthResolver.resolve(
                upgrade.getUserId(), from, upgradeCreationOf(upgrade));
    var lines =
        subscriptionUpgradeInvoiceLines.creditLines(
            invoiceIdentifier,
            fromProduct,
            upgrade.getFromBillingInterval(),
            from,
            to,
            chargedFirstMonth);
    var originalInvoiceReference = originalInvoiceReferenceOf(upgrade, to);
    var title =
        "Avoir sur l'abonnement "
            + fromProduct.getName()
            + " pour la période du "
            + customDateFormatter.formatFrenchDate(from)
            + " au "
            + customDateFormatter.formatFrenchDate(to)
            + originalInvoiceReference.map(reference -> " (facture " + reference + ")").orElse("");
    var created =
        invoiceService.crupdateSubscriptionInvoice(
            invoiceOf(
                userToCredit,
                customerToDebit,
                new InvoiceDraft(
                    invoiceIdentifier,
                    title,
                    lines,
                    upgrade.getFromBillingInterval() == YEARLY,
                    true,
                    from)));
    log.info(
        "Credit note Invoice(id={}, ref={}) issued for UserSubscriptionUpgrade(id={}) covering {}"
            + " to {}",
        created.getId(),
        created.getRef(),
        upgrade.getId(),
        from,
        to);
    return created;
  }

  private Invoice issueNewInvoice(
      UserSubscriptionUpgrade upgrade,
      SubscriptionProduct toProduct,
      User userToCredit,
      Customer customerToDebit) {
    var from = dateOf(upgrade.getUpgradeDatetime());
    var to = newCoverageEndOf(upgrade);
    var invoiceIdentifier = randomUUID().toString();
    var lines =
        subscriptionUpgradeInvoiceLines.newSubscriptionLines(
            invoiceIdentifier, toProduct, upgrade.getToBillingInterval(), from, to);
    var title =
        "Facture d'abonnement pour la période du "
            + customDateFormatter.formatFrenchDate(from)
            + " au "
            + customDateFormatter.formatFrenchDate(to);
    var created =
        invoiceService.crupdateSubscriptionInvoice(
            invoiceOf(
                userToCredit,
                customerToDebit,
                new InvoiceDraft(
                    invoiceIdentifier,
                    title,
                    lines,
                    upgrade.getToBillingInterval() == YEARLY,
                    false,
                    from)));
    subscriptionInvoicePeriodRepository.save(
        SubscriptionInvoicePeriod.builder()
            .id(randomUUID().toString())
            .userId(upgrade.getUserId())
            .invoiceId(created.getId())
            .periodStartDatetime(startOfDay(from))
            .periodEndDatetime(startOfDay(to))
            .build());
    log.info(
        "Invoice(id={}, ref={}) issued for UserSubscriptionUpgrade(id={}) covering {} to {}",
        created.getId(),
        created.getRef(),
        upgrade.getId(),
        from,
        to);
    return created;
  }

  private record InvoiceDraft(
      String invoiceIdentifier,
      String title,
      SubscriptionUpgradeInvoiceLines.Lines lines,
      boolean yearlyBilled,
      boolean creditNote,
      LocalDate upgradeDate) {}

  private Invoice invoiceOf(User userToCredit, Customer customerToDebit, InvoiceDraft draft) {
    var issuedAt = now();
    var referenceInstant = availableReferenceInstant(issuedAt, userToCredit.getId());
    var referenceGenerator =
        new ReferenceGenerator(() -> LocalDateTime.ofInstant(referenceInstant, PARIS));
    var discount = draft.lines().discountBasisPoints();
    var products = draft.lines().products();
    var yearlyBilled = draft.yearlyBilled();
    var settledAtOnce = draft.creditNote() || yearlyBilled;
    var sendingDate = issuedAt.atZone(PARIS).toLocalDate();
    return Invoice.builder()
        .id(draft.invoiceIdentifier())
        .ref(referenceGenerator.get())
        .title(draft.title())
        .subscriptionInvoice(true)
        .subscriptionBillingInterval(yearlyBilled ? YEARLY : MONTHLY)
        .creditNote(draft.creditNote())
        .status(settledAtOnce ? PAID : CONFIRMED)
        .archiveStatus(ArchiveStatus.ENABLED)
        .customer(customerToDebit)
        .toPayAt(settledAtOnce ? sendingDate : draft.upgradeDate())
        .sendingDate(sendingDate)
        .validityDate(null)
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .user(userToCredit)
        .paymentType(settledAtOnce ? PaymentTypeEnum.CASH : PaymentTypeEnum.IN_INSTALMENT)
        .paymentRegulations(new ArrayList<>())
        .products(products)
        .totalPriceWithoutDiscount(computePriceWithoutDiscount(products))
        .totalPriceWithoutVat(computePriceNoVatWithDiscount(discount, products))
        .totalVat(computeTotalVatWithDiscount(discount, products))
        .totalPriceWithVat(computeTotalPriceWithVatAndDiscount(discount, products))
        .delayInPaymentAllowed(0)
        .discount(
            InvoiceDiscount.builder()
                .percentValue(discount)
                .amountValue(computeTotalDiscountAmount(discount, products))
                .build())
        .createdAt(issuedAt)
        .delayPenaltyPercent(new Fraction(BigInteger.ZERO))
        .build();
  }

  private static Instant upgradeCreationOf(UserSubscriptionUpgrade upgrade) {
    return upgrade.effectiveDatetime() == null ? now() : upgrade.effectiveDatetime();
  }

  private Optional<String> originalInvoiceReferenceOf(
      UserSubscriptionUpgrade upgrade, LocalDate coverageEnd) {
    return livePeriodFinder.findLive(upgrade.getUserId(), upgradeCreationOf(upgrade)).stream()
        .filter(period -> period.getPeriodEndDatetime() != null)
        .filter(period -> !dateOf(period.getPeriodEndDatetime()).isAfter(coverageEnd))
        .max(comparing(SubscriptionInvoicePeriod::getPeriodEndDatetime))
        .map(period -> invoiceRepository.findById(period.getInvoiceId()))
        .map(Invoice::getRef);
  }

  private Instant availableReferenceInstant(Instant issuedAt, String userToCreditIdentifier) {
    var candidate = issuedAt;
    for (var attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
      var probed = candidate;
      var reference = new ReferenceGenerator(() -> LocalDateTime.ofInstant(probed, PARIS)).get();
      if (invoiceRepository.findByIdUserAndRef(userToCreditIdentifier, reference).isEmpty()) {
        return candidate;
      }
      candidate = candidate.plusSeconds(1);
    }
    throw new ApiException(
        SERVER_EXCEPTION,
        "No subscription invoice reference available within "
            + MAX_REFERENCE_ATTEMPTS
            + " seconds after "
            + issuedAt);
  }

  private void notifySubscriber(Invoice invoice, SubscriptionMailSummary summary) {
    try {
      subscriptionInvoiceMailer.sendWithSummary(
          invoice,
          summary,
          SUBSCRIPTION_INVOICE_MAIL_TEMPLATE,
          subscriptionInvoiceMailer.subscriberRecipientOf(invoice));
    } catch (RuntimeException e) {
      log.warn("Unable to send upgrade Invoice(id={}) by mail", invoice.getId(), e);
    }
  }

  private SubscriptionProduct productOf(String subscriptionProductIdentifier) {
    return subscriptionProductRepository
        .findById(subscriptionProductIdentifier)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SubscriptionProduct(id=" + subscriptionProductIdentifier + ") not found"));
  }

  private static LocalDate dateOf(Instant instant) {
    return instant.atZone(PARIS).toLocalDate();
  }

  private static Instant startOfDay(LocalDate date) {
    return date.atStartOfDay(PARIS).toInstant();
  }
}
