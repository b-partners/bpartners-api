package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.FREE_TRIAL;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpUserSubscription;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static java.time.ZoneId.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.api.UserSubscriptionApi;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateSubscriptionTrial;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionTrialJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class UserSubscriptionTrialIT extends MockedThirdParties {
  private static final String ESSENTIAL_PLAN_ID = "89f1acdd-c3b9-4717-a21d-355b2021ad58";
  private static final String USAGE_BASED_PLAN_ID = "4219611e-7584-4636-a3c5-ba212600715b";

  @Autowired private UserSubscriptionTrialJpaRepository userSubscriptionTrialJpaRepository;
  @Autowired private UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private SubscriptionProductRepository subscriptionProductRepository;
  @Autowired private UserSubscriptionProductService userSubscriptionProductService;
  @Autowired private CreditService creditService;
  @MockBean private StripeInvoiceService stripeInvoiceServiceMock;

  private UserSubscriptionApi joeUserSubscriptionApi() {
    return new UserSubscriptionApi(TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort));
  }

  private UserAccountsApi joeUserAccountsApi() {
    return new UserAccountsApi(TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort));
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpUserSubscription(subscriptionService);
    when(subscriptionService.getSubscriptionByUser(any()))
        .thenReturn(UserSubscription.builder().subscriptions(List.of()).build());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(any(), any())).thenReturn(List.of());
    clearTrialState();
  }

  @AfterEach
  void tearDown() {
    clearTrialState();
  }

  private void clearTrialState() {
    creditTransactionRepository.deleteAll();
    userSubscriptionTrialJpaRepository.deleteAll();
    userSubscriptionProductJpaRepository.deleteAll();
  }

  @SneakyThrows
  @Test
  void starts_free_trial_grants_credits_and_opens_active_subscription() {
    var plan = subscriptionProductRepository.findById(ESSENTIAL_PLAN_ID).orElseThrow();

    var actual =
        joeUserSubscriptionApi()
            .startUserSubscriptionTrial(
                JOE_DOE_ID,
                new CreateSubscriptionTrial().subscriptionPlanIdentifier(ESSENTIAL_PLAN_ID));

    assertEquals(ESSENTIAL_PLAN_ID, actual.getSubscriptionPlanIdentifier());
    assertNotNull(actual.getStartedAt());
    assertEquals(
        actual.getStartedAt().atZone(of("Europe/Paris")).plusDays(7).toInstant(),
        actual.getExpiresAt());
    assertEquals((int) plan.trialAnalysisGrantedOrDefault(), actual.getGrantedAnalyses());
    assertEquals(plan.trialCreditsGranted(), actual.getGrantedCredits());

    assertTrue(
        userSubscriptionTrialJpaRepository.existsByUserIdAndSubscriptionProductId(
            JOE_DOE_ID, ESSENTIAL_PLAN_ID));
    var activeProduct =
        userSubscriptionProductService.findActiveUserSubscriptionProduct(JOE_DOE_ID).orElseThrow();
    assertEquals(ESSENTIAL_PLAN_ID, activeProduct.getSubscriptionProduct().getId());
    assertNull(activeProduct.getBillingInterval());
    assertNotNull(activeProduct.getSubscriptionEndDatetime());
    assertEquals(
        plan.trialCreditsGranted(),
        creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());

    var user = joeUserAccountsApi().getUserById(JOE_DOE_ID);
    assertEquals(FREE_TRIAL, user.getSubscriptionStatus());
    assertEquals(activeProduct.getSubscriptionStartDatetime(), user.getSubscription().getStart());
    assertEquals(activeProduct.getSubscriptionEndDatetime(), user.getSubscription().getEnd());
  }

  @SneakyThrows
  @Test
  void rejects_a_second_trial_on_the_same_plan() {
    joeUserSubscriptionApi()
        .startUserSubscriptionTrial(
            JOE_DOE_ID,
            new CreateSubscriptionTrial().subscriptionPlanIdentifier(ESSENTIAL_PLAN_ID));

    var conflict =
        assertThrows(
            ApiException.class,
            () ->
                joeUserSubscriptionApi()
                    .startUserSubscriptionTrial(
                        JOE_DOE_ID,
                        new CreateSubscriptionTrial()
                            .subscriptionPlanIdentifier(ESSENTIAL_PLAN_ID)));
    assertEquals(409, conflict.getCode());
  }

  @SneakyThrows
  @Test
  void reports_trial_eligibility_per_plan_before_and_after_starting_a_trial() {
    var before = joeUserSubscriptionApi().getUserSubscriptionTrialEligibility(JOE_DOE_ID);
    assertEquals(
        app.bpartners.api.endpoint.rest.model.SubscriptionTrialIneligibilityReason.ELIGIBLE,
        reasonFor(before, ESSENTIAL_PLAN_ID));
    assertEquals(
        app.bpartners.api.endpoint.rest.model.SubscriptionTrialIneligibilityReason
            .PLAN_HAS_NO_TRIAL,
        reasonFor(before, USAGE_BASED_PLAN_ID));

    joeUserSubscriptionApi()
        .startUserSubscriptionTrial(
            JOE_DOE_ID,
            new CreateSubscriptionTrial().subscriptionPlanIdentifier(ESSENTIAL_PLAN_ID));

    var after = joeUserSubscriptionApi().getUserSubscriptionTrialEligibility(JOE_DOE_ID);
    assertEquals(
        app.bpartners.api.endpoint.rest.model.SubscriptionTrialIneligibilityReason
            .TRIAL_ALREADY_USED,
        reasonFor(after, ESSENTIAL_PLAN_ID));
  }

  private static app.bpartners.api.endpoint.rest.model.SubscriptionTrialIneligibilityReason
      reasonFor(
          java.util.List<app.bpartners.api.endpoint.rest.model.SubscriptionTrialEligibility>
              eligibilities,
          String planId) {
    return eligibilities.stream()
        .filter(e -> planId.equals(e.getSubscriptionPlanIdentifier()))
        .findFirst()
        .orElseThrow()
        .getReason();
  }

  @Test
  void rejects_trial_on_a_plan_without_free_trial() {
    var badRequest =
        assertThrows(
            ApiException.class,
            () ->
                joeUserSubscriptionApi()
                    .startUserSubscriptionTrial(
                        JOE_DOE_ID,
                        new CreateSubscriptionTrial()
                            .subscriptionPlanIdentifier(USAGE_BASED_PLAN_ID)));
    assertEquals(400, badRequest.getCode());
  }

  @SneakyThrows
  @Test
  void user_status_is_active_when_subscribed_even_if_trial_not_finished() {
    var plan = subscriptionProductRepository.findById(ESSENTIAL_PLAN_ID).orElseThrow();
    var now = java.time.Instant.now();
    userSubscriptionProductService.createTrialAssociation(
        JOE_DOE_ID, plan, now, now.plus(7, java.time.temporal.ChronoUnit.DAYS));
    when(subscriptionService.getSubscriptionByUser(any()))
        .thenReturn(
            UserSubscription.builder()
                .subscriptions(
                    List.of(
                        app.bpartners.api.model.subscription.Subscription.builder()
                            .e2Id("stripe_sub_id")
                            .active(true)
                            .status(
                                app.bpartners.api.model.subscription.Subscription.SubscriptionStatus
                                    .ACTIVE)
                            .startDatetime(now)
                            .endDatetime(now.plus(30, java.time.temporal.ChronoUnit.DAYS))
                            .build()))
                .build());

    var user = joeUserAccountsApi().getUserById(JOE_DOE_ID);

    assertEquals(
        app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.ACTIVE,
        user.getSubscriptionStatus());
  }

  @Test
  void rejects_trial_when_user_already_has_an_active_subscription() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    var conflict =
        assertThrows(
            ApiException.class,
            () ->
                joeUserSubscriptionApi()
                    .startUserSubscriptionTrial(
                        JOE_DOE_ID,
                        new CreateSubscriptionTrial()
                            .subscriptionPlanIdentifier(ESSENTIAL_PLAN_ID)));
    assertEquals(409, conflict.getCode());
  }
}
