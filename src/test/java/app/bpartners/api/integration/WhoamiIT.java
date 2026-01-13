package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.IdentificationStatus.VALID_IDENTITY;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionStatus.EMPTY;
import static app.bpartners.api.integration.UserIT.userSubscriptionMaker;
import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.SecurityApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import com.stripe.model.PaymentMethod;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
class WhoamiIT extends MockedThirdParties {
  @MockBean StripeInvoiceService stripeInvoiceServiceMock;

  private ApiClient anApiClientWithBearer() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
  }

  private ApiClient anApiClientWithApiKey() {
    return TestUtils.anApiClient(null, JOE_DOE_API_KEY, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);

    var defaultUserSubscription = userSubscriptionMaker(false);
    when(subscriptionService.getSubscriptionByUser(any())).thenReturn(defaultUserSubscription);
    when(subscriptionService.getSubscriptionByUserId(any())).thenReturn(defaultUserSubscription);
    when(stripePaymentMethodServiceMock.getPaymentMethod(any()))
        .thenReturn(List.of(mock(PaymentMethod.class)));
  }

  @Test
  void whoami_with_bearer() throws ApiException {
    ApiClient client = anApiClientWithBearer();
    SecurityApi api = new SecurityApi(client);

    var actual = api.whoami();

    assertEquals(restJoeDoeUser(), actual.getUser().roles(List.of()));
  }

  @Test
  void whoami_with_api_key() throws ApiException {
    ApiClient client = anApiClientWithApiKey();
    SecurityApi api = new SecurityApi(client);

    var actual = api.whoami();

    assertEquals(restJoeDoeUser(), actual.getUser().roles(List.of()));
  }

  private static User restJoeDoeUser() {
    return new User()
        .id(JOE_DOE_ID)
        .firstName("Joe")
        .lastName("Doe")
        .idVerified(true)
        .identificationStatus(VALID_IDENTITY)
        .phone("+261340465338")
        .monthlySubscriptionAmount(5)
        .logoFileId("logo.jpeg")
        .status(ENABLED)
        .activeAccount(restJoeAccount())
        .roles(List.of())
        .subscriptionStatus(EMPTY)
        .subscription(
            new app.bpartners.api.endpoint.rest.model.UserSubscription()
                .status(EMPTY)
                .end(null)
                .start(null));
  }

  private static app.bpartners.api.endpoint.rest.model.Account restJoeAccount() {
    return new app.bpartners.api.endpoint.rest.model.Account()
        .id("other_joe_account_id")
        .availableBalance(0)
        .status(OPENED)
        .active(true)
        .name("Other joe account")
        .iban("Other iban")
        .bic("Other bic");
  }
}
