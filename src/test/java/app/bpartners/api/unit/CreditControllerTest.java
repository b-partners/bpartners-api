package app.bpartners.api.unit;

import static app.bpartners.api.model.credit.CreditCode.ANALYSES_10;
import static app.bpartners.api.model.credit.CreditCode.PACK_CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.CreditController;
import app.bpartners.api.endpoint.rest.mapper.CreditBalanceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CreditPackRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CreditPurchaseRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CreditTransactionRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCreditPackPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCreditPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCustomCreditPurchase;
import app.bpartners.api.endpoint.rest.model.CreditPackDescription;
import app.bpartners.api.endpoint.rest.model.CreditPackPurchase;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseOrigin;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseType;
import app.bpartners.api.endpoint.rest.model.CustomCreditPurchase;
import app.bpartners.api.endpoint.rest.model.Redirection1;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.endpoint.rest.validator.CreateCreditPurchaseRestValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditBalance;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchaseSubmission;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionMovementType;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.model.credit.CreditUnitPrice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.credit.CreditPurchaseService;
import app.bpartners.api.service.credit.CreditService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreditControllerTest {
  CreditPackRestMapper creditPackRestMapper = new CreditPackRestMapper();
  CreditBalanceRestMapper creditBalanceRestMapper = new CreditBalanceRestMapper();
  CreditTransactionRestMapper creditTransactionRestMapper = new CreditTransactionRestMapper();
  CreditPurchaseRestMapper creditPurchaseRestMapper =
      new CreditPurchaseRestMapper(creditPackRestMapper);
  CreditService creditServiceMock = mock(CreditService.class);
  CreditPurchaseService creditPurchaseServiceMock = mock(CreditPurchaseService.class);
  CreateCreditPurchaseRestValidator createCreditPurchaseRestValidator =
      new CreateCreditPurchaseRestValidator();
  AuthenticatedResourceProvider authenticatedResourceProviderMock =
      mock(AuthenticatedResourceProvider.class);

  CreditController subject =
      new CreditController(
          creditServiceMock,
          creditPackRestMapper,
          creditBalanceRestMapper,
          creditTransactionRestMapper,
          creditPurchaseRestMapper,
          creditPurchaseServiceMock,
          createCreditPurchaseRestValidator,
          authenticatedResourceProviderMock);

  @Test
  void get_credit_packs_priced_for_the_caller_active_plan() {
    var page = new PageFromOne(1);
    var pageSize = new BoundedPageSize(100);
    var user = User.builder().id("user_id").build();
    when(authenticatedResourceProviderMock.getUser()).thenReturn(user);
    when(creditServiceMock.resolveCreditUnitPrice(user))
        .thenReturn(new CreditUnitPrice(500L, 2000L));
    when(creditServiceMock.getCreditPacks(page, pageSize))
        .thenReturn(
            List.of(
                CreditPack.builder()
                    .id("pack_10")
                    .code(ANALYSES_10)
                    .description("10 analyses de toiture")
                    .creditPurchaseType(PACK)
                    .credits(10L)
                    .mostChosen(true)
                    .displayPosition(2)
                    .build(),
                CreditPack.builder()
                    .id("pack_custom")
                    .code(PACK_CUSTOM)
                    .description("Nombre d'analyses au choix")
                    .creditPurchaseType(CUSTOM)
                    .displayPosition(4)
                    .build()));

    var actual = subject.getCreditPacks(page, pageSize);

    assertEquals(
        List.of(
            new app.bpartners.api.endpoint.rest.model.CreditPack()
                .id("pack_10")
                .code("ANALYSES_10")
                .description("10 analyses de toiture")
                .creditPurchaseType(CreditPurchaseType.PACK)
                .credits(10L)
                .creditUnitPriceInCentsWithoutVat(500L)
                .creditUnitPriceInCentsWithVat(600L)
                .priceInCentsWithoutVat(5000L)
                .priceInCentsWithVat(6000L)
                .vatPercent(2000L)
                .isMostChosen(true)
                .isDeprecated(false)
                .displayPosition(2),
            new app.bpartners.api.endpoint.rest.model.CreditPack()
                .id("pack_custom")
                .code("PACK_CUSTOM")
                .description("Nombre d'analyses au choix")
                .creditPurchaseType(CreditPurchaseType.CUSTOM)
                .creditUnitPriceInCentsWithoutVat(500L)
                .creditUnitPriceInCentsWithVat(600L)
                .vatPercent(2000L)
                .isMostChosen(false)
                .isDeprecated(false)
                .displayPosition(4)),
        actual);
  }

  @Test
  void get_credit_pack_by_id_priced_for_the_caller_active_plan() {
    var user = User.builder().id("user_id").build();
    when(authenticatedResourceProviderMock.getUser()).thenReturn(user);
    when(creditServiceMock.resolveCreditUnitPrice(user))
        .thenReturn(new CreditUnitPrice(500L, 2000L));
    when(creditServiceMock.getCreditPack("pack_10"))
        .thenReturn(
            CreditPack.builder()
                .id("pack_10")
                .code(ANALYSES_10)
                .description("10 analyses de toiture")
                .creditPurchaseType(PACK)
                .credits(10L)
                .displayPosition(2)
                .build());

    var actual = subject.getCreditPackById("pack_10");

    assertEquals(
        new app.bpartners.api.endpoint.rest.model.CreditPack()
            .id("pack_10")
            .code("ANALYSES_10")
            .description("10 analyses de toiture")
            .creditPurchaseType(CreditPurchaseType.PACK)
            .credits(10L)
            .creditUnitPriceInCentsWithoutVat(500L)
            .creditUnitPriceInCentsWithVat(600L)
            .priceInCentsWithoutVat(5000L)
            .priceInCentsWithVat(6000L)
            .vatPercent(2000L)
            .isMostChosen(false)
            .isDeprecated(false)
            .displayPosition(2),
        actual);
  }

  @Test
  void get_credit_balance() {
    when(creditServiceMock.getCreditBalance("user_id"))
        .thenReturn(
            CreditBalance.builder()
                .spendableCredits(45L)
                .grantedCredits(15L)
                .purchasedCredits(30L)
                .creditCostPerAnalysis(2L)
                .estimatedRemainingAnalyses(22L)
                .expirations(List.of())
                .build());

    var actual = subject.getCreditBalance("user_id");

    assertEquals(
        new app.bpartners.api.endpoint.rest.model.CreditBalance()
            .spendableCredits(45L)
            .grantedCredits(15L)
            .purchasedCredits(30L)
            .creditCostPerAnalysis(2L)
            .estimatedRemainingAnalyses(22L)
            .expirations(List.of()),
        actual);
  }

  @Test
  void get_credit_transactions_maps_domain_filters_and_results() {
    var creation = java.time.Instant.parse("2026-08-01T00:00:00Z");
    when(creditServiceMock.getCreditTransactions(
            "user_id", List.of(CreditTransactionType.PURCHASE), null, null, null, null))
        .thenReturn(
            List.of(
                CreditTransaction.builder()
                    .id("tx_1")
                    .userId("user_id")
                    .type(CreditTransactionType.PURCHASE)
                    .movementType(CreditTransactionMovementType.CREDIT)
                    .credits(30L)
                    .creationDatetime(creation)
                    .build()));

    var actual =
        subject.getCreditTransactions(
            "user_id",
            List.of(app.bpartners.api.endpoint.rest.model.CreditTransactionType.PURCHASE),
            null,
            null,
            null,
            null);

    assertEquals(
        List.of(
            new app.bpartners.api.endpoint.rest.model.CreditTransaction()
                .id("tx_1")
                .type(app.bpartners.api.endpoint.rest.model.CreditTransactionType.PURCHASE)
                .movementType(
                    app.bpartners.api.endpoint.rest.model.CreditTransactionMovementType.CREDIT)
                .credits(30L)
                .creationDatetime(creation)),
        actual);
  }

  @Test
  void get_credit_transaction_by_id() {
    var creation = java.time.Instant.parse("2026-08-01T00:00:00Z");
    when(creditServiceMock.getCreditTransaction("user_id", "tx_1"))
        .thenReturn(
            CreditTransaction.builder()
                .id("tx_1")
                .userId("user_id")
                .type(CreditTransactionType.CONSUMPTION)
                .movementType(CreditTransactionMovementType.DEBIT)
                .credits(5L)
                .creationDatetime(creation)
                .build());

    var actual = subject.getCreditTransactionById("user_id", "tx_1");

    assertEquals(
        new app.bpartners.api.endpoint.rest.model.CreditTransaction()
            .id("tx_1")
            .type(app.bpartners.api.endpoint.rest.model.CreditTransactionType.CONSUMPTION)
            .movementType(app.bpartners.api.endpoint.rest.model.CreditTransactionMovementType.DEBIT)
            .credits(5L)
            .creationDatetime(creation),
        actual);
  }

  @Test
  void get_credit_purchases_maps_a_pack_purchase_priced_as_paid() {
    var creation = java.time.Instant.parse("2026-08-01T00:00:00Z");
    var completion = java.time.Instant.parse("2026-08-01T00:05:00Z");
    var expiration = java.time.Instant.parse("2027-08-01T00:00:00Z");
    when(creditServiceMock.getCreditPurchases(
            "user_id",
            List.of(app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED),
            null,
            null,
            null,
            null))
        .thenReturn(
            List.of(
                app.bpartners.api.model.credit.CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(PACK)
                    .creditPack(
                        CreditPack.builder()
                            .id("pack_10")
                            .code(ANALYSES_10)
                            .description("10 analyses de toiture")
                            .creditPurchaseType(PACK)
                            .credits(10L)
                            .validityDays(365)
                            .build())
                    .quantity(2)
                    .credits(20L)
                    .creditUnitPriceInCentsWithoutVat(400L)
                    .amountInCentsWithoutVat(8000L)
                    .amountInCentsWithVat(9600L)
                    .vatPercent(2000L)
                    .status(app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED)
                    .origin(app.bpartners.api.model.credit.CreditPurchaseOrigin.SELF_SERVICE)
                    .redirectionUrl("https://pay.stripe.com/session")
                    .redirectionSuccessUrl("https://birdia.fr/success")
                    .redirectionFailureUrl("https://birdia.fr/failure")
                    .creditTransactionId("tx_1")
                    .invoiceId("invoice_1")
                    .creationDatetime(creation)
                    .completionDatetime(completion)
                    .creditsExpirationDatetime(expiration)
                    .build()));

    var actual =
        subject.getCreditPurchases(
            "user_id", List.of(CreditPurchaseStatus.COMPLETED), null, null, null, null);

    assertEquals(
        List.of(
            new app.bpartners.api.endpoint.rest.model.CreditPurchase()
                .id("purchase_1")
                .type(CreditPurchaseType.PACK)
                .packPurchase(
                    new CreditPackPurchase()
                        .creditPack(
                            new CreditPackDescription()
                                .id("pack_10")
                                .code("ANALYSES_10")
                                .description("10 analyses de toiture")
                                .creditPurchaseType(CreditPurchaseType.PACK)
                                .credits(10L)
                                .creditUnitPriceInCentsWithoutVat(400L)
                                .creditUnitPriceInCentsWithVat(480L)
                                .priceInCentsWithoutVat(4000L)
                                .priceInCentsWithVat(4800L)
                                .vatPercent(2000L))
                        .quantity(2))
                .credits(20L)
                .amountInCentsWithoutVat(8000L)
                .amountInCentsWithVat(9600L)
                .vatPercent(2000L)
                .status(CreditPurchaseStatus.COMPLETED)
                .origin(CreditPurchaseOrigin.SELF_SERVICE)
                .redirection(
                    new Redirection1()
                        .redirectionUrl("https://pay.stripe.com/session")
                        .redirectionStatusUrls(
                            new RedirectionStatusUrls()
                                .successUrl("https://birdia.fr/success")
                                .failureUrl("https://birdia.fr/failure")))
                .creditTransactionId("tx_1")
                .invoiceId("invoice_1")
                .creationDatetime(creation)
                .completionDatetime(completion)
                .creditsExpirationDatetime(expiration)),
        actual);
  }

  @Test
  void get_credit_purchases_maps_a_custom_purchase_without_pack() {
    var creation = java.time.Instant.parse("2026-08-02T00:00:00Z");
    when(creditServiceMock.getCreditPurchases("user_id", null, null, null, null, null))
        .thenReturn(
            List.of(
                app.bpartners.api.model.credit.CreditPurchase.builder()
                    .id("purchase_2")
                    .userId("user_id")
                    .type(CUSTOM)
                    .credits(7L)
                    .creditUnitPriceInCentsWithoutVat(500L)
                    .amountInCentsWithoutVat(3500L)
                    .amountInCentsWithVat(4200L)
                    .vatPercent(2000L)
                    .status(app.bpartners.api.model.credit.CreditPurchaseStatus.PENDING)
                    .origin(app.bpartners.api.model.credit.CreditPurchaseOrigin.AUTO_RECHARGE)
                    .creationDatetime(creation)
                    .build()));

    var actual = subject.getCreditPurchases("user_id", null, null, null, null, null);

    assertEquals(
        List.of(
            new app.bpartners.api.endpoint.rest.model.CreditPurchase()
                .id("purchase_2")
                .type(CreditPurchaseType.CUSTOM)
                .customPurchase(new CustomCreditPurchase().creditUnitPriceInCentsWithoutVat(500L))
                .credits(7L)
                .amountInCentsWithoutVat(3500L)
                .amountInCentsWithVat(4200L)
                .vatPercent(2000L)
                .status(CreditPurchaseStatus.PENDING)
                .origin(CreditPurchaseOrigin.AUTO_RECHARGE)
                .creationDatetime(creation)),
        actual);
  }

  @Test
  void submit_a_pack_purchase_maps_the_payload_and_the_submitted_purchase() {
    var creation = java.time.Instant.parse("2026-08-01T00:00:00Z");
    var user = User.builder().id("user_id").userSubscriptionId("cus_1").build();
    when(authenticatedResourceProviderMock.getUser()).thenReturn(user);
    var submissionCaptor = ArgumentCaptor.forClass(CreditPurchaseSubmission.class);
    when(creditPurchaseServiceMock.submit(eq(user), any()))
        .thenReturn(
            app.bpartners.api.model.credit.CreditPurchase.builder()
                .id("purchase_1")
                .userId("user_id")
                .type(PACK)
                .credits(20L)
                .creditUnitPriceInCentsWithoutVat(400L)
                .amountInCentsWithoutVat(8000L)
                .amountInCentsWithVat(9600L)
                .vatPercent(2000L)
                .status(app.bpartners.api.model.credit.CreditPurchaseStatus.PENDING)
                .origin(app.bpartners.api.model.credit.CreditPurchaseOrigin.SELF_SERVICE)
                .redirectionUrl("https://pay.stripe.com/session")
                .creationDatetime(creation)
                .build());

    var actual =
        subject.submitCreditPurchase(
            "user_id",
            "purchase_1",
            (CreateCreditPurchase)
                new CreateCreditPackPurchase()
                    .creditPackIdentifier("pack_10")
                    .quantity(2)
                    .type(CreditPurchaseType.PACK)
                    .redirectionStatusUrls(
                        new RedirectionStatusUrls()
                            .successUrl("https://birdia.fr/success")
                            .failureUrl("https://birdia.fr/failure")));

    verify(creditPurchaseServiceMock).submit(eq(user), submissionCaptor.capture());
    assertEquals(
        new CreditPurchaseSubmission(
            "purchase_1",
            PACK,
            "pack_10",
            2,
            null,
            "https://birdia.fr/success",
            "https://birdia.fr/failure"),
        submissionCaptor.getValue());
    assertEquals("purchase_1", actual.getId());
    assertEquals(CreditPurchaseStatus.PENDING, actual.getStatus());
    assertEquals(9600L, actual.getAmountInCentsWithVat());
    assertEquals("https://pay.stripe.com/session", actual.getRedirection().getRedirectionUrl());
  }

  @Test
  void submit_a_custom_purchase_maps_the_chosen_credits() {
    var user = User.builder().id("user_id").userSubscriptionId("cus_1").build();
    when(authenticatedResourceProviderMock.getUser()).thenReturn(user);
    var submissionCaptor = ArgumentCaptor.forClass(CreditPurchaseSubmission.class);
    when(creditPurchaseServiceMock.submit(eq(user), any()))
        .thenReturn(
            app.bpartners.api.model.credit.CreditPurchase.builder().id("purchase_2").build());

    subject.submitCreditPurchase(
        "user_id",
        "purchase_2",
        (CreateCreditPurchase)
            new CreateCustomCreditPurchase()
                .credits(7L)
                .type(CreditPurchaseType.CUSTOM)
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl("https://birdia.fr/success")
                        .failureUrl("https://birdia.fr/failure")));

    verify(creditPurchaseServiceMock).submit(eq(user), submissionCaptor.capture());
    assertEquals(
        new CreditPurchaseSubmission(
            "purchase_2",
            CUSTOM,
            null,
            null,
            7L,
            "https://birdia.fr/success",
            "https://birdia.fr/failure"),
        submissionCaptor.getValue());
  }

  @Test
  void submit_an_invalid_payload_is_rejected_before_reaching_the_service() {
    var payloadWithoutPack =
        (CreateCreditPurchase) new CreateCreditPackPurchase().type(CreditPurchaseType.PACK);

    assertThrows(
        BadRequestException.class,
        () -> subject.submitCreditPurchase("user_id", "purchase_1", payloadWithoutPack));

    verify(creditPurchaseServiceMock, never()).submit(any(), any());
  }
}
