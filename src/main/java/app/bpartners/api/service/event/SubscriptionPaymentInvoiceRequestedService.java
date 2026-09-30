package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.ProductStatus.ENABLED;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceWithoutDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalDiscountAmount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalPriceWithVatAndDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalVatWithDiscount;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceCreated;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.subscription.AnnualInvoiceBillingType;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.invoice.ReferenceGenerator;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentInvoiceRequestedService
    implements Consumer<SubscriptionPaymentInvoiceRequested> {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final int MONTHS_PER_YEAR = 12;
  private static final int BASIS_POINTS = 10_000;
  private static final Comparator<UserSubscriptionCommitment> BY_RECENCY =
      comparing(SubscriptionPaymentInvoiceRequestedService::startedAt)
          .thenComparing(SubscriptionPaymentInvoiceRequestedService::recordedAt);
  static AnnualInvoiceBillingType annualInvoiceBillingType =
      AnnualInvoiceBillingType.MONTHLY_DETAILED;
  static boolean excludesAlreadyInvoicedPeriods = true;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository;
  private final SubscriptionPaymentService subscriptionPaymentService;
  private final UserRepository userRepository;
  private final UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentRepository;
  private final UserSubscriptionConf userSubscriptionConf;
  private final SubscriptionCustomerResolver subscriptionCustomerResolver;
  private final InvoiceService invoiceService;
  private final CustomDateFormatter customDateFormatter;
  private final EventProducer eventProducer;

  @Override
  public void accept(SubscriptionPaymentInvoiceRequested event) {
    var subscriptionPaymentIdentifier = event.getSubscriptionPaymentId();
    var optionalSubscriptionPayment =
        subscriptionPaymentRepository.findById(subscriptionPaymentIdentifier);
    if (optionalSubscriptionPayment.isEmpty()) {
      log.warn("No SubscriptionPayment.id={} to invoice, skipping", subscriptionPaymentIdentifier);
      return;
    }
    var subscriptionPayment = optionalSubscriptionPayment.get();
    if (subscriptionPayment.getInvoiceId() != null) {
      log.info(
          "SubscriptionPayment(id={}) is already invoiced by Invoice(id={}), skipping",
          subscriptionPayment.getId(),
          subscriptionPayment.getInvoiceId());
      return;
    }

    var monthlyBilling = monthlyBillingOf(subscriptionPayment);
    if (monthlyBilling.alreadyInvoiced()) {
      attachToCoveringInvoice(subscriptionPayment, monthlyBilling.coveringPeriod());
      return;
    }

    var billedMonths = monthlyBilling.instalments();
    var invoicedPeriod = invoicedPeriodOf(subscriptionPayment, billedMonths);
    var createdInvoice =
        issueSubscriptionInvoice(
            subscriptionPayment, billedMonths, invoicedPeriod, paidAt(subscriptionPayment));

    subscriptionPaymentService.invoicedBy(
        subscriptionPayment, createdInvoice.getId(), invoicedPeriod.start(), invoicedPeriod.end());

    notifySubscriber(createdInvoice, subscriptionPayment);
  }

  public Optional<Invoice> invoiceRemainingCommitmentPeriod(SubscriptionPayment referencePayment) {
    var monthlyBilling = monthlyBillingOf(referencePayment);
    if (monthlyBilling.alreadyInvoiced() || monthlyBilling.instalments().isEmpty()) {
      log.info(
          "User(id={}) has no commitment period left to invoice after SubscriptionPayment(id={}),"
              + " skipping",
          referencePayment.getUserId(),
          referencePayment.getId());
      return Optional.empty();
    }
    var billedMonths = monthlyBilling.instalments();
    var invoicedPeriod = invoicedPeriodOf(referencePayment, billedMonths);
    var createdInvoice =
        issueSubscriptionInvoice(referencePayment, billedMonths, invoicedPeriod, now());

    notifySubscriber(createdInvoice, referencePayment);

    return Optional.of(createdInvoice);
  }

  private Invoice issueSubscriptionInvoice(
      SubscriptionPayment subscriptionPayment,
      List<MonthSegment> billedMonths,
      InvoicedPeriod invoicedPeriod,
      Instant issuedAt) {
    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());
    var userToDebit = userRepository.getById(subscriptionPayment.getUserId());
    var customerToDebit = subscriptionCustomerResolver.apply(userToCredit, userToDebit);
    var createdInvoice =
        invoiceService.crupdateSubscriptionInvoice(
            computeSubscriptionInvoice(
                userToCredit, customerToDebit, subscriptionPayment, billedMonths, issuedAt));

    subscriptionInvoicePeriodRepository.save(
        SubscriptionInvoicePeriod.builder()
            .id(randomUUID().toString())
            .userId(subscriptionPayment.getUserId())
            .invoiceId(createdInvoice.getId())
            .periodStartDatetime(invoicedPeriod.start())
            .periodEndDatetime(invoicedPeriod.end())
            .build());

    log.info(
        "Invoice(id={}, ref={}) created for SubscriptionPayment(id={}) of User(id={}),"
            + " covering {} to {}",
        createdInvoice.getId(),
        createdInvoice.getRef(),
        subscriptionPayment.getId(),
        userToDebit.getId(),
        invoicedPeriod.start(),
        invoicedPeriod.end());

    return createdInvoice;
  }

  private void notifySubscriber(Invoice createdInvoice, SubscriptionPayment subscriptionPayment) {
    eventProducer.accept(
        List.of(
            SubscriptionPaymentInvoiceCreated.builder()
                .invoiceId(createdInvoice.getId())
                .subscriptionPaymentId(subscriptionPayment.getId())
                .build()));
  }

  private void attachToCoveringInvoice(
      SubscriptionPayment subscriptionPayment, SubscriptionInvoicePeriod coveringPeriod) {
    subscriptionPaymentService.invoicedBy(
        subscriptionPayment,
        coveringPeriod.getInvoiceId(),
        coveringPeriod.getPeriodStartDatetime(),
        coveringPeriod.getPeriodEndDatetime());
    log.info(
        "SubscriptionPayment(id={}) is already covered by Invoice(id={}) until {},"
            + " no subscription invoice issued nor sent",
        subscriptionPayment.getId(),
        coveringPeriod.getInvoiceId(),
        coveringPeriod.getPeriodEndDatetime());
  }

  private InvoicedPeriod invoicedPeriodOf(
      SubscriptionPayment subscriptionPayment, List<MonthSegment> billedMonths) {
    if (!billedMonths.isEmpty()) {
      return new InvoicedPeriod(
          startOfDay(billedMonths.getFirst().start()), startOfDay(billedMonths.getLast().end()));
    }
    var periodStart = billingPeriodStart(subscriptionPayment);
    return new InvoicedPeriod(
        startOfDay(periodStart), startOfDay(billingPeriodEnd(subscriptionPayment, periodStart)));
  }

  private Instant startOfDay(LocalDate date) {
    return date.atStartOfDay(PARIS).toInstant();
  }

  private Invoice computeSubscriptionInvoice(
      User userToCredit,
      Customer customerToDebit,
      SubscriptionPayment subscriptionPayment,
      List<MonthSegment> billedMonths,
      Instant issuedAt) {
    var invoiceIdentifier = randomUUID().toString();
    var sendingDate = issuedAt.atZone(PARIS).toLocalDate();
    var invoiceProducts =
        computeSubscriptionProducts(invoiceIdentifier, subscriptionPayment, billedMonths);
    var discount = annualDiscountFraction(subscriptionPayment);
    var referenceGenerator = new ReferenceGenerator(() -> LocalDateTime.ofInstant(issuedAt, PARIS));
    return Invoice.builder()
        .id(invoiceIdentifier)
        .ref(referenceGenerator.get())
        .title(titleOf(subscriptionPayment, issuedAt, billedMonths))
        .subscriptionInvoice(true)
        .subscriptionBillingInterval(subscriptionPayment.getBillingInterval())
        .status(billedMonths.isEmpty() ? PAID : CONFIRMED)
        .archiveStatus(ArchiveStatus.ENABLED)
        .customer(customerToDebit)
        .toPayAt(billedMonths.isEmpty() ? sendingDate : billedMonths.getFirst().start())
        .sendingDate(sendingDate)
        .validityDate(null)
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .user(userToCredit)
        .paymentType(billedMonths.isEmpty() ? PaymentTypeEnum.CASH : PaymentTypeEnum.IN_INSTALMENT)
        .paymentRegulations(new ArrayList<>())
        .products(invoiceProducts)
        .totalPriceWithoutDiscount(computePriceWithoutDiscount(invoiceProducts))
        .totalPriceWithoutVat(computePriceNoVatWithDiscount(discount, invoiceProducts))
        .totalVat(computeTotalVatWithDiscount(discount, invoiceProducts))
        .totalPriceWithVat(computeTotalPriceWithVatAndDiscount(discount, invoiceProducts))
        .delayInPaymentAllowed(0)
        .discount(
            InvoiceDiscount.builder()
                .percentValue(discount)
                .amountValue(computeTotalDiscountAmount(discount, invoiceProducts))
                .build())
        .createdAt(now())
        .delayPenaltyPercent(new Fraction(BigInteger.ZERO))
        .build();
  }

  private String titleOf(
      SubscriptionPayment subscriptionPayment, Instant paidAt, List<MonthSegment> billedMonths) {
    if (!billedMonths.isEmpty()) {
      return "Facture d'abonnement pour la période du "
          + customDateFormatter.formatFrenchDate(billedMonths.getFirst().start())
          + " au "
          + customDateFormatter.formatFrenchDate(billedMonths.getLast().end());
    }
    var billedPeriod = billedPeriodOf(subscriptionPayment);
    return billedPeriod == null
        ? "Facture d'abonnement du " + customDateFormatter.formatFrenchDate(paidAt)
        : "Facture d'abonnement " + billedPeriod;
  }

  private MonthlyBilling monthlyBillingOf(SubscriptionPayment subscriptionPayment) {
    if (subscriptionPayment.getBillingInterval() != BillingInterval.MONTHLY) {
      return MonthlyBilling.notInstalmentPayable();
    }
    var paymentPeriodStart = billingPeriodStart(subscriptionPayment);
    if (startsAfterItsPayment(subscriptionPayment)) {
      log.warn(
          "SubscriptionPayment(id={}) bills a period starting {} after being paid on {},"
              + " no instalment schedule computed from it",
          subscriptionPayment.getId(),
          paymentPeriodStart,
          paidAt(subscriptionPayment).atZone(PARIS).toLocalDate());
      return MonthlyBilling.notInstalmentPayable();
    }
    var commitment = latestCommitmentOf(subscriptionPayment, paymentPeriodStart);
    var commitmentEnd = commitmentEndOf(commitment, paymentPeriodStart);
    if (!excludesAlreadyInvoicedPeriods) {
      return MonthlyBilling.toInvoice(
          monthlyInstalments(commitmentStartOf(commitment, paymentPeriodStart), commitmentEnd));
    }
    var alreadyInvoicedPeriod = latestInvoicedPeriodOf(subscriptionPayment, commitmentEnd);
    if (alreadyInvoicedPeriod.isEmpty()) {
      return MonthlyBilling.toInvoice(monthlyInstalments(paymentPeriodStart, commitmentEnd));
    }
    var coveringPeriod = alreadyInvoicedPeriod.get();
    var firstUninvoicedDay = periodEndDateOf(coveringPeriod).plusDays(1);
    if (firstUninvoicedDay.isAfter(commitmentEnd)) {
      return MonthlyBilling.alreadyCoveredBy(coveringPeriod);
    }
    return MonthlyBilling.toInvoice(
        monthlyInstalments(
            firstUninvoicedDay.isAfter(paymentPeriodStart)
                ? firstUninvoicedDay
                : paymentPeriodStart,
            commitmentEnd));
  }

  private List<MonthSegment> monthlyInstalments(LocalDate start, LocalDate commitmentEnd) {
    var lastBilledMonthEnd =
        commitmentEnd.isBefore(start) ? start.withDayOfMonth(start.lengthOfMonth()) : commitmentEnd;
    return calendarMonthSegments(start, lastBilledMonthEnd);
  }

  private boolean startsAfterItsPayment(SubscriptionPayment subscriptionPayment) {
    var periodStart = subscriptionPayment.getPeriodStartDatetime();
    return periodStart != null
        && periodStart
            .atZone(PARIS)
            .toLocalDate()
            .isAfter(paidAt(subscriptionPayment).atZone(PARIS).toLocalDate());
  }

  private LocalDate commitmentStartOf(
      Optional<UserSubscriptionCommitment> commitment, LocalDate paymentPeriodStart) {
    return commitment
        .map(UserSubscriptionCommitment::getCommitmentStartDatetime)
        .filter(Objects::nonNull)
        .map(startDatetime -> startDatetime.atZone(PARIS).toLocalDate())
        .orElse(paymentPeriodStart);
  }

  private Optional<SubscriptionInvoicePeriod> latestInvoicedPeriodOf(
      SubscriptionPayment subscriptionPayment, LocalDate commitmentEnd) {
    var userIdentifier = subscriptionPayment.getUserId();
    var refundedInvoiceIds =
        Set.copyOf(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(userIdentifier));
    return subscriptionInvoicePeriodRepository.findByUserId(userIdentifier).stream()
        .filter(period -> !refundedInvoiceIds.contains(period.getInvoiceId()))
        .filter(period -> period.getPeriodEndDatetime() != null)
        .filter(period -> !periodEndDateOf(period).isAfter(commitmentEnd))
        .max(comparing(this::periodEndDateOf));
  }

  private LocalDate periodEndDateOf(SubscriptionInvoicePeriod invoicedPeriod) {
    return invoicedPeriod.getPeriodEndDatetime().atZone(PARIS).toLocalDate();
  }

  private Optional<UserSubscriptionCommitment> latestCommitmentOf(
      SubscriptionPayment subscriptionPayment, LocalDate paymentPeriodStart) {
    return userSubscriptionCommitmentRepository
        .findAllByUserId(subscriptionPayment.getUserId())
        .stream()
        .filter(commitment -> commitment.getCommitmentEndDatetime() != null)
        .filter(commitment -> endDateOf(commitment).isAfter(paymentPeriodStart))
        .max(BY_RECENCY);
  }

  private LocalDate commitmentEndOf(
      Optional<UserSubscriptionCommitment> commitment, LocalDate paymentPeriodStart) {
    return lastFullMonthEnd(
        commitment.map(this::endDateOf).orElseGet(() -> paymentPeriodStart.plusYears(1)));
  }

  private static Instant startedAt(UserSubscriptionCommitment commitment) {
    return commitment.getCommitmentStartDatetime() == null
        ? Instant.EPOCH
        : commitment.getCommitmentStartDatetime();
  }

  private static Instant recordedAt(UserSubscriptionCommitment commitment) {
    return commitment.getCreationDatetime() == null
        ? Instant.EPOCH
        : commitment.getCreationDatetime();
  }

  private LocalDate endDateOf(UserSubscriptionCommitment commitment) {
    return commitment.getCommitmentEndDatetime().atZone(PARIS).toLocalDate();
  }

  private LocalDate lastFullMonthEnd(LocalDate commitmentEndExclusive) {
    var lastServedDay = commitmentEndExclusive.minusDays(1);
    return lastServedDay.getDayOfMonth() == lastServedDay.lengthOfMonth()
        ? lastServedDay
        : lastServedDay.withDayOfMonth(1).minusDays(1);
  }

  private String billedPeriodOf(SubscriptionPayment subscriptionPayment) {
    var periodStart = subscriptionPayment.getPeriodStartDatetime();
    var periodEnd = subscriptionPayment.getPeriodEndDatetime();
    if (periodStart == null || periodEnd == null || startsAfterItsPayment(subscriptionPayment)) {
      return null;
    }
    return "pour la période du "
        + customDateFormatter.formatFrenchDate(periodStart)
        + " au "
        + customDateFormatter.formatFrenchDate(periodEnd);
  }

  private Instant paidAt(SubscriptionPayment subscriptionPayment) {
    return subscriptionPayment.getPaymentDatetime() == null
        ? now()
        : subscriptionPayment.getPaymentDatetime();
  }

  private List<InvoiceProduct> computeSubscriptionProducts(
      String invoiceIdentifier,
      SubscriptionPayment subscriptionPayment,
      List<MonthSegment> billedMonths) {
    if (isYearly(subscriptionPayment)) {
      return computeYearlySubscriptionProducts(invoiceIdentifier, subscriptionPayment);
    }
    if (!billedMonths.isEmpty()) {
      return computeMonthlyCommitmentProducts(invoiceIdentifier, subscriptionPayment, billedMonths);
    }
    var unitPrice =
        new Fraction(BigInteger.valueOf(subscriptionPayment.amountInCentsWithoutVatOrZero()));
    return List.of(
        invoiceProduct(
            invoiceIdentifier,
            subscriptionPayment.paymentLabel(),
            1,
            unitPrice,
            vatPercentOf(subscriptionPayment)));
  }

  private List<InvoiceProduct> computeMonthlyCommitmentProducts(
      String invoiceIdentifier,
      SubscriptionPayment subscriptionPayment,
      List<MonthSegment> billedMonths) {
    var fullMonthUnitPrice = monthlyCommitmentUnitPrice(subscriptionPayment);
    var vatPercent = vatPercentOf(subscriptionPayment);
    var products = new ArrayList<InvoiceProduct>();
    for (var index = 0; index < billedMonths.size(); index++) {
      var segment = billedMonths.get(index);
      products.add(
          invoiceProduct(
              invoiceIdentifier,
              subscriptionLineLabel(subscriptionPayment, segment.start(), segment.end()),
              1,
              monthlyInstalmentUnitPrice(
                  subscriptionPayment, segment, index == 0, fullMonthUnitPrice),
              vatPercent));
    }
    return products;
  }

  private Fraction monthlyInstalmentUnitPrice(
      SubscriptionPayment subscriptionPayment,
      MonthSegment segment,
      boolean firstInstalment,
      Fraction fullMonthUnitPrice) {
    if (segment.fullMonth()) {
      return fullMonthUnitPrice;
    }
    if (firstInstalment) {
      var chargedProrata = chargedProrataOf(subscriptionPayment, segment, fullMonthUnitPrice);
      if (chargedProrata != null) {
        return chargedProrata;
      }
    }
    return proratedOnMonthLength(
        fullMonthUnitPrice, segment.days(), segment.start().lengthOfMonth());
  }

  private Fraction chargedProrataOf(
      SubscriptionPayment subscriptionPayment, MonthSegment segment, Fraction fullMonthUnitPrice) {
    if (!segment.start().equals(billingPeriodStart(subscriptionPayment))) {
      return null;
    }
    var chargedInCents = subscriptionPayment.amountInCentsWithoutVatOrZero();
    if (chargedInCents <= 0L) {
      return null;
    }
    var chargedProrata = new Fraction(BigInteger.valueOf(chargedInCents));
    return chargedProrata.compareTo(fullMonthUnitPrice) >= 0 ? null : chargedProrata;
  }

  private Fraction monthlyCommitmentUnitPrice(SubscriptionPayment subscriptionPayment) {
    var listMonthlyInCents = monthlyListPriceInCents(subscriptionPayment);
    return listMonthlyInCents == null
        ? new Fraction(BigInteger.valueOf(subscriptionPayment.amountInCentsWithoutVatOrZero()))
        : new Fraction(listMonthlyInCents);
  }

  private Fraction proratedOnMonthLength(Fraction monthlyUnitPrice, int days, int monthLength) {
    return new Fraction(
        monthlyUnitPrice.getNumerator().multiply(BigInteger.valueOf(days)),
        monthlyUnitPrice.getDenominator().multiply(BigInteger.valueOf(monthLength)));
  }

  private List<InvoiceProduct> computeYearlySubscriptionProducts(
      String invoiceIdentifier, SubscriptionPayment subscriptionPayment) {
    var monthlyGrossUnitPrice = grossMonthlyUnitPrice(subscriptionPayment);
    var vatPercent = vatPercentOf(subscriptionPayment);
    if (annualInvoiceBillingType == AnnualInvoiceBillingType.MONTHLY_DETAILED) {
      return detailedMonthlyProducts(
          invoiceIdentifier, subscriptionPayment, monthlyGrossUnitPrice, vatPercent);
    }
    return List.of(
        invoiceProduct(
            invoiceIdentifier,
            groupedMonthlyDescription(subscriptionPayment),
            MONTHS_PER_YEAR,
            monthlyGrossUnitPrice,
            vatPercent));
  }

  private List<InvoiceProduct> detailedMonthlyProducts(
      String invoiceIdentifier,
      SubscriptionPayment subscriptionPayment,
      Fraction monthlyGrossUnitPrice,
      Fraction vatPercent) {
    var products = new ArrayList<InvoiceProduct>();
    var periodStart = billingPeriodStart(subscriptionPayment);
    var periodEnd = billingPeriodEnd(subscriptionPayment, periodStart);
    var segments = calendarMonthSegments(periodStart, periodEnd);
    var fullMonthUnitPrice = fullMonthUnitPrice(subscriptionPayment, monthlyGrossUnitPrice);
    var annualGrossTarget =
        monthlyGrossUnitPrice.operate(
            new Fraction(BigInteger.valueOf(MONTHS_PER_YEAR)), Aprational::multiply);
    var partialDaysTotal =
        segments.stream()
            .filter(segment -> !segment.fullMonth())
            .mapToInt(MonthSegment::days)
            .sum();
    var allocatedGross = new Fraction(BigInteger.ZERO);
    for (var index = 0; index < segments.size(); index++) {
      var segment = segments.get(index);
      var lastSegment = index == segments.size() - 1;
      var unitPrice =
          lastSegment
              ? annualGrossTarget.operate(allocatedGross, Aprational::subtract)
              : segment.fullMonth()
                  ? fullMonthUnitPrice
                  : proratedUnitPrice(fullMonthUnitPrice, segment.days(), partialDaysTotal);
      if (!lastSegment) {
        allocatedGross = allocatedGross.operate(unitPrice, Aprational::add);
      }
      products.add(
          invoiceProduct(
              invoiceIdentifier,
              subscriptionLineLabel(subscriptionPayment, segment.start(), segment.end()),
              1,
              unitPrice,
              vatPercent));
    }
    return products;
  }

  private Fraction fullMonthUnitPrice(
      SubscriptionPayment subscriptionPayment, Fraction monthlyGrossUnitPrice) {
    var listMonthlyInCents = monthlyListPriceInCents(subscriptionPayment);
    return listMonthlyInCents == null ? monthlyGrossUnitPrice : new Fraction(listMonthlyInCents);
  }

  private List<MonthSegment> calendarMonthSegments(LocalDate periodStart, LocalDate periodEnd) {
    var segments = new ArrayList<MonthSegment>();
    var cursor = periodStart;
    while (!cursor.isAfter(periodEnd)) {
      var monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth());
      var segmentEnd = monthEnd.isAfter(periodEnd) ? periodEnd : monthEnd;
      var fullMonth = cursor.getDayOfMonth() == 1 && segmentEnd.equals(monthEnd);
      var days = (int) ChronoUnit.DAYS.between(cursor, segmentEnd) + 1;
      segments.add(new MonthSegment(cursor, segmentEnd, fullMonth, days));
      cursor = monthEnd.plusDays(1);
    }
    return segments;
  }

  private Fraction proratedUnitPrice(
      Fraction monthlyGrossUnitPrice, int days, int partialDaysTotal) {
    if (partialDaysTotal <= 0) {
      return monthlyGrossUnitPrice;
    }
    return new Fraction(
        monthlyGrossUnitPrice.getNumerator().multiply(BigInteger.valueOf(days)),
        monthlyGrossUnitPrice.getDenominator().multiply(BigInteger.valueOf(partialDaysTotal)));
  }

  private String subscriptionLineLabel(
      SubscriptionPayment subscriptionPayment, LocalDate start, LocalDate end) {
    return subscriptionLabelPrefix(subscriptionPayment)
        + " du "
        + customDateFormatter.formatFrenchDate(start)
        + " au "
        + customDateFormatter.formatFrenchDate(end);
  }

  private String subscriptionLabelPrefix(SubscriptionPayment subscriptionPayment) {
    var subscriptionProduct = subscriptionPayment.getSubscriptionProduct();
    var planName = subscriptionProduct == null ? null : subscriptionProduct.getName();
    return planName == null || planName.isBlank() ? "Abonnement" : "Abonnement " + planName;
  }

  private InvoiceProduct invoiceProduct(
      String invoiceIdentifier,
      String description,
      int quantity,
      Fraction unitPrice,
      Fraction vatPercent) {
    return InvoiceProduct.builder()
        .id(randomUUID().toString())
        .idInvoice(invoiceIdentifier)
        .createdAt(now())
        .description(description)
        .quantity(quantity)
        .unitPrice(unitPrice)
        .vatPercent(vatPercent)
        .status(ENABLED)
        .build();
  }

  private String groupedMonthlyDescription(SubscriptionPayment subscriptionPayment) {
    var periodStart = subscriptionPayment.getPeriodStartDatetime();
    var periodEnd = subscriptionPayment.getPeriodEndDatetime();
    if (periodStart == null || periodEnd == null) {
      return subscriptionLabelPrefix(subscriptionPayment);
    }
    return subscriptionLabelPrefix(subscriptionPayment)
        + " du "
        + customDateFormatter.formatFrenchDate(periodStart)
        + " au "
        + customDateFormatter.formatFrenchDate(periodEnd);
  }

  private Fraction grossMonthlyUnitPrice(SubscriptionPayment subscriptionPayment) {
    return annualLinePricing(subscriptionPayment).monthlyUnitPrice();
  }

  private Fraction annualDiscountFraction(SubscriptionPayment subscriptionPayment) {
    if (!isYearly(subscriptionPayment)) {
      return new Fraction(BigInteger.ZERO);
    }
    return annualLinePricing(subscriptionPayment).discount();
  }

  private AnnualLinePricing annualLinePricing(SubscriptionPayment subscriptionPayment) {
    var netAnnualInCents = BigInteger.valueOf(subscriptionPayment.amountInCentsWithoutVatOrZero());
    var declaredDiscountBasisPoints = annualDiscountBasisPoints(subscriptionPayment);
    if (declaredDiscountBasisPoints > 0) {
      return reconstructedPricing(netAnnualInCents, declaredDiscountBasisPoints);
    }
    var catalogPricing = catalogPricing(subscriptionPayment, netAnnualInCents);
    if (catalogPricing != null) {
      return catalogPricing;
    }
    return reconstructedPricing(netAnnualInCents, 0);
  }

  private AnnualLinePricing reconstructedPricing(
      BigInteger netAnnualInCents, int discountBasisPoints) {
    var monthlyUnitPrice =
        new Fraction(
            netAnnualInCents.multiply(BigInteger.valueOf(BASIS_POINTS)),
            BigInteger.valueOf((long) (BASIS_POINTS - discountBasisPoints) * MONTHS_PER_YEAR));
    return new AnnualLinePricing(
        monthlyUnitPrice, new Fraction(BigInteger.valueOf(discountBasisPoints)));
  }

  private AnnualLinePricing catalogPricing(
      SubscriptionPayment subscriptionPayment, BigInteger netAnnualInCents) {
    var listMonthlyInCents = monthlyListPriceInCents(subscriptionPayment);
    if (listMonthlyInCents == null) {
      return null;
    }
    var grossAnnualInCents = listMonthlyInCents.multiply(BigInteger.valueOf(MONTHS_PER_YEAR));
    if (grossAnnualInCents.compareTo(netAnnualInCents) <= 0) {
      return null;
    }
    var discount =
        new Fraction(
            BigInteger.valueOf(BASIS_POINTS)
                .multiply(grossAnnualInCents.subtract(netAnnualInCents)),
            grossAnnualInCents);
    return new AnnualLinePricing(new Fraction(listMonthlyInCents), discount);
  }

  private BigInteger monthlyListPriceInCents(SubscriptionPayment subscriptionPayment) {
    var subscriptionProduct = subscriptionPayment.getSubscriptionProduct();
    if (subscriptionProduct == null
        || subscriptionProduct.getPriceInCentsWithoutVat() == null
        || subscriptionProduct.getPriceInCentsWithoutVat() <= 0) {
      return null;
    }
    return BigInteger.valueOf(subscriptionProduct.getPriceInCentsWithoutVat());
  }

  private int annualDiscountBasisPoints(SubscriptionPayment subscriptionPayment) {
    var subscriptionProduct = subscriptionPayment.getSubscriptionProduct();
    if (subscriptionProduct == null || subscriptionProduct.getAnnualDiscountPercent() == null) {
      return 0;
    }
    var basisPoints = subscriptionProduct.getAnnualDiscountPercent();
    if (basisPoints <= 0 || basisPoints >= BASIS_POINTS) {
      return 0;
    }
    return basisPoints;
  }

  private Fraction vatPercentOf(SubscriptionPayment subscriptionPayment) {
    return new Fraction(BigInteger.valueOf(subscriptionPayment.vatPercentOrZero()));
  }

  private LocalDate billingPeriodStart(SubscriptionPayment subscriptionPayment) {
    var periodStart = subscriptionPayment.getPeriodStartDatetime();
    var instant = periodStart == null ? paidAt(subscriptionPayment) : periodStart;
    return instant.atZone(PARIS).toLocalDate();
  }

  private LocalDate billingPeriodEnd(
      SubscriptionPayment subscriptionPayment, LocalDate periodStart) {
    var periodEnd = subscriptionPayment.getPeriodEndDatetime();
    if (periodEnd == null) {
      return periodStart.plusYears(1).minusDays(1);
    }
    return periodEnd.atZone(PARIS).toLocalDate();
  }

  private boolean isYearly(SubscriptionPayment subscriptionPayment) {
    return subscriptionPayment.getBillingInterval() == BillingInterval.YEARLY;
  }

  private record AnnualLinePricing(Fraction monthlyUnitPrice, Fraction discount) {}

  private record InvoicedPeriod(Instant start, Instant end) {}

  private record MonthlyBilling(
      List<MonthSegment> instalments, SubscriptionInvoicePeriod coveringPeriod) {
    private static MonthlyBilling notInstalmentPayable() {
      return new MonthlyBilling(List.of(), null);
    }

    private static MonthlyBilling toInvoice(List<MonthSegment> instalments) {
      return new MonthlyBilling(instalments, null);
    }

    private static MonthlyBilling alreadyCoveredBy(SubscriptionInvoicePeriod coveringPeriod) {
      return new MonthlyBilling(List.of(), coveringPeriod);
    }

    private boolean alreadyInvoiced() {
      return coveringPeriod != null;
    }
  }

  private record MonthSegment(LocalDate start, LocalDate end, boolean fullMonth, int days) {}
}
