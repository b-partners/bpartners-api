package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.PENDING;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION_REVERSAL;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT;
import static app.bpartners.api.service.subscription.SubscriptionBillingStatsService.OVERAGE_PRODUCT_DESCRIPTION;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditPurchaseOrigin;
import app.bpartners.api.model.credit.CreditPurchaseStatus;
import app.bpartners.api.model.credit.CreditPurchaseType;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionMovementType;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.OverageInvoiceLine;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionBillingRepositoriesIT extends MockedThirdParties {
  private static final Instant IN_RANGE = Instant.parse("2024-06-15T12:00:00Z");
  private static final Instant OUT_OF_RANGE = Instant.parse("2024-05-15T12:00:00Z");
  private static final Instant FROM = Instant.parse("2024-06-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2024-07-01T00:00:00Z");
  private static final String SYSTEM_USER = JOE_DOE_ID;

  @Autowired private SubscriptionPaymentRepository subscriptionPaymentRepository;
  @Autowired private CreditPurchaseRepository creditPurchaseRepository;
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private InvoiceJpaRepository invoiceJpaRepository;
  @Autowired private UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  @Autowired private SubscriptionProductRepository subscriptionProductRepository;

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpUserSubscription(subscriptionService);
    subscriptionPaymentRepository.deleteAll();
    creditPurchaseRepository.deleteAll();
    creditTransactionRepository.deleteAll();
  }

  @Test
  void findInvoicedBetween_returns_only_invoiced_payments_in_range() {
    persistPayment(1000L, IN_RANGE, "inv-1");
    persistPayment(2000L, IN_RANGE, "inv-2");
    persistPayment(9999L, IN_RANGE, null);
    persistPayment(3000L, OUT_OF_RANGE, "inv-3");

    var rows = subscriptionPaymentRepository.findInvoicedBetween(FROM, TO);

    assertEquals(2, rows.size());
    assertEquals(
        true,
        rows.stream()
            .allMatch(p -> p.getInvoiceId() != null && p.getPaymentDatetime().equals(IN_RANGE)));
  }

  @Test
  void findByStatusBetween_returns_only_completed_purchases_in_range() {
    persistPurchase(COMPLETED, IN_RANGE);
    persistPurchase(COMPLETED, IN_RANGE);
    persistPurchase(PENDING, IN_RANGE);
    persistPurchase(COMPLETED, OUT_OF_RANGE);

    var rows = creditPurchaseRepository.findByStatusBetween(COMPLETED, FROM, TO);

    assertEquals(2, rows.size());
    assertEquals(
        true,
        rows.stream()
            .allMatch(
                p -> p.getStatus() == COMPLETED && p.getCompletionDatetime().equals(IN_RANGE)));
  }

  @Test
  void findByTypesBetween_filters_by_type_and_range() {
    persistTransaction(CONSUMPTION, DEBIT, 8L, IN_RANGE);
    persistTransaction(CONSUMPTION_REVERSAL, CREDIT, 3L, IN_RANGE);
    persistTransaction(PURCHASE, CREDIT, 50L, IN_RANGE);
    persistTransaction(CONSUMPTION, DEBIT, 4L, OUT_OF_RANGE);

    var rows =
        creditTransactionRepository.findByTypesBetween(
            List.of(CONSUMPTION, CONSUMPTION_REVERSAL), FROM, TO);

    assertEquals(2, rows.size());
    assertEquals(
        true,
        rows.stream()
            .allMatch(
                t ->
                    (t.getType() == CONSUMPTION || t.getType() == CONSUMPTION_REVERSAL)
                        && t.getCreationDatetime().equals(IN_RANGE)));
  }

  @Test
  void findOverageLines_returns_only_overage_lines_of_the_system_user_invoices() {
    var window = Instant.now();
    var from = window.minus(1, DAYS);
    var to = window.plus(1, DAYS);
    var systemInvoiceId =
        persistInvoice(SYSTEM_USER, List.of(overageLine(3), regularLine("Autre prestation")));
    try {
      var rows =
          invoiceJpaRepository.findOverageLines(SYSTEM_USER, OVERAGE_PRODUCT_DESCRIPTION, from, to);

      assertEquals(1, rows.size());
      OverageInvoiceLine line = rows.get(0);
      assertEquals(3, line.getQuantity());
      assertEquals("200/1", line.getUnitPrice());
      assertEquals("2000/1", line.getVatPercent());

      var otherUserRows =
          invoiceJpaRepository.findOverageLines(
              "another-user", OVERAGE_PRODUCT_DESCRIPTION, from, to);
      assertEquals(0, otherUserRows.size());
    } finally {
      invoiceJpaRepository.deleteById(systemInvoiceId);
    }
  }

  @Test
  void findByUserIdIn_returns_subscriptions_of_the_given_users() {
    SubscriptionProduct plan =
        subscriptionProductRepository.findFirstByBillingType(COMMITMENT).orElseThrow();
    var uspId =
        userSubscriptionProductJpaRepository
            .save(
                UserSubscriptionProduct.builder()
                    .id(randomUUID().toString())
                    .userId(JOE_DOE_ID)
                    .subscriptionProduct(plan)
                    .billingInterval(MONTHLY)
                    .subscriptionStartDatetime(IN_RANGE.minus(30, DAYS))
                    .subscriptionEndDatetime(null)
                    .build())
            .getId();
    try {
      var rows = userSubscriptionProductJpaRepository.findByUserIdIn(List.of(JOE_DOE_ID));

      assertEquals(true, rows.stream().anyMatch(usp -> usp.getId().equals(uspId)));
      assertEquals(
          true,
          rows.stream()
              .filter(usp -> usp.getId().equals(uspId))
              .allMatch(usp -> plan.getId().equals(usp.getSubscriptionProduct().getId())));
    } finally {
      userSubscriptionProductJpaRepository.deleteById(uspId);
    }
  }

  private void persistPayment(long ht, Instant paymentDatetime, String invoiceId) {
    subscriptionPaymentRepository.save(
        SubscriptionPayment.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .stripeInvoiceId(randomUUID().toString())
            .billingInterval(BillingInterval.MONTHLY)
            .amountInCentsWithoutVat(ht)
            .amountInCentsWithVat(ht)
            .vatPercent(2000L)
            .paymentDatetime(paymentDatetime)
            .invoiceId(invoiceId)
            .build());
  }

  private void persistPurchase(CreditPurchaseStatus status, Instant completionDatetime) {
    creditPurchaseRepository.save(
        CreditPurchase.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(CreditPurchaseType.CUSTOM)
            .credits(10L)
            .creditUnitPriceInCentsWithoutVat(100L)
            .amountInCentsWithoutVat(1000L)
            .amountInCentsWithVat(1200L)
            .vatPercent(2000L)
            .status(status)
            .origin(CreditPurchaseOrigin.SELF_SERVICE)
            .completionDatetime(completionDatetime)
            .build());
  }

  private void persistTransaction(
      CreditTransactionType type,
      CreditTransactionMovementType movementType,
      long credits,
      Instant creationDatetime) {
    creditTransactionRepository.save(
        CreditTransaction.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(type)
            .movementType(movementType)
            .credits(credits)
            .creationDatetime(creationDatetime)
            .build());
  }

  private String persistInvoice(String idUser, List<HInvoiceProduct> products) {
    var invoiceId = randomUUID().toString();
    invoiceJpaRepository.save(
        HInvoice.builder()
            .id(invoiceId)
            .idUser(idUser)
            .status(InvoiceStatus.CONFIRMED)
            .archiveStatus(ArchiveStatus.ENABLED)
            .metadataString("{}")
            .products(products)
            .build());
    return invoiceId;
  }

  private static HInvoiceProduct overageLine(int quantity) {
    return HInvoiceProduct.builder()
        .description(OVERAGE_PRODUCT_DESCRIPTION)
        .quantity(quantity)
        .unitPrice("200/1")
        .vatPercent("2000/1")
        .status(ProductStatus.ENABLED)
        .build();
  }

  private static HInvoiceProduct regularLine(String description) {
    return HInvoiceProduct.builder()
        .description(description)
        .quantity(1)
        .unitPrice("5000/1")
        .vatPercent("2000/1")
        .status(ProductStatus.ENABLED)
        .build();
  }
}
