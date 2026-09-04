package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static app.bpartners.api.model.mapper.InvoiceMapper.*;
import static app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT;
import static app.bpartners.api.model.subscription.SubscriptionProduct.DEFAULT_FREE_USAGE_THRESHOLD;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.*;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.payment.StripeConf;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.invoice.ReferenceGenerator;
import app.bpartners.api.service.subscription.StripeFactory;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionInvoiceTitleComputer;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.exception.StripeException;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlySubscriptionInvoiceRequestedService
    implements Consumer<MonthlySubscriptionInvoiceRequested> {
  private static final int VAT_PERCENT = 2000; // basis points of 10000 : 2000 = 20%
  private final InvoiceService invoiceService;
  private final SubscriptionService subscriptionService;
  private final CustomDateFormatter customDateFormatter;
  private final TemporalUtils temporalUtils;
  private final UserSubscriptionEligibleJpaRepository subscriptionEligibleJpaRepository;
  private final SubscriptionCustomerResolver subscriptionCustomerResolver;
  private final StripeConf stripeConf;
  private final StripeFactory stripeFactory;
  private final StripeInvoiceService stripeInvoiceService;
  private final SubscriptionInvoiceTitleComputer subscriptionInvoiceTitleComputer;
  private final SubscriptionProductRepository subscriptionProductRepository;

  @Override
  public void accept(MonthlySubscriptionInvoiceRequested event) {
    var userToCredit = event.getUserToCredit();
    var userToDebit = event.getUserToAttemptDebit();
    if (!isSubscribed(userToDebit)) {
      return;
    }

    var userSubscription = subscriptionService.getSubscriptionByUser(userToDebit);
    var upcomingStripeInvoice =
        stripeInvoiceService.getUpcomingStripeInvoice(userToDebit.getUserSubscriptionId());
    var nextInvoiceDate =
        upcomingStripeInvoice == null
            ? null
            : Instant.ofEpochSecond(upcomingStripeInvoice.getNextPaymentAttempt());

    log.info("Upcoming Stripe Invoice {} for user(id={})", nextInvoiceDate, userToDebit.getId());

    if (nextInvoiceDate != null
        && nextInvoiceDate.isBefore(temporalUtils.getFirstOfMonthAt2359(now(), 1))) {
      Invoice monthlySubscriptionInvoice;
      try {
        monthlySubscriptionInvoice =
            computeMonthlySusbcriptionInvoice(userToCredit, userToDebit, userSubscription);
      } catch (StripeException e) {
        throw new RuntimeException(e);
      }
      var existingComputedInvoices =
          invoiceService.getInvoices(
              userToCredit.getId(),
              new PageFromOne(MIN_PAGE),
              new BoundedPageSize(MAX_SIZE),
              List.of(CONFIRMED, PAID),
              ArchiveStatus.ENABLED,
              monthlySubscriptionInvoice.getTitle(),
              List.of(userToDebit.getName()));
      if (isCustomerToDebitAlreadyHasComputedInvoice(
          existingComputedInvoices, monthlySubscriptionInvoice)) {
        log.info("Subscription Invoice already computed for user(id={})", userToDebit.getId());
      } else {
        var createdInvoice = invoiceService.crupdateSubscriptionInvoice(monthlySubscriptionInvoice);
        log.info(
            "Invoice(ref={}, customer(id={})) created",
            createdInvoice.getRef(),
            createdInvoice.getCustomer().getId());
        /*
        TODO : uncomment to triggered mail sent
        eventProducer.accept(
            List.of(
                MonthlySubscriptionInvoiceCreated.builder()
                    .invoiceId(createdInvoice.getId())
                    .build()));
        */
      }
    } else {
      log.info(
          "User(id={}) does not have upcoming stripe invoice, skip computing invoice",
          userToDebit.getId());
    }
  }

  private boolean isSubscribed(User user) {
    var userSubscriptionId = user.getUserSubscriptionId();
    if (userSubscriptionId == null) {
      return false;
    }
    var optionalUserSubscriptionEligible =
        subscriptionEligibleJpaRepository.findByUserId(user.getId());
    var subscription = subscriptionService.getSubscriptionByUser(user);
    if (optionalUserSubscriptionEligible.isEmpty()) {
      return false;
    }
    var userSubscriptionEligible = optionalUserSubscriptionEligible.get();
    return subscription.hasValidSubscription()
        && !userSubscriptionEligible.hasFreeTrialPeriodActive();
  }

  private boolean isCustomerToDebitAlreadyHasComputedInvoice(
      List<Invoice> existingComputedInvoices, Invoice monthlySubscriptionInvoice) {
    return existingComputedInvoices.stream()
        .anyMatch(
            existingInvoice ->
                existingInvoice
                        .getCustomer()
                        .getName()
                        .equalsIgnoreCase(monthlySubscriptionInvoice.getCustomer().getName())
                    && existingInvoice
                        .getTitle()
                        .equalsIgnoreCase(monthlySubscriptionInvoice.getTitle())
                    && existingInvoice
                        .getCreatedAt()
                        .isBefore(temporalUtils.getFirstOfMonthAt2359(now(), 1))
                    && existingInvoice
                        .getCreatedAt()
                        .isAfter(
                            temporalUtils
                                .startOfActualMonth()
                                .atStartOfDay(ZoneId.of("Europe/Paris"))
                                .toInstant()));
  }

  private Invoice computeMonthlySusbcriptionInvoice(
      User userToCredit, User userToDebit, UserSubscription userSubscription)
      throws StripeException {
    var customerToDebit = subscriptionCustomerResolver.apply(userToCredit, userToDebit);
    var variableAnalysisConsumptionUsage = getVariableAnalysisConsumptionUsage(userToDebit);

    var invoiceId = randomUUID().toString();
    var billedMonth = YearMonth.from(temporalUtils.startOfLastMonth());
    var monthPeriod = subscriptionInvoiceTitleComputer.monthPeriodOf(billedMonth);
    var invoiceTitle = subscriptionInvoiceTitleComputer.apply(billedMonth);
    var defaultProductDescription = "Abonnement Essentiel " + monthPeriod;
    var invoiceProducts =
        computeSubscriptionProducts(
            invoiceId,
            defaultProductDescription,
            userSubscription,
            variableAnalysisConsumptionUsage);
    var discountZero = new Fraction(BigInteger.ZERO);
    var sendingDate = temporalUtils.endOfLastMonth();
    LocalDateTime fixedDateTime = LocalDateTime.of(sendingDate, LocalTime.now());
    Supplier<LocalDateTime> fixedDateTimeSupplier = () -> fixedDateTime;
    var referenceGenerator = new ReferenceGenerator(fixedDateTimeSupplier);
    return Invoice.builder()
        .id(invoiceId)
        .ref(referenceGenerator.get())
        .title(invoiceTitle)
        .subscriptionInvoice(true)
        .status(CONFIRMED)
        .archiveStatus(ArchiveStatus.ENABLED)
        .customer(customerToDebit)
        .toPayAt(temporalUtils.startOfActualMonth())
        .sendingDate(sendingDate)
        .validityDate(sendingDate.plusDays(30L))
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .user(userToCredit)
        .paymentType(app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH)
        .paymentRegulations(new ArrayList<>())
        .products(invoiceProducts)
        .totalPriceWithoutDiscount(computePriceWithoutDiscount(invoiceProducts))
        .totalPriceWithoutVat(computePriceNoVatWithDiscount(discountZero, invoiceProducts))
        .totalVat(computeTotalVatWithDiscount(discountZero, invoiceProducts))
        .totalPriceWithVat(computeTotalPriceWithVatAndDiscount(discountZero, invoiceProducts))
        .delayInPaymentAllowed(0)
        .discount(InvoiceDiscount.builder().percentValue(new Fraction(BigInteger.ZERO)).build())
        .createdAt(now())
        .delayPenaltyPercent(new Fraction(BigInteger.ZERO))
        .build();
  }

  private long getVariableAnalysisConsumptionUsage(User userToDebit) {
    var consumptionUsageSummaries =
        subscriptionService.computeMonthlySubscriptionVariableConsumption(userToDebit);
    var variableConsumptionUsage = new AtomicLong(0);
    consumptionUsageSummaries.forEach(
        consumptionUsageSummary -> {
          if (consumptionUsageSummary
              .consumptionType()
              .equals(SubscriptionConsumptionType.ROOF_ANALYSIS)) {
            variableConsumptionUsage.set(consumptionUsageSummary.usage());
          }
        });
    return variableConsumptionUsage.get();
  }

  private long vatPercentOf(SubscriptionProduct subscriptionProduct) {
    return subscriptionProduct == null || subscriptionProduct.getVatPercent() == null
        ? VAT_PERCENT
        : subscriptionProduct.getVatPercent();
  }

  private long getProductUnitPrice(
      Optional<SubscriptionProduct> commitmentPlan, List<String> productIds) {
    return commitmentPlan
        .filter(plan -> plan.getPriceInCentsWithoutVat() != null)
        .map(SubscriptionProduct::getPriceInCentsWithoutVat)
        .orElseGet(
            () ->
                productIds.stream()
                        .anyMatch(
                            productId ->
                                productId.contains(stripeConf.getBasicSubscriptionProductId()))
                    ? 700L
                    : 4900L);
  }

  private Optional<SubscriptionProduct> findCommitmentPlan(List<String> stripeProductIds) {
    return stripeProductIds.stream()
        .map(subscriptionProductRepository::findByE2Id)
        .flatMap(Optional::stream)
        .filter(plan -> COMMITMENT.equals(plan.getBillingType()))
        .findFirst();
  }

  private long freeUsageThresholdOf(Optional<SubscriptionProduct> commitmentPlan) {
    return commitmentPlan
        .map(SubscriptionProduct::freeUsageThresholdOrDefault)
        .orElse(DEFAULT_FREE_USAGE_THRESHOLD);
  }

  private long overageUnitPriceOf(Optional<SubscriptionProduct> commitmentPlan) {
    return commitmentPlan
        .map(SubscriptionProduct::overageUnitPriceInCentsOrDefault)
        .orElse(SubscriptionProduct.DEFAULT_OVERAGE_UNIT_PRICE_IN_CENTS);
  }

  private @NotNull ArrayList<InvoiceProduct> computeSubscriptionProducts(
      String invoiceId,
      String invoiceTitle,
      UserSubscription userSubscription,
      Long variableAnalysisConsumptionUsage)
      throws StripeException {
    var invoiceProducts = new ArrayList<InvoiceProduct>();
    var latestSubscription = userSubscription.getLatestSubscription();

    var user = userSubscription.getUser();
    var subscriptions =
        stripeFactory.retrieveUserSubscriptions(user).stream()
            .flatMap(subscription -> subscription.getItems().getData().stream())
            .map(subscriptionItem -> subscriptionItem.getPlan().getProduct())
            .toList();
    var commitmentPlan = findCommitmentPlan(subscriptions);
    var subscriptionProduct = latestSubscription.getSubscriptionProduct();
    invoiceProducts.add(
        InvoiceProduct.builder()
            .id(randomUUID().toString())
            .idInvoice(invoiceId)
            .createdAt(now())
            .description(subscriptionProduct == null ? invoiceTitle : subscriptionProduct.getName())
            .quantity(1)
            .unitPrice(
                new Fraction(
                    BigInteger.valueOf(getProductUnitPrice(commitmentPlan, subscriptions))))
            .vatPercent(new Fraction(BigInteger.valueOf(vatPercentOf(subscriptionProduct))))
            .status(ProductStatus.ENABLED)
            .build());

    var analysisPayableUsage =
        variableAnalysisConsumptionUsage - freeUsageThresholdOf(commitmentPlan);
    if (analysisPayableUsage > 0L) {
      invoiceProducts.add(
          InvoiceProduct.builder()
              .id(randomUUID().toString())
              .idInvoice(invoiceId)
              .createdAt(now())
              .description("Analyse de toîtures supplémentaire")
              .quantity((int) analysisPayableUsage)
              .unitPrice(new Fraction(BigInteger.valueOf(overageUnitPriceOf(commitmentPlan))))
              .vatPercent(new Fraction(BigInteger.valueOf(vatPercentOf(subscriptionProduct))))
              .status(ProductStatus.ENABLED)
              .build());
    }
    return invoiceProducts;
  }
}
