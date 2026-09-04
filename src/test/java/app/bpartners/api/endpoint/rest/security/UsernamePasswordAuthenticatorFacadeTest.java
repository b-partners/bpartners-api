package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.model.WhiteListScope.PAYMENT_METHOD_NOT_REQUIRED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.security.exception.NoPaymentMethodFoundException;
import app.bpartners.api.endpoint.rest.security.exception.UserSubscriptionExpiredException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserWhiteListed;
import app.bpartners.api.model.credit.CreditBalance;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.LegalFileService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class UsernamePasswordAuthenticatorFacadeTest {
  BearerAuthenticator bearerAuthenticatorMock = mock();
  ApiKeyAuthenticator apiKeyAuthenticatorMock = mock();
  LegalFileService legalServiceMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  UserSubscriptionEligibleJpaRepository userSubscriptionEligibleJpaRepositoryMock = mock();
  UserWhiteListedJpaRepository userWhiteListedJpaRepositoryMock =
      mock(UserWhiteListedJpaRepository.class);
  CreditService creditServiceMock = mock();
  UsernamePasswordAuthenticatorFacade subject =
      new UsernamePasswordAuthenticatorFacade(
          bearerAuthenticatorMock,
          apiKeyAuthenticatorMock,
          legalServiceMock,
          subscriptionServiceMock,
          userWhiteListedJpaRepositoryMock,
          userSubscriptionEligibleJpaRepositoryMock,
          creditServiceMock);

  @Test
  void throw_exception_when_user_does_not_have_payment_method_and_not_whitelisted() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.isPaymentMethodExists()).thenReturn(false);
    when(principalMock.getUser()).thenReturn(userMock);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
    when(userSubscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));

    var actualException =
        assertThrows(
            NoPaymentMethodFoundException.class,
            () -> subject.retrieveUser(username, authenticationTokenMock));

    assertEquals(
        "User.id="
            + userId
            + " does not have any payment method. Add a new one through billing portal redirection",
        actualException.getMessage());
    verify(subscriptionServiceMock, never()).getSubscriptionByUserId(any());
  }

  @Test
  void throw_exception_user_without_payment_method_and_not_whitelisted_even_still_trial_period() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    var userSubscriptionMock = mock(UserSubscription.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.isPaymentMethodExists()).thenReturn(false);
    when(principalMock.getUser()).thenReturn(userMock);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(true);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
    when(userSubscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(subscriptionServiceMock.getSubscriptionByUserId(userId)).thenReturn(userSubscriptionMock);

    var actualException =
        assertThrows(
            NoPaymentMethodFoundException.class,
            () -> subject.retrieveUser(username, authenticationTokenMock));

    assertEquals(
        "User.id="
            + userId
            + " does not have any payment method. Add a new one through billing portal redirection",
        actualException.getMessage());
  }

  @Test
  void do_nothing_user_does_not_have_payment_method_with_trial_period_expired_but_whitelisted() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    var userSubscriptionMock = mock(UserSubscription.class);
    var userWhiteListedMock = mock(UserWhiteListed.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.isPaymentMethodExists()).thenReturn(false);
    when(principalMock.getUser()).thenReturn(userMock);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());
    when(userWhiteListedMock.getScopes()).thenReturn(List.of(PAYMENT_METHOD_NOT_REQUIRED));
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userWhiteListedMock));
    when(userSubscriptionEligibleJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userSubscriptionEligibleMock));
    when(subscriptionServiceMock.getSubscriptionByUserId(userId)).thenReturn(userSubscriptionMock);

    assertDoesNotThrow(() -> subject.retrieveUser(username, authenticationTokenMock));
  }

  @Test
  void do_nothing_user_without_valid_subscription_but_with_spendable_credits() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);
    var userSubscriptionMock = mock(UserSubscription.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.isPaymentMethodExists()).thenReturn(true);
    when(principalMock.getUser()).thenReturn(userMock);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(false);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
    when(subscriptionServiceMock.getSubscriptionByUserId(userId)).thenReturn(userSubscriptionMock);
    when(creditServiceMock.getCreditBalance(userId))
        .thenReturn(CreditBalance.builder().spendableCredits(10L).build());

    assertDoesNotThrow(() -> subject.retrieveUser(username, authenticationTokenMock));
  }

  @Test
  void throw_exception_user_without_valid_subscription_and_no_spendable_credits() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);
    var userSubscriptionMock = mock(UserSubscription.class);

    when(userMock.getId()).thenReturn(userId);
    when(userMock.isPaymentMethodExists()).thenReturn(true);
    when(principalMock.getUser()).thenReturn(userMock);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(false);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
    when(subscriptionServiceMock.getSubscriptionByUserId(userId)).thenReturn(userSubscriptionMock);
    when(creditServiceMock.getCreditBalance(userId))
        .thenReturn(CreditBalance.builder().spendableCredits(0L).build());

    assertThrows(
        UserSubscriptionExpiredException.class,
        () -> subject.retrieveUser(username, authenticationTokenMock));
  }

  @Test
  void do_nothing_when_getting_credit_purchases_even_without_valid_subscription() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);

    when(userMock.getId()).thenReturn(userId);
    when(principalMock.getUser()).thenReturn(userMock);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());
    when(authenticationTokenMock.getDetails())
        .thenReturn(request("GET", "/users/" + userId + "/creditPurchases"));

    assertDoesNotThrow(() -> subject.retrieveUser(username, authenticationTokenMock));
    verify(subscriptionServiceMock, never()).getSubscriptionByUserId(any());
    verify(creditServiceMock, never()).getCreditBalance(any());
  }

  @Test
  void do_nothing_when_updating_a_credit_purchase_even_without_valid_subscription() {
    var username = randomUUID().toString();
    var userId = randomUUID().toString();
    var authenticationTokenMock = mock(UsernamePasswordAuthenticationToken.class);
    var principalMock = mock(Principal.class);
    var userMock = mock(User.class);

    when(userMock.getId()).thenReturn(userId);
    when(principalMock.getUser()).thenReturn(userMock);
    when(bearerAuthenticatorMock.retrieveUser(username, authenticationTokenMock))
        .thenReturn(principalMock);
    when(legalServiceMock.getAllToBeApprovedLegalFilesByUserId(userId)).thenReturn(List.of());
    when(authenticationTokenMock.getDetails())
        .thenReturn(request("PUT", "/users/" + userId + "/creditPurchases/" + randomUUID()));

    assertDoesNotThrow(() -> subject.retrieveUser(username, authenticationTokenMock));
    verify(subscriptionServiceMock, never()).getSubscriptionByUserId(any());
    verify(creditServiceMock, never()).getCreditBalance(any());
  }

  private static MockHttpServletRequest request(String method, String servletPath) {
    var request = new MockHttpServletRequest();
    request.setMethod(method);
    request.setServletPath(servletPath);
    return request;
  }
}
