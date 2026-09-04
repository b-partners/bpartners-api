package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.CreditsApi;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.api.UserSubscriptionApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateUserSubscriptionCommitment;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.subscription.StripeSetupService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NewUserSubscriptionSecurityIT extends MockedThirdParties {
  private static final String SEEDED_SUBSCRIPTION_PLAN_ID = "aef20fa7-5d85-4900-bad5-ee121be69ef3";

  @MockBean private StripeSetupService stripeSetupServiceMock;
  @Autowired private CreditTransactionRepository creditTransactionRepository;

  private ApiClient joeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
  }

  private void persistJoePurchasedCredits(long credits) {
    creditTransactionRepository.save(
        CreditTransaction.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(PURCHASE)
            .movementType(CREDIT)
            .credits(credits)
            .expirationDatetime(null)
            .creationDatetime(now())
            .build());
  }

  private static CreateUserSubscriptionCommitment aCreateUserSubscriptionCommitment() {
    return new CreateUserSubscriptionCommitment()
        .subscriptionPlanIdentifier(SEEDED_SUBSCRIPTION_PLAN_ID)
        .duration(UserSubscriptionCommitmentDuration.TWELVE_MONTHS)
        .commitmentStart(now())
        .approvalDatetime(now());
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    creditTransactionRepository.deleteAll();
    UserSubscription expiredSubscription = mock(UserSubscription.class);
    when(expiredSubscription.hasValidSubscription()).thenReturn(false);
    when(subscriptionService.getSubscriptionByUserId(any())).thenReturn(expiredSubscription);
    when(stripeSetupServiceMock.setupReplacementCheckoutSession(any(), any()))
        .thenReturn(new Redirection());
  }

  @Test
  void new_user_without_valid_subscription_can_get_credit_balance() throws ApiException {
    var actual = new CreditsApi(joeClient()).getCreditBalance(JOE_DOE_ID);

    assertNotNull(actual);
  }

  @Test
  void new_user_without_valid_subscription_can_list_credit_packs() throws ApiException {
    var actual = new CreditsApi(joeClient()).getCreditPacks(null, null);

    assertNotNull(actual);
  }

  @Test
  void new_user_without_valid_subscription_can_get_credit_pack_by_id() throws ApiException {
    var creditsApi = new CreditsApi(joeClient());
    var existingPackId = creditsApi.getCreditPacks(null, null).getFirst().getId();

    var actual = creditsApi.getCreditPackById(existingPackId);

    assertNotNull(actual);
  }

  @Test
  void new_user_without_valid_subscription_can_get_payment_methods() throws ApiException {
    var actual = new UserSubscriptionApi(joeClient()).getUserPaymentMethods(JOE_DOE_ID, true);

    assertNotNull(actual);
  }

  @Test
  void new_user_without_valid_subscription_can_initiate_payment_method_replacement()
      throws ApiException {
    var actual =
        new UserSubscriptionApi(joeClient())
            .initiatePaymentMethodReplacement(
                JOE_DOE_ID,
                new RedirectionStatusUrls()
                    .successUrl("https://success.url")
                    .failureUrl("https://failure.url"));

    assertNotNull(actual);
  }

  @Test
  void new_user_without_valid_subscription_can_get_subscription_commitments() throws ApiException {
    var actual =
        new UserSubscriptionApi(joeClient()).getUserSubscriptionCommitments(JOE_DOE_ID, null, null);

    assertEquals(List.of(), actual);
  }

  @Test
  void new_user_without_valid_subscription_can_save_subscription_commitments() throws ApiException {
    var actual =
        new UserSubscriptionApi(joeClient())
            .saveUserSubscriptionCommitments(
                JOE_DOE_ID, List.of(aCreateUserSubscriptionCommitment()));

    assertNotNull(actual);
  }

  @Test
  void new_user_without_valid_subscription_forbidden_on_non_exempt_endpoint() {
    UserAccountsApi api = new UserAccountsApi(joeClient());

    ApiException forbidden =
        assertThrows(
            ApiException.class, () -> api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID));
    assertEquals(403, forbidden.getCode());
  }

  @Test
  void user_with_spendable_credits_but_no_valid_subscription_passes_non_exempt_endpoint()
      throws ApiException {
    persistJoePurchasedCredits(10L);
    UserAccountsApi api = new UserAccountsApi(joeClient());

    var actual = api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertNotNull(actual);
  }
}
