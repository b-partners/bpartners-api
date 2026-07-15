package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.security.BearerAuthenticator;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoConf;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class AuthenticationIT extends MockedThirdParties {
  @MockBean private CognitoConf cognitoConf;
  @MockBean private BearerAuthenticator bearerAuthenticatorMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, null, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
    when(bearerAuthenticatorMock.retrieveUser(any(), any()))
        .thenReturn(new Principal(User.builder().id(JOE_DOE_ID).build(), JOE_DOE_TOKEN));
  }

  @Test
  void user_has_not_approved_legal_file_forbidden() {
    reset(legalFileRepositoryMock);
    when(legalFileRepositoryMock.findAllToBeApprovedLegalFilesByUserId(JOE_DOE_ID))
        .thenReturn(List.of(domainLegalFile()));
    ApiClient joeDoeClient = anApiClient();
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\""
            + "User.joe_doe_id has not approved the legal file cgu_20-11-23.pdf\"}",
        () -> api.getAccountHolders(JOE_DOE_ID, JOE_DOE_ACCOUNT_ID));
  }
}
