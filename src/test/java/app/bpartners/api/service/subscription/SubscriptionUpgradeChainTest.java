package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemporalUtils;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionUpgradeChainTest {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final String USER_ID = "subscriber_id";
  private static final String USER_TO_CREDIT_ID = "birdia_id";
  private static final String STRIPE_CUSTOMER_ID = "cus_subscriber";
  private static final String PAYMENT_METHOD_ID = "pm_default";
  private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
  private static final LocalDate COVERAGE_END = LocalDate.of(2027, 9, 30);

  CustomDateFormatter customDateFormatter = new CustomDateFormatter();
  Map<String, Invoice> invoices = new LinkedHashMap<>();
  List<SubscriptionInvoicePeriod> periods = new ArrayList<>();
  Map<String, UserSubscriptionUpgrade> upgrades = new LinkedHashMap<>();
  Map<String, SubscriptionProduct> catalog = new LinkedHashMap<>();

  SubscriptionService subscriptionService = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  StripeInvoiceService stripeInvoiceService = mock();
  StripeUpgradeService stripeUpgradeService = mock();
  SubscriptionUpgradeCommitmentService commitmentService = mock();
  SubscriptionInvoicePeriodRepository periodRepository = mock();
  SubscriptionPaymentRepository paymentRepository = mock();
  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  InvoiceRepository invoiceRepository = mock();
  InvoiceService invoiceService = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  UserRepository userRepository = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  SubscriptionInvoiceMailer subscriptionInvoiceMailer = mock();
  CreditGrantService creditGrantService = mock();
  EventProducer eventProducer = mock();
  TemporalUtils temporalUtils = mock();
  SubscriptionUpgradeTransitionService transitionService = mock();
  SubscriptionUpgradeExpirationService expirationService = mock();
  SubscriptionUpgradeTimeline timeline = new SubscriptionUpgradeTimeline(temporalUtils);

  LiveSubscriptionInvoicePeriodFinder livePeriodFinder =
      new LiveSubscriptionInvoicePeriodFinder(
          periodRepository, paymentRepository, upgradeRepository, invoiceRepository);
  ChargedFirstMonthResolver chargedFirstMonthResolver =
      new ChargedFirstMonthResolver(livePeriodFinder, invoiceRepository, customDateFormatter);

  SubscriptionUpgradeCompletionService completionService =
      new SubscriptionUpgradeCompletionService(
          subscriptionProductRepository,
          userRepository,
          stripeUpgradeService,
          subscriptionService,
          commitmentService,
          transitionService,
          upgradeRepository,
          timeline,
          creditGrantService,
          eventProducer);

  SubscriptionUpgradeService upgradeService =
      new SubscriptionUpgradeService(
          subscriptionService,
          userSubscriptionProductService,
          stripeInvoiceService,
          stripeUpgradeService,
          new SubscriptionUpgradeQuoteCalculator(),
          commitmentService,
          chargedFirstMonthResolver,
          upgradeRepository,
          periodRepository,
          paymentRepository,
          completionService,
          expirationService,
          timeline,
          temporalUtils);

  SubscriptionUpgradeInvoicingService invoicingService =
      new SubscriptionUpgradeInvoicingService(
          upgradeRepository,
          subscriptionProductRepository,
          periodRepository,
          userRepository,
          new UserSubscriptionConf(USER_TO_CREDIT_ID),
          subscriptionCustomerResolver,
          invoiceService,
          invoiceRepository,
          customDateFormatter,
          new SubscriptionUpgradeInvoiceLines(customDateFormatter),
          subscriptionInvoiceMailer,
          chargedFirstMonthResolver,
          livePeriodFinder);

  SubscriptionProduct essential = product("essential_id", "Essentiel", 1, 4_900L, 52_920L);
  SubscriptionProduct pro = product("pro_id", "Pro", 2, 9_900L, 106_920L);
  SubscriptionProduct expert = product("expert_id", "Expert", 3, 19_900L, 257_880L);
  User subscriber = User.builder().id(USER_ID).userSubscriptionId(STRIPE_CUSTOMER_ID).build();
  User birdia = User.builder().id(USER_TO_CREDIT_ID).build();
  List<Long> stripeCharges = new ArrayList<>();
  Invoice essentialInvoice;

  @BeforeEach
  void setUp() {
    catalog.put("essential_id", essential);
    catalog.put("pro_id", pro);
    catalog.put("expert_id", expert);
    when(temporalUtils.today()).thenReturn(TODAY);
    when(temporalUtils.startOfMonthAfter(TODAY)).thenReturn(LocalDate.of(2026, 11, 1));
    when(subscriptionService.getByPlanId(anyString(), any(BillingInterval.class)))
        .thenAnswer(
            invocation ->
                Subscription.builder()
                    .subscriptionProduct(catalog.get(invocation.getArgument(0, String.class)))
                    .build());
    when(subscriptionService.getSubscriptionByUser(subscriber))
        .thenReturn(
            UserSubscription.builder()
                .user(subscriber)
                .subscriptions(
                    List.of(
                        Subscription.builder()
                            .e2Id("sub_current")
                            .status(ACTIVE)
                            .startDatetime(Instant.parse("2026-10-14T09:22:00Z"))
                            .build()))
                .build());
    when(stripeInvoiceService.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID)).thenReturn(List.of());
    when(stripeUpgradeService.requireDefaultPaymentMethod(STRIPE_CUSTOMER_ID))
        .thenReturn(PAYMENT_METHOD_ID);
    when(stripeUpgradeService.chargeOneOffAmount(
            eq(STRIPE_CUSTOMER_ID), eq(PAYMENT_METHOD_ID), anyLong(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              stripeCharges.add(invocation.getArgument(2, Long.class));
              return new UpgradeCharge.Paid("in_" + stripeCharges.size());
            });
    when(stripeUpgradeService.createUpgradedSubscription(
            any(), any(), any(), any(), anyLong(), anyString()))
        .thenReturn("sub_new");
    when(commitmentService.findActiveCommitment(USER_ID)).thenReturn(Optional.empty());
    when(paymentRepository.findRefundedInvoiceIdsByUserId(USER_ID)).thenReturn(List.of());
    when(userRepository.getById(USER_TO_CREDIT_ID)).thenReturn(birdia);
    when(userRepository.getById(USER_ID)).thenReturn(subscriber);
    when(subscriptionCustomerResolver.apply(any(), any()))
        .thenReturn(Customer.builder().name("Birdia").build());
    when(subscriptionProductRepository.findById(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(catalog.get(invocation.getArgument(0))));

    when(periodRepository.findByUserId(USER_ID)).thenAnswer(invocation -> List.copyOf(periods));
    when(periodRepository.save(any(SubscriptionInvoicePeriod.class)))
        .thenAnswer(
            invocation -> {
              periods.add(invocation.getArgument(0));
              return invocation.getArgument(0);
            });
    when(upgradeRepository.save(any(UserSubscriptionUpgrade.class)))
        .thenAnswer(
            invocation -> {
              var saved = invocation.getArgument(0, UserSubscriptionUpgrade.class);
              upgrades.put(saved.getId(), saved);
              return saved;
            });
    when(upgradeRepository.findById(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(upgrades.get(invocation.getArgument(0))));
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenAnswer(
            invocation ->
                upgrades.values().stream()
                    .sorted((a, b) -> b.getCreationDatetime().compareTo(a.getCreationDatetime()))
                    .toList());
    when(invoiceService.crupdateSubscriptionInvoice(any(Invoice.class)))
        .thenAnswer(
            invocation -> {
              var invoice = invocation.getArgument(0, Invoice.class);
              invoices.put(invoice.getId(), invoice);
              return invoice;
            });
    when(invoiceRepository.findById(anyString()))
        .thenAnswer(invocation -> invoices.get(invocation.getArgument(0)));
    when(invoiceRepository.findByIdUserAndRef(anyString(), anyString()))
        .thenAnswer(
            invocation ->
                invoices.values().stream()
                    .filter(invoice -> invoice.getRef().equals(invocation.getArgument(1)))
                    .toList());

    essentialInvoice = essentialInvoiceOfTheFirstSubscription();
  }

  @Test
  void essential_then_monthly_pro_then_yearly_expert_credits_what_was_really_invoiced()
      throws InterruptedException {
    var toPro = upgradeTo("pro_id", null, essential, MONTHLY);
    invoicingService.invoice(toPro.getId());
    pause();
    var toExpert = upgradeTo("expert_id", YEARLY, pro, MONTHLY);
    invoicingService.invoice(toExpert.getId());

    assertEquals(List.of(3_566L, 250_982L), stripeCharges);

    var issued = issuedInvoicesInOrder();
    var essentialCreditNote = issued.get(0);
    var proInvoice = issued.get(1);
    var proCreditNote = issued.get(2);
    var expertInvoice = issued.get(3);

    assertEquals(-566.77, totalWithoutVatOf(essentialCreditNote), 0.005);
    assertEquals(-totalWithVatOf(essentialInvoice), totalWithVatOf(essentialCreditNote), 0.005);
    assertTrue(
        essentialCreditNote.getTitle().endsWith("(facture " + essentialInvoice.getRef() + ")"));

    assertEquals(1_146.48, totalWithoutVatOf(proInvoice), 0.005);
    assertEquals(57.48, firstLineOf(proInvoice), 0.005);

    assertEquals(-57.48, firstLineOf(proCreditNote), 0.005);
    assertEquals(-1_146.48, totalWithoutVatOf(proCreditNote), 0.005);
    assertEquals(-totalWithVatOf(proInvoice), totalWithVatOf(proCreditNote), 0.005);
    assertTrue(proCreditNote.getTitle().endsWith("(facture " + proInvoice.getRef() + ")"));
    assertTrue(proCreditNote.getTitle().startsWith("Avoir sur l'abonnement Pro"));

    assertEquals(2_578.80, totalWithVatOf(expertInvoice), 0.005);
    assertEquals(
        totalWithVatOf(expertInvoice) - Math.abs(firstLineOf(proCreditNote)) * 1.2,
        stripeCharges.get(1) / 100.0,
        0.01);
  }

  @Test
  void second_upgrade_credits_the_first_month_of_the_invoice_issued_by_the_first_upgrade()
      throws InterruptedException {
    var toPro = upgradeTo("pro_id", null, essential, MONTHLY);
    invoicingService.invoice(toPro.getId());
    pause();

    var toExpert = upgradeTo("expert_id", YEARLY, pro, MONTHLY);

    assertEquals(257_880L - 6_898L, toExpert.getProrationAmountInCentsWithVat());
  }

  private UserSubscriptionUpgrade upgradeTo(
      String planId,
      BillingInterval interval,
      SubscriptionProduct fromProduct,
      BillingInterval fromInterval)
      throws InterruptedException {
    pause();
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct(USER_ID))
        .thenReturn(
            Optional.of(
                UserSubscriptionProduct.builder()
                    .userId(USER_ID)
                    .subscriptionProduct(fromProduct)
                    .billingInterval(fromInterval)
                    .subscriptionStartDatetime(Instant.parse("2026-10-14T09:22:00Z"))
                    .build()));
    return upgradeService.upgrade(subscriber, planId, interval);
  }

  private List<Invoice> issuedInvoicesInOrder() {
    var captured = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService, times(4)).crupdateSubscriptionInvoice(captured.capture());
    return captured.getAllValues();
  }

  private Invoice essentialInvoiceOfTheFirstSubscription() {
    var products = new ArrayList<InvoiceProduct>();
    products.add(line("Essentiel", LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 31), 2_777L));
    var month = LocalDate.of(2026, 11, 1);
    while (!month.isAfter(COVERAGE_END)) {
      products.add(line("Essentiel", month, month.withDayOfMonth(month.lengthOfMonth()), 4_900L));
      month = month.plusMonths(1);
    }
    var invoice =
        Invoice.builder()
            .id("essential_invoice_id")
            .ref("REF-14102026112214")
            .user(birdia)
            .customer(Customer.builder().name("Birdia").build())
            .createdAt(Instant.now().minusSeconds(3_600))
            .products(products)
            .build();
    invoices.put(invoice.getId(), invoice);
    periods.add(
        SubscriptionInvoicePeriod.builder()
            .id("essential_period_id")
            .userId(USER_ID)
            .invoiceId(invoice.getId())
            .periodStartDatetime(TODAY.atStartOfDay(PARIS).toInstant())
            .periodEndDatetime(COVERAGE_END.atStartOfDay(PARIS).toInstant())
            .build());
    return invoice;
  }

  private InvoiceProduct line(String plan, LocalDate start, LocalDate end, long unitPriceInCents) {
    return InvoiceProduct.builder()
        .description(
            "Abonnement "
                + plan
                + " du "
                + customDateFormatter.formatFrenchDate(start)
                + " au "
                + customDateFormatter.formatFrenchDate(end))
        .quantity(1)
        .unitPrice(new Fraction(BigInteger.valueOf(unitPriceInCents)))
        .vatPercent(new Fraction(BigInteger.valueOf(2_000L)))
        .build();
  }

  private static double firstLineOf(Invoice invoice) {
    return invoice.getProducts().getFirst().getUnitPrice().getApproximatedValue() / 100.0;
  }

  private static double totalWithoutVatOf(Invoice invoice) {
    return invoice.getTotalPriceWithoutVat().getApproximatedValue() / 100.0;
  }

  private static double totalWithVatOf(Invoice invoice) {
    if (invoice.getTotalPriceWithVat() != null) {
      return invoice.getTotalPriceWithVat().getApproximatedValue() / 100.0;
    }
    return invoice.getProducts().stream()
            .mapToDouble(product -> product.getUnitPrice().getApproximatedValue())
            .sum()
        * 1.2
        / 100.0;
  }

  private static void pause() throws InterruptedException {
    Thread.sleep(5);
  }

  private static SubscriptionProduct product(
      String id,
      String name,
      int tierRank,
      long priceInCentsWithoutVat,
      long annualPriceInCentsWithVat) {
    return SubscriptionProduct.builder()
        .id(id)
        .name(name)
        .tierRank(tierRank)
        .vatPercent(2_000L)
        .priceInCentsWithoutVat(priceInCentsWithoutVat)
        .annualPriceInCentsWithVat(annualPriceInCentsWithVat)
        .build();
  }
}
