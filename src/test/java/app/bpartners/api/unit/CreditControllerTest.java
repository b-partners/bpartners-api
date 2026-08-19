package app.bpartners.api.unit;

import static app.bpartners.api.model.credit.CreditCode.ANALYSES_10;
import static app.bpartners.api.model.credit.CreditCode.PACK_CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.CreditController;
import app.bpartners.api.endpoint.rest.mapper.CreditBalanceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CreditPackRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CreditTransactionRestMapper;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseType;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditBalance;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionMovementType;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.model.credit.CreditUnitPrice;
import app.bpartners.api.service.credit.CreditService;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreditControllerTest {
  CreditPackRestMapper creditPackRestMapper = new CreditPackRestMapper();
  CreditBalanceRestMapper creditBalanceRestMapper = new CreditBalanceRestMapper();
  CreditTransactionRestMapper creditTransactionRestMapper = new CreditTransactionRestMapper();
  CreditService creditServiceMock = mock(CreditService.class);
  AuthenticatedResourceProvider authenticatedResourceProviderMock =
      mock(AuthenticatedResourceProvider.class);

  CreditController subject =
      new CreditController(
          creditServiceMock,
          creditPackRestMapper,
          creditBalanceRestMapper,
          creditTransactionRestMapper,
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
}
