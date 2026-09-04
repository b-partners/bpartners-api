package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
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
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.subscription.UserSubscription;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NewUserSubscriptionSecurityIT extends MockedThirdParties {

  private ApiClient joeClient() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
  }

  @BeforeEach
  void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpLegalFileRepository(legalFileRepositoryMock);
    UserSubscription expiredSubscription = mock(UserSubscription.class);
    when(expiredSubscription.hasValidSubscription()).thenReturn(false);
    when(subscriptionService.getSubscriptionByUserId(any())).thenReturn(expiredSubscription);
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
  void new_user_without_valid_subscription_can_get_subscription_commitments() throws ApiException {
    var actual =
        new UserSubscriptionApi(joeClient()).getUserSubscriptionCommitments(JOE_DOE_ID, null, null);

    assertEquals(List.of(), actual);
  }

  @Test
  void new_user_without_valid_subscription_forbidden_on_non_exempt_endpoint() {
    UserAccountsApi api = new UserAccountsApi(joeClient());

    ApiException forbidden =
        assertThrows(
            ApiException.class, () -> api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID));
    assertEquals(403, forbidden.getCode());
  }
}
