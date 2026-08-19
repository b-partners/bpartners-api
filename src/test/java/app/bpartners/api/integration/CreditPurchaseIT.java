package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static app.bpartners.api.model.credit.CreditPurchaseOrigin.AUTO_RECHARGE;
import static app.bpartners.api.model.credit.CreditPurchaseOrigin.SELF_SERVICE;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.FAILED;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.PENDING;
import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.endpoint.rest.api.CreditsApi;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditPurchaseStatus;
import app.bpartners.api.repository.jpa.CreditPackRepository;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreditPurchaseIT extends MockedThirdParties {
  private static final String ANALYSES_10_PACK_ID = "a1c1e2f0-0000-4000-a000-000000000010";
  @Autowired private CreditPurchaseRepository creditPurchaseRepository;
  @Autowired private CreditPackRepository creditPackRepository;

  private final Instant now = Instant.now().truncatedTo(MILLIS);

  private CreditsApi joeCreditsApi() {
    return new CreditsApi(TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort));
  }

  private String persistPackPurchase(CreditPurchaseStatus status, Instant creationDatetime) {
    return creditPurchaseRepository
        .save(
            CreditPurchase.builder()
                .id(randomUUID().toString())
                .userId(JOE_DOE_ID)
                .type(PACK)
                .creditPack(creditPackRepository.findById(ANALYSES_10_PACK_ID).orElseThrow())
                .quantity(2)
                .credits(20L)
                .creditUnitPriceInCentsWithoutVat(400L)
                .amountInCentsWithoutVat(8000L)
                .amountInCentsWithVat(9600L)
                .vatPercent(2000L)
                .status(status)
                .origin(SELF_SERVICE)
                .redirectionUrl("https://pay.stripe.com/session")
                .redirectionSuccessUrl("https://birdia.fr/success")
                .redirectionFailureUrl("https://birdia.fr/failure")
                .creditTransactionId("tx_1")
                .invoiceId("invoice_1")
                .creationDatetime(creationDatetime)
                .completionDatetime(creationDatetime.plus(1, DAYS))
                .creditsExpirationDatetime(creationDatetime.plus(365, DAYS))
                .build())
        .getId();
  }

  private String persistCustomPurchase(CreditPurchaseStatus status, Instant creationDatetime) {
    return creditPurchaseRepository
        .save(
            CreditPurchase.builder()
                .id(randomUUID().toString())
                .userId(JOE_DOE_ID)
                .type(CUSTOM)
                .credits(7L)
                .creditUnitPriceInCentsWithoutVat(500L)
                .amountInCentsWithoutVat(3500L)
                .amountInCentsWithVat(4200L)
                .vatPercent(2000L)
                .status(status)
                .origin(AUTO_RECHARGE)
                .creationDatetime(creationDatetime)
                .build())
        .getId();
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpUserSubscription(subscriptionService);
    creditPurchaseRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    creditPurchaseRepository.deleteAll();
  }

  @SneakyThrows
  @Test
  void list_returns_all_purchases_most_recent_first() {
    persistPackPurchase(COMPLETED, now.minus(2, DAYS));
    persistCustomPurchase(PENDING, now.minus(1, DAYS));
    persistPackPurchase(FAILED, now);

    var actual = joeCreditsApi().getCreditPurchases(JOE_DOE_ID, null, null, null, null, null);

    assertEquals(3, actual.size());
    assertEquals(
        List.of(
            app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.FAILED,
            app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.PENDING,
            app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.COMPLETED),
        actual.stream().map(purchase -> purchase.getStatus()).toList());
  }

  @SneakyThrows
  @Test
  void list_filters_by_status() {
    persistPackPurchase(COMPLETED, now.minus(2, DAYS));
    persistCustomPurchase(PENDING, now.minus(1, DAYS));

    var actual =
        joeCreditsApi()
            .getCreditPurchases(
                JOE_DOE_ID,
                List.of(app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.PENDING),
                null,
                null,
                null,
                null);

    assertEquals(1, actual.size());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.PENDING,
        actual.getFirst().getStatus());
  }

  @SneakyThrows
  @Test
  void list_filters_by_period() {
    persistPackPurchase(COMPLETED, now.minus(10, DAYS));
    var recentId = persistCustomPurchase(PENDING, now.minus(1, DAYS));

    var actual =
        joeCreditsApi().getCreditPurchases(JOE_DOE_ID, null, now.minus(2, DAYS), now, null, null);

    assertEquals(1, actual.size());
    assertEquals(recentId, actual.getFirst().getId());
  }

  @SneakyThrows
  @Test
  void pack_purchase_carries_the_pack_priced_as_paid() {
    var purchaseId = persistPackPurchase(COMPLETED, now);

    var actual =
        joeCreditsApi().getCreditPurchases(JOE_DOE_ID, null, null, null, null, null).getFirst();

    assertEquals(purchaseId, actual.getId());
    assertEquals(app.bpartners.api.endpoint.rest.model.CreditPurchaseType.PACK, actual.getType());
    assertNull(actual.getCustomPurchase());
    assertEquals(2, actual.getPackPurchase().getQuantity());
    var pack = actual.getPackPurchase().getCreditPack();
    assertEquals(ANALYSES_10_PACK_ID, pack.getId());
    assertEquals("ANALYSES_10", pack.getCode());
    assertEquals(10L, pack.getCredits());
    assertEquals(400L, pack.getCreditUnitPriceInCentsWithoutVat());
    assertEquals(480L, pack.getCreditUnitPriceInCentsWithVat());
    assertEquals(4000L, pack.getPriceInCentsWithoutVat());
    assertEquals(4800L, pack.getPriceInCentsWithVat());
    assertEquals(20L, actual.getCredits());
    assertEquals(8000L, actual.getAmountInCentsWithoutVat());
    assertEquals(9600L, actual.getAmountInCentsWithVat());
    assertEquals(2000L, actual.getVatPercent());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditPurchaseOrigin.SELF_SERVICE,
        actual.getOrigin());
    assertEquals("https://pay.stripe.com/session", actual.getRedirection().getRedirectionUrl());
    assertEquals(
        "https://birdia.fr/success",
        actual.getRedirection().getRedirectionStatusUrls().getSuccessUrl());
    assertEquals(
        "https://birdia.fr/failure",
        actual.getRedirection().getRedirectionStatusUrls().getFailureUrl());
    assertEquals("tx_1", actual.getCreditTransactionId());
    assertEquals("invoice_1", actual.getInvoiceId());
    assertEquals(now.plus(1, DAYS), actual.getCompletionDatetime());
    assertEquals(now.plus(365, DAYS), actual.getCreditsExpirationDatetime());
  }

  @SneakyThrows
  @Test
  void custom_purchase_carries_the_unit_price_applied_and_no_pack() {
    persistCustomPurchase(PENDING, now);

    var actual =
        joeCreditsApi().getCreditPurchases(JOE_DOE_ID, null, null, null, null, null).getFirst();

    assertEquals(app.bpartners.api.endpoint.rest.model.CreditPurchaseType.CUSTOM, actual.getType());
    assertNull(actual.getPackPurchase());
    assertNull(actual.getRedirection());
    assertEquals(500L, actual.getCustomPurchase().getCreditUnitPriceInCentsWithoutVat());
    assertEquals(7L, actual.getCredits());
    assertEquals(3500L, actual.getAmountInCentsWithoutVat());
    assertEquals(4200L, actual.getAmountInCentsWithVat());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditPurchaseOrigin.AUTO_RECHARGE,
        actual.getOrigin());
  }

  @Test
  void another_user_purchases_are_not_readable() {
    assertThrowsForbiddenException(
        () -> joeCreditsApi().getCreditPurchases(JANE_DOE_ID, null, null, null, null, null));
  }
}
