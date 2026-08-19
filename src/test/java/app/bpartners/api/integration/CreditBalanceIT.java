package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.CreditsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.model.CreditOrigin;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionMovementType;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import java.time.Instant;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreditBalanceIT extends MockedThirdParties {
  @Autowired private CreditTransactionRepository creditTransactionRepository;

  private CreditsApi joeCreditsApi() {
    ApiClient joeClient = TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
    return new CreditsApi(joeClient);
  }

  private void persist(
      CreditTransactionType type,
      CreditTransactionMovementType movementType,
      long credits,
      Instant expirationDatetime,
      Instant creationDatetime) {
    creditTransactionRepository.save(
        CreditTransaction.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(type)
            .movementType(movementType)
            .credits(credits)
            .expirationDatetime(expirationDatetime)
            .creationDatetime(creationDatetime)
            .build());
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpUserSubscription(subscriptionService);
    creditTransactionRepository.deleteAll();
  }

  @SneakyThrows
  @Test
  void get_credit_balance_computed_from_persisted_ledger() {
    var now = Instant.now().truncatedTo(MILLIS);
    var grantExpiry = now.plus(10, DAYS);
    persist(SUBSCRIPTION_GRANT, CREDIT, 20L, grantExpiry, now.minus(2, DAYS));
    persist(PURCHASE, CREDIT, 30L, null, now.minus(1, DAYS));
    persist(CONSUMPTION, DEBIT, 5L, null, now);

    var actual = joeCreditsApi().getCreditBalance(JOE_DOE_ID);

    assertEquals(15L, actual.getGrantedCredits());
    assertEquals(30L, actual.getPurchasedCredits());
    assertEquals(45L, actual.getSpendableCredits());
    assertEquals(1L, actual.getCreditCostPerAnalysis());
    assertEquals(45L, actual.getEstimatedRemainingAnalyses());
    assertNull(actual.getNextGrantDatetime());
    assertEquals(now, actual.getUpdatedAt());
    assertEquals(1, actual.getExpirations().size());
    assertEquals(15L, actual.getExpirations().getFirst().getCredits());
    assertEquals(grantExpiry, actual.getExpirations().getFirst().getExpirationDatetime());
    assertEquals(CreditOrigin.SUBSCRIPTION_GRANT, actual.getExpirations().getFirst().getOrigin());
  }

  @SneakyThrows
  @Test
  void get_credit_balance_is_empty_without_transactions() {
    var actual = joeCreditsApi().getCreditBalance(JOE_DOE_ID);

    assertEquals(0L, actual.getSpendableCredits());
    assertEquals(0L, actual.getGrantedCredits());
    assertEquals(0L, actual.getPurchasedCredits());
    assertEquals(1L, actual.getCreditCostPerAnalysis());
    assertEquals(0L, actual.getEstimatedRemainingAnalyses());
    assertNull(actual.getNextGrantDatetime());
    assertNull(actual.getUpdatedAt());
    assertTrue(actual.getExpirations().isEmpty());
  }
}
