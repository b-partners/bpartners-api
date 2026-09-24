package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.ProductStatus.ENABLED;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceWithoutDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalDiscountAmount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalPriceWithVatAndDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalVatWithDiscount;
import static java.time.Instant.now;
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
import app.bpartners.api.model.subscription.AnnualInvoiceBillingType;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentInvoiceRequestedService
    implements Consumer<SubscriptionPaymentInvoiceRequested> {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final int MONTHS_PER_YEAR = 12;
  private static final int BASIS_POINTS = 10_000;
  static AnnualInvoiceBillingType annualInvoiceBillingType =
      AnnualInvoiceBillingType.MONTHLY_DETAILED;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final SubscriptionPaymentService subscriptionPaymentService;
  private final UserRepository userRepository;
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

    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());
    var userToDebit = userRepository.getById(subscriptionPayment.getUserId());
    var customerToDebit = subscriptionCustomerResolver.apply(userToCredit, userToDebit);

    var createdInvoice =
        invoiceService.crupdateSubscriptionInvoice(
            computeSubscriptionInvoice(userToCredit, customerToDebit, subscriptionPayment));
    subscriptionPaymentService.invoicedBy(subscriptionPayment, createdInvoice.getId());
    log.info(
        "Invoice(id={}, ref={}) created for SubscriptionPayment(id={}) of User(id={})",
        createdInvoice.getId(),
        createdInvoice.getRef(),
        subscriptionPayment.getId(),
        userToDebit.getId());
    eventProducer.accept(
        List.of(
            SubscriptionPaymentInvoiceCreated.builder()
                .invoiceId(createdInvoice.getId())
                .subscriptionPaymentId(subscriptionPayment.getId())
                .build()));
  }

  private Invoice computeSubscriptionInvoice(
      User userToCredit, Customer customerToDebit, SubscriptionPayment subscriptionPayment) {
    var invoiceIdentifier = randomUUID().toString();
    var paidAt = paidAt(subscriptionPayment);
    var sendingDate = paidAt.atZone(PARIS).toLocalDate();
    var invoiceProducts = computeSubscriptionProducts(invoiceIdentifier, subscriptionPayment);
    var discount = annualDiscountFraction(subscriptionPayment);
    var referenceGenerator = new ReferenceGenerator(() -> LocalDateTime.ofInstant(paidAt, PARIS));
    return Invoice.builder()
        .id(invoiceIdentifier)
        .ref(referenceGenerator.get())
        .title(titleOf(subscriptionPayment, paidAt))
        .subscriptionInvoice(true)
        .status(PAID)
        .archiveStatus(ArchiveStatus.ENABLED)
        .customer(customerToDebit)
        .toPayAt(sendingDate)
        .sendingDate(sendingDate)
        .validityDate(null)
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .user(userToCredit)
        .paymentType(PaymentTypeEnum.CASH)
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

  private String titleOf(SubscriptionPayment subscriptionPayment, Instant paidAt) {
    var billedPeriod = billedPeriodOf(subscriptionPayment);
    return billedPeriod == null
        ? "Facture d'abonnement du " + customDateFormatter.formatFrenchDate(paidAt)
        : "Facture d'abonnement " + billedPeriod;
  }

  private String billedPeriodOf(SubscriptionPayment subscriptionPayment) {
    var periodStart = subscriptionPayment.getPeriodStartDatetime();
    var periodEnd = subscriptionPayment.getPeriodEndDatetime();
    if (periodStart == null || periodEnd == null) {
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
      String invoiceIdentifier, SubscriptionPayment subscriptionPayment) {
    if (isYearly(subscriptionPayment)) {
      return computeYearlySubscriptionProducts(invoiceIdentifier, subscriptionPayment);
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
    for (int month = 0; month < MONTHS_PER_YEAR; month++) {
      var monthStart = periodStart.plusMonths(month);
      var monthEnd = monthStart.plusMonths(1).minusDays(1);
      var description =
          "Abonnement mensuel du "
              + customDateFormatter.formatFrenchDate(monthStart)
              + " au "
              + customDateFormatter.formatFrenchDate(monthEnd);
      products.add(
          invoiceProduct(invoiceIdentifier, description, 1, monthlyGrossUnitPrice, vatPercent));
    }
    return products;
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
      return "Abonnement mensuel";
    }
    return "Abonnement mensuel du "
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

  private boolean isYearly(SubscriptionPayment subscriptionPayment) {
    return subscriptionPayment.getBillingInterval() == BillingInterval.YEARLY;
  }

  private record AnnualLinePricing(Fraction monthlyUnitPrice, Fraction discount) {}
}
