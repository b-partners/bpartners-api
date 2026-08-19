package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
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
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.CreditsApi;
import app.bpartners.api.endpoint.rest.model.CreateCreditPackPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCreditPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCustomCreditPurchase;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditPurchaseCharge;
import app.bpartners.api.model.credit.CreditPurchaseStatus;
import app.bpartners.api.repository.jpa.CreditPackRepository;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.credit.CreditService;
import com.stripe.model.PaymentMethod;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreditPurchaseIT extends MockedThirdParties {
  private static final String ANALYSES_10_PACK_ID = "a1c1e2f0-0000-4000-a000-000000000010";
  private static final String PACK_CUSTOM_ID = "a1c1e2f0-0000-4000-a000-0000000000c0";
  private static final String CHECKOUT_SESSION_URL = "https://pay.stripe.com/session";
  @Autowired private CreditPurchaseRepository creditPurchaseRepository;
  @Autowired private CreditPackRepository creditPackRepository;
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private CreditService creditService;

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
    when(stripeCreditPurchaseServiceMock.checkoutSessionUrl(any(), any(), any(), any()))
        .thenReturn(CHECKOUT_SESSION_URL);
    clearJoeDoePurchases();
  }

  @AfterEach
  void tearDown() {
    clearJoeDoePurchases();
  }

  private void clearJoeDoePurchases() {
    creditPurchaseRepository.deleteAll();
    creditTransactionRepository.deleteAll();
  }

  @SneakyThrows
  private void givenJoeDoeHasARegisteredCard() {
    var cardDetails = mock(PaymentMethod.Card.class);
    when(cardDetails.getExpYear()).thenReturn(2050L);
    when(cardDetails.getExpMonth()).thenReturn(12L);
    var card = mock(PaymentMethod.class);
    when(card.getType()).thenReturn("card");
    when(card.getCard()).thenReturn(cardDetails);
    when(stripePaymentMethodServiceMock.getPaymentMethod(any())).thenReturn(List.of(card));
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
        actual.stream()
            .map(app.bpartners.api.endpoint.rest.model.CreditPurchase::getStatus)
            .toList());
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

  private RedirectionStatusUrls bothUrls() {
    return new RedirectionStatusUrls()
        .successUrl("https://birdia.fr/success")
        .failureUrl("https://birdia.fr/failure");
  }

  private CreateCreditPurchase packPayload(String packId, Integer quantity) {
    return new CreateCreditPackPurchase()
        .creditPackIdentifier(packId)
        .quantity(quantity)
        .type(app.bpartners.api.endpoint.rest.model.CreditPurchaseType.PACK)
        .redirectionStatusUrls(bothUrls());
  }

  private CreateCreditPurchase customPayload(Long credits) {
    return new CreateCustomCreditPurchase()
        .credits(credits)
        .type(app.bpartners.api.endpoint.rest.model.CreditPurchaseType.CUSTOM)
        .redirectionStatusUrls(bothUrls());
  }

  @SneakyThrows
  @Test
  void submit_a_pack_purchase_returns_it_pending_with_its_redirection() {
    var actual =
        joeCreditsApi()
            .submitCreditPurchase(JOE_DOE_ID, "purchase_1", packPayload(ANALYSES_10_PACK_ID, 2));

    assertEquals("purchase_1", actual.getId());
    assertEquals(app.bpartners.api.endpoint.rest.model.CreditPurchaseType.PACK, actual.getType());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.PENDING, actual.getStatus());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditPurchaseOrigin.SELF_SERVICE,
        actual.getOrigin());
    assertEquals(20L, actual.getCredits());
    assertEquals(20000L, actual.getAmountInCentsWithoutVat());
    assertEquals(24000L, actual.getAmountInCentsWithVat());
    assertEquals(2000L, actual.getVatPercent());
    assertEquals(2, actual.getPackPurchase().getQuantity());
    assertEquals(ANALYSES_10_PACK_ID, actual.getPackPurchase().getCreditPack().getId());
    assertEquals(
        1000L, actual.getPackPurchase().getCreditPack().getCreditUnitPriceInCentsWithoutVat());
    assertEquals(
        1200L, actual.getPackPurchase().getCreditPack().getCreditUnitPriceInCentsWithVat());
    assertNull(actual.getCustomPurchase());
    assertEquals(CHECKOUT_SESSION_URL, actual.getRedirection().getRedirectionUrl());
    assertEquals(
        "https://birdia.fr/success",
        actual.getRedirection().getRedirectionStatusUrls().getSuccessUrl());
    assertNull(actual.getCreditTransactionId());
    assertNull(actual.getCompletionDatetime());
    assertNull(actual.getCreditsExpirationDatetime());
    assertEquals(
        List.of(actual),
        joeCreditsApi().getCreditPurchases(JOE_DOE_ID, null, null, null, null, null));
  }

  @SneakyThrows
  @Test
  void submit_a_custom_purchase_is_priced_at_the_unit_price_in_force() {
    var actual = joeCreditsApi().submitCreditPurchase(JOE_DOE_ID, "purchase_1", customPayload(7L));

    assertEquals(app.bpartners.api.endpoint.rest.model.CreditPurchaseType.CUSTOM, actual.getType());
    assertEquals(7L, actual.getCredits());
    assertEquals(7000L, actual.getAmountInCentsWithoutVat());
    assertEquals(8400L, actual.getAmountInCentsWithVat());
    assertEquals(1000L, actual.getCustomPurchase().getCreditUnitPriceInCentsWithoutVat());
    assertNull(actual.getPackPurchase());
  }

  @SneakyThrows
  @Test
  void resubmitting_the_same_payload_buys_only_once() {
    var first =
        joeCreditsApi()
            .submitCreditPurchase(JOE_DOE_ID, "purchase_1", packPayload(ANALYSES_10_PACK_ID, 2));
    var second =
        joeCreditsApi()
            .submitCreditPurchase(JOE_DOE_ID, "purchase_1", packPayload(ANALYSES_10_PACK_ID, 2));

    assertEquals(first, second);
    assertEquals(1, creditPurchaseRepository.findAll().size());
    verify(stripeCreditPurchaseServiceMock, times(1))
        .checkoutSessionUrl(any(), any(), any(), any());
  }

  @SneakyThrows
  @Test
  void resubmitting_a_different_payload_is_a_conflict() {
    joeCreditsApi()
        .submitCreditPurchase(JOE_DOE_ID, "purchase_1", packPayload(ANALYSES_10_PACK_ID, 2));

    var api = joeCreditsApi();
    var anotherPayload = packPayload(ANALYSES_10_PACK_ID, 3);

    assertThrowsApiException(
        "{\"type\":\"409 CONFLICT\",\"message\":\"CreditPurchase.id=purchase_1 was already"
            + " submitted with a different payload, a purchase is immutable\"}",
        () -> api.submitCreditPurchase(JOE_DOE_ID, "purchase_1", anotherPayload));

    assertEquals(1, creditPurchaseRepository.findAll().size());
  }

  @Test
  void submit_a_pack_purchase_on_an_unknown_pack_is_not_found() {
    var api = joeCreditsApi();
    var unknownPackPayload = packPayload("unknown", 1);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"CreditPack(id=unknown) not found\"}",
        () -> api.submitCreditPurchase(JOE_DOE_ID, "purchase_1", unknownPackPayload));

    assertEquals(0, creditPurchaseRepository.findAll().size());
  }

  @Test
  void submit_a_pack_purchase_on_the_custom_pack_is_rejected() {
    var api = joeCreditsApi();
    var customPackAsPackPayload = packPayload(PACK_CUSTOM_ID, 1);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"CreditPack(id="
            + PACK_CUSTOM_ID
            + ") carries no fixed credits amount, submit a CUSTOM purchase instead\"}",
        () -> api.submitCreditPurchase(JOE_DOE_ID, "purchase_1", customPackAsPackPayload));
  }

  @Test
  void submit_an_incomplete_payload_is_a_bad_request() {
    var api = joeCreditsApi();
    var payloadWithoutRedirectionUrls =
        new CreateCustomCreditPurchase()
            .credits(7L)
            .type(app.bpartners.api.endpoint.rest.model.CreditPurchaseType.CUSTOM);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"CreateCreditPurchase"
            + ".redirectionStatusUrls is mandatory.\"}",
        () -> api.submitCreditPurchase(JOE_DOE_ID, "purchase_1", payloadWithoutRedirectionUrls));
  }

  @Test
  void another_user_purchase_cannot_be_submitted() {
    var api = joeCreditsApi();
    var payload = packPayload(ANALYSES_10_PACK_ID, 1);

    assertThrowsForbiddenException(
        () -> api.submitCreditPurchase(JANE_DOE_ID, "purchase_1", payload));
  }

  @SneakyThrows
  @Test
  void submit_charges_the_registered_card_and_grants_the_credits_right_away() {
    givenJoeDoeHasARegisteredCard();
    when(stripeCreditPurchaseServiceMock.chargeOffSession(any(), any()))
        .thenReturn(CreditPurchaseCharge.succeeded("pi_1"));
    var creditsBefore = creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits();

    var actual =
        joeCreditsApi()
            .submitCreditPurchase(JOE_DOE_ID, "purchase_1", packPayload(ANALYSES_10_PACK_ID, 2));

    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.COMPLETED, actual.getStatus());
    assertNull(actual.getRedirection());
    assertNotNull(actual.getCompletionDatetime());
    assertNotNull(actual.getCreditTransactionId());
    verify(stripeCreditPurchaseServiceMock, never()).checkoutSessionUrl(any(), any(), any(), any());

    var granted =
        creditTransactionRepository.findById(actual.getCreditTransactionId()).orElseThrow();
    assertEquals(PURCHASE, granted.getType());
    assertEquals(CREDIT, granted.getMovementType());
    assertEquals(20L, granted.getCredits());
    assertEquals("purchase_1", granted.getCreditPurchaseId());
    assertEquals("10 analyses de toiture", granted.getLabel());
    assertNull(granted.getExpirationDatetime());
    assertEquals(
        creditsBefore + 20L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void resubmitting_a_completed_purchase_does_not_grant_the_credits_twice() {
    givenJoeDoeHasARegisteredCard();
    when(stripeCreditPurchaseServiceMock.chargeOffSession(any(), any()))
        .thenReturn(CreditPurchaseCharge.succeeded("pi_1"));

    var first = joeCreditsApi().submitCreditPurchase(JOE_DOE_ID, "purchase_1", customPayload(7L));
    var second = joeCreditsApi().submitCreditPurchase(JOE_DOE_ID, "purchase_1", customPayload(7L));

    assertEquals(first, second);
    assertEquals(1, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(7L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void submit_falls_back_to_checkout_when_the_registered_card_needs_authentication() {
    givenJoeDoeHasARegisteredCard();
    when(stripeCreditPurchaseServiceMock.chargeOffSession(any(), any()))
        .thenReturn(CreditPurchaseCharge.failed("authentication_required"));

    var actual = joeCreditsApi().submitCreditPurchase(JOE_DOE_ID, "purchase_1", customPayload(7L));

    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus.PENDING, actual.getStatus());
    assertEquals(CHECKOUT_SESSION_URL, actual.getRedirection().getRedirectionUrl());
    assertNull(actual.getCreditTransactionId());
    assertEquals(0, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(0L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @Test
  void a_purchase_reads_back_with_the_datetimes_it_was_returned_with() {
    var stampedWithNanos = Instant.parse("2026-08-01T00:00:00.123456789Z");
    var saved =
        creditPurchaseRepository.save(
            CreditPurchase.builder()
                .id(randomUUID().toString())
                .userId(JOE_DOE_ID)
                .type(CUSTOM)
                .credits(7L)
                .status(COMPLETED)
                .origin(SELF_SERVICE)
                .creationDatetime(stampedWithNanos)
                .completionDatetime(stampedWithNanos)
                .build());

    var reread = creditPurchaseRepository.findById(saved.getId()).orElseThrow();

    assertEquals(saved.getCreationDatetime(), reread.getCreationDatetime());
    assertEquals(saved.getCompletionDatetime(), reread.getCompletionDatetime());
  }

  @Test
  void another_user_purchases_are_not_readable() {
    var api = joeCreditsApi();

    assertThrowsForbiddenException(
        () -> api.getCreditPurchases(JANE_DOE_ID, null, null, null, null, null));
  }
}
