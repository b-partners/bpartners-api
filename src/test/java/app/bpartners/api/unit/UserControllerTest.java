package app.bpartners.api.unit;

import static app.bpartners.api.endpoint.rest.model.SubscriptionCancellationType.END_OF_PERIOD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.UserController;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserSubscriptionCommitmentRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserSubscriptionPaymentMethodRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionCancellationType;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.validator.CreateSubscriptionInitiationRestValidator;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.StripePortalService;
import app.bpartners.api.service.subscription.StripeSetupService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.ApiKeyService;
import app.bpartners.api.service.user.UserService;
import org.junit.jupiter.api.Test;

class UserControllerTest {
  UserRestMapper userRestMapperMock = mock(UserRestMapper.class);
  CognitoComponent cognitoComponentMock = mock(CognitoComponent.class);
  UserService userServiceMock = mock(UserService.class);
  SubscriptionService subscriptionServiceMock = mock(SubscriptionService.class);
  CreateSubscriptionInitiationRestValidator subscriptionInitiationRestValidatorMock = mock();
  StripePortalService stripePortalServiceMock = mock(StripePortalService.class);
  ApiKeyService apiKeyServiceMock = mock(ApiKeyService.class);
  StripeSetupService stripeSetupServiceMock = mock(StripeSetupService.class);
  UserSubscriptionCommitmentRestMapper userSubscriptionCommitmentRestMapperMock = mock();
  StripePaymentMethodService stripePaymentMethodServiceMock = mock();
  UserSubscriptionPaymentMethodRestMapper userSubscriptionPaymentMethodRestMapperMock = mock();

  UserController subject =
      new UserController(
          userRestMapperMock,
          cognitoComponentMock,
          userServiceMock,
          subscriptionServiceMock,
          subscriptionInitiationRestValidatorMock,
          stripePortalServiceMock,
          apiKeyServiceMock,
          stripeSetupServiceMock,
          userSubscriptionCommitmentRestMapperMock,
          stripePaymentMethodServiceMock,
          userSubscriptionPaymentMethodRestMapperMock);

  @Test
  void cancel_user_subscription_without_cancellation_type() {
    var cancelledUser = mockCancellation("user_id");

    var actual = subject.cancelUserSubscription("user_id", null);

    // An omitted query param reaches the service as null, which applies its IMMEDIATE default.
    verify(subscriptionServiceMock)
        .cancelLatestUserSubscription(eq(domainUser("user_id")), isNull());
    assertEquals(cancelledUser, actual);
  }

  @Test
  void cancel_user_subscription_at_period_end() {
    var cancelledUser = mockCancellation("user_id");

    var actual = subject.cancelUserSubscription("user_id", END_OF_PERIOD);

    verify(subscriptionServiceMock)
        .cancelLatestUserSubscription(eq(domainUser("user_id")), eq(END_OF_PERIOD));
    assertEquals(cancelledUser, actual);
  }

  private User mockCancellation(String userId) {
    var domainUser = domainUser(userId);
    var cancelledUser = new User().id(userId);
    when(userServiceMock.getUserById(userId)).thenReturn(domainUser);
    when(subscriptionServiceMock.cancelLatestUserSubscription(
            eq(domainUser), any(SubscriptionCancellationType.class)))
        .thenReturn(UserSubscription.builder().user(domainUser).build());
    when(subscriptionServiceMock.cancelLatestUserSubscription(eq(domainUser), isNull()))
        .thenReturn(UserSubscription.builder().user(domainUser).build());
    when(userRestMapperMock.toRest(domainUser)).thenReturn(cancelledUser);
    return cancelledUser;
  }

  private static app.bpartners.api.model.User domainUser(String userId) {
    return app.bpartners.api.model.User.builder().id(userId).build();
  }
}
