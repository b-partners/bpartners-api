package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static app.bpartners.api.model.subscription.SecondaryUserStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SecondaryUserStatus.INVITED;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.CreditsApi;
import app.bpartners.api.endpoint.rest.api.UserSubscriptionApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateSubscriptionUpgrade;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.model.subscription.SecondaryUser;
import app.bpartners.api.model.subscription.SecondaryUserStatus;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.SecondaryUserRepository;
import app.bpartners.api.service.credit.CreditService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SecondaryUserIT extends MockedThirdParties {
  @Autowired private SecondaryUserRepository secondaryUserRepository;
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private CreditService creditService;

  private ApiClient clientOf(String token) {
    return TestUtils.anApiClient(token, null, localPort);
  }

  private void linkJaneToJoe(SecondaryUserStatus status) {
    secondaryUserRepository.save(
        SecondaryUser.builder()
            .id(randomUUID().toString())
            .primaryUserId(JOE_DOE_ID)
            .secondaryUserId(ACTIVE.equals(status) ? JANE_DOE_ID : null)
            .invitedEmail(TestUtils.JANE_EMAIL)
            .status(status)
            .creationDatetime(Instant.now())
            .updateDatetime(Instant.now())
            .build());
  }

  private void grantJoeCredits(long credits) {
    creditTransactionRepository.save(
        CreditTransaction.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(SUBSCRIPTION_GRANT)
            .movementType(CREDIT)
            .credits(credits)
            .creationDatetime(Instant.now().minus(1, DAYS))
            .build());
  }

  private static int statusOf(org.junit.jupiter.api.function.Executable executable) {
    return assertThrows(ApiException.class, executable).getCode();
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpUserSubscription(subscriptionService);
    secondaryUserRepository.deleteAll();
    creditTransactionRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
    secondaryUserRepository.deleteAll();
    creditTransactionRepository.deleteAll();
  }

  @SneakyThrows
  @Test
  void secondary_user_reads_the_credit_balance_of_the_primary_user() {
    var costPerAnalysis = creditService.getCreditBalance(JOE_DOE_ID).getCreditCostPerAnalysis();
    grantJoeCredits(costPerAnalysis * 3);
    linkJaneToJoe(ACTIVE);

    var janeBalance = new CreditsApi(clientOf(JANE_DOE_TOKEN)).getCreditBalance(JANE_DOE_ID);
    var joeBalance = new CreditsApi(clientOf(JOE_DOE_TOKEN)).getCreditBalance(JOE_DOE_ID);

    assertEquals(costPerAnalysis * 3, janeBalance.getSpendableCredits());
    assertEquals(joeBalance.getSpendableCredits(), janeBalance.getSpendableCredits());
  }

  @Test
  void secondary_user_analysis_debits_the_primary_user_wallet() {
    var costPerAnalysis = creditService.getCreditBalance(JOE_DOE_ID).getCreditCostPerAnalysis();
    grantJoeCredits(costPerAnalysis * 3);
    linkJaneToJoe(ACTIVE);

    creditService.consumeRoofAnalysis(JANE_DOE_ID, "Analyse toiture : zone");

    var debit =
        creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
            .filter(transaction -> !transaction.isCredit())
            .findFirst()
            .orElseThrow();
    assertEquals(JOE_DOE_ID, debit.getUserId());
    assertEquals(JANE_DOE_ID, debit.getActorUserId());
    assertEquals(costPerAnalysis, debit.getCredits());
    assertEquals(
        costPerAnalysis * 2, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
    assertTrue(creditTransactionRepository.findAllByUserId(JANE_DOE_ID).isEmpty());
  }

  @Test
  void user_without_primary_user_is_debited_from_own_wallet() {
    var costPerAnalysis = creditService.getCreditBalance(JOE_DOE_ID).getCreditCostPerAnalysis();
    grantJoeCredits(costPerAnalysis * 3);

    creditService.consumeRoofAnalysis(JOE_DOE_ID, "Analyse toiture : zone");

    var debit =
        creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
            .filter(transaction -> !transaction.isCredit())
            .findFirst()
            .orElseThrow();
    assertEquals(JOE_DOE_ID, debit.getActorUserId());
  }

  @Test
  void concurrent_analyses_of_primary_and_secondary_users_share_one_wallet() throws Exception {
    var costPerAnalysis = creditService.getCreditBalance(JOE_DOE_ID).getCreditCostPerAnalysis();
    grantJoeCredits(costPerAnalysis * 2);
    linkJaneToJoe(ACTIVE);
    var executor = Executors.newFixedThreadPool(4);
    List<Callable<Boolean>> analyses = new ArrayList<>();
    for (var userId : List.of(JOE_DOE_ID, JANE_DOE_ID, JOE_DOE_ID, JANE_DOE_ID)) {
      analyses.add(
          () -> {
            try {
              creditService.consumeRoofAnalysis(userId, "Analyse toiture : zone");
              return true;
            } catch (InsufficientCreditsException e) {
              return false;
            }
          });
    }

    var results = executor.invokeAll(analyses);
    executor.shutdown();

    var succeeded = 0;
    for (Future<Boolean> result : results) {
      if (result.get()) {
        succeeded++;
      }
    }
    assertEquals(2, succeeded);
    assertEquals(0L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
    assertEquals(
        2L,
        creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
            .filter(transaction -> !transaction.isCredit())
            .count());
  }

  @Test
  void secondary_user_is_forbidden_on_billing_routes() {
    linkJaneToJoe(ACTIVE);
    var janeClient = clientOf(JANE_DOE_TOKEN);
    var janeCredits = new CreditsApi(janeClient);
    var janeSubscription = new UserSubscriptionApi(janeClient);

    assertEquals(
        403,
        statusOf(() -> janeCredits.getCreditPurchases(JANE_DOE_ID, null, null, null, null, null)));
    assertEquals(
        403,
        statusOf(() -> janeSubscription.getUserSubscriptionCommitments(JANE_DOE_ID, null, null)));
    assertEquals(
        403, statusOf(() -> janeSubscription.getUserSubscriptionUpgrades(JANE_DOE_ID, null)));
    assertEquals(403, statusOf(() -> janeSubscription.cancelUserSubscription(JANE_DOE_ID, null)));
    assertEquals(
        403,
        statusOf(
            () ->
                janeSubscription.upgradeUserSubscription(
                    JANE_DOE_ID, new CreateSubscriptionUpgrade().subscriptionPlanIdentifier("x"))));
  }

  @SneakyThrows
  @Test
  void user_without_primary_user_can_still_use_billing_routes() {
    var joeCredits = new CreditsApi(clientOf(JOE_DOE_TOKEN));

    var actual = joeCredits.getCreditPurchases(JOE_DOE_ID, null, null, null, null, null);

    assertNotEquals(null, actual);
  }

  @Test
  void plan_change_is_rejected_while_an_active_secondary_user_exists() {
    linkJaneToJoe(ACTIVE);
    var joeSubscription = new UserSubscriptionApi(clientOf(JOE_DOE_TOKEN));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                joeSubscription.upgradeUserSubscription(
                    JOE_DOE_ID, new CreateSubscriptionUpgrade().subscriptionPlanIdentifier("x")));

    assertEquals(400, exception.getCode());
    assertTrue(exception.getResponseBody().contains("cannot be changed"));
  }

  @Test
  void plan_change_is_rejected_while_an_invitation_is_pending() {
    linkJaneToJoe(INVITED);
    var joeSubscription = new UserSubscriptionApi(clientOf(JOE_DOE_TOKEN));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                joeSubscription.upgradeUserSubscription(
                    JOE_DOE_ID, new CreateSubscriptionUpgrade().subscriptionPlanIdentifier("x")));

    assertEquals(400, exception.getCode());
  }

  @Test
  void plan_change_is_not_blocked_without_secondary_users() {
    var joeSubscription = new UserSubscriptionApi(clientOf(JOE_DOE_TOKEN));

    var exception =
        assertThrows(
            ApiException.class,
            () ->
                joeSubscription.upgradeUserSubscription(
                    JOE_DOE_ID, new CreateSubscriptionUpgrade().subscriptionPlanIdentifier("x")));

    assertTrue(
        exception.getResponseBody() == null
            || !exception.getResponseBody().contains("secondary users exist"));
  }
}
