package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
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

import app.bpartners.api.endpoint.rest.api.CreditsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionMovementType;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import java.time.Instant;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreditTransactionIT extends MockedThirdParties {
  @Autowired private CreditTransactionRepository creditTransactionRepository;

  private final Instant now = Instant.now().truncatedTo(MILLIS);

  private CreditsApi joeCreditsApi() {
    ApiClient joeClient = TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
    return new CreditsApi(joeClient);
  }

  private String persist(
      CreditTransactionType type,
      CreditTransactionMovementType movementType,
      long credits,
      Instant creationDatetime) {
    return creditTransactionRepository
        .save(
            CreditTransaction.builder()
                .id(randomUUID().toString())
                .userId(JOE_DOE_ID)
                .type(type)
                .movementType(movementType)
                .credits(credits)
                .creationDatetime(creationDatetime)
                .build())
        .getId();
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
  void list_returns_all_transactions_most_recent_first() {
    persist(SUBSCRIPTION_GRANT, CREDIT, 20L, now.minus(2, DAYS));
    persist(PURCHASE, CREDIT, 30L, now.minus(1, DAYS));
    persist(CONSUMPTION, DEBIT, 5L, now);

    var actual = joeCreditsApi().getCreditTransactions(JOE_DOE_ID, null, null, null, null, null);

    assertEquals(3, actual.size());
    assertEquals(
        List.of(
            app.bpartners.api.endpoint.rest.model.CreditTransactionType.CONSUMPTION,
            app.bpartners.api.endpoint.rest.model.CreditTransactionType.PURCHASE,
            app.bpartners.api.endpoint.rest.model.CreditTransactionType.SUBSCRIPTION_GRANT),
        actual.stream().map(t -> t.getType()).toList());
  }

  @SneakyThrows
  @Test
  void list_filters_by_type() {
    persist(SUBSCRIPTION_GRANT, CREDIT, 20L, now.minus(2, DAYS));
    persist(PURCHASE, CREDIT, 30L, now.minus(1, DAYS));
    persist(CONSUMPTION, DEBIT, 5L, now);

    var actual =
        joeCreditsApi()
            .getCreditTransactions(
                JOE_DOE_ID,
                List.of(app.bpartners.api.endpoint.rest.model.CreditTransactionType.PURCHASE),
                null,
                null,
                null,
                null);

    assertEquals(1, actual.size());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditTransactionType.PURCHASE,
        actual.getFirst().getType());
    assertEquals(30L, actual.getFirst().getCredits());
  }

  @SneakyThrows
  @Test
  void get_single_transaction_by_id() {
    var purchaseId = persist(PURCHASE, CREDIT, 30L, now.minus(1, DAYS));

    var actual = joeCreditsApi().getCreditTransactionById(JOE_DOE_ID, purchaseId);

    assertEquals(purchaseId, actual.getId());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditTransactionType.PURCHASE, actual.getType());
    assertEquals(
        app.bpartners.api.endpoint.rest.model.CreditTransactionMovementType.CREDIT,
        actual.getMovementType());
    assertEquals(30L, actual.getCredits());
    assertNull(actual.getLabel());
  }

  @Test
  void get_unknown_transaction_returns_not_found() {
    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\"CreditTransaction(id=unknown) not found for"
            + " User.id=joe_doe_id\"}",
        () -> joeCreditsApi().getCreditTransactionById(JOE_DOE_ID, "unknown"));
  }
}
