package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.model.WhiteListScope.PAYMENT_METHOD_NOT_REQUIRED;
import static app.bpartners.api.model.WhiteListScope.SUBSCRIPTION_VALIDATION_NOT_REQUIRED;
import static org.springframework.http.HttpMethod.*;

import app.bpartners.api.endpoint.rest.security.exception.NoPaymentMethodFoundException;
import app.bpartners.api.endpoint.rest.security.exception.UnapprovedLegalFileException;
import app.bpartners.api.endpoint.rest.security.exception.UserSubscriptionExpiredException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.LegalFileService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

@Primary
@Component
@AllArgsConstructor
public class UsernamePasswordAuthenticatorFacade implements UsernamePasswordAuthenticator {
  private static final RequestMatcher SUBSCRIPTION_VALIDATION_EXEMPTED_MATCHER =
      new OrRequestMatcher(
          new AntPathRequestMatcher("/creditPacks", GET.name()),
          new AntPathRequestMatcher("/creditPacks/*", GET.name()),
          new AntPathRequestMatcher("/users/*/creditBalance", GET.name()),
          new AntPathRequestMatcher("/users/*/paymentMethods", GET.name()),
          new AntPathRequestMatcher("/users/*/paymentMethods", PUT.name()),
          new AntPathRequestMatcher("/users/*/subscriptionCommitments", GET.name()),
          new AntPathRequestMatcher("/users/*/subscriptionCommitments", POST.name()));

  private final BearerAuthenticator bearerAuthenticator;
  private final ApiKeyAuthenticator apiKeyAuthenticator;
  private final LegalFileService legalFileService;
  private final SubscriptionService subscriptionService;
  private final UserWhiteListedJpaRepository userWhiteListedJpaRepository;
  private final UserSubscriptionEligibleJpaRepository userSubscriptionEligibleJpaRepository;
  private final CreditService creditService;

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authenticationToken) {
    Principal principal;
    try {
      principal = (Principal) bearerAuthenticator.retrieveUser(username, authenticationToken);
    } catch (AuthenticationException ignored) {
      principal = (Principal) apiKeyAuthenticator.retrieveUser(username, authenticationToken);
    }
    var user = principal.getUser();

    List<LegalFile> legalFilesList =
        legalFileService.getAllToBeApprovedLegalFilesByUserId(user.getId());
    checkLegalFiles(legalFilesList, user);
    if (!isSubscriptionValidationExempted(authenticationToken)) {
      validateSubscriptionRequirements(user);
    }

    return principal;
  }

  private static boolean isSubscriptionValidationExempted(
      UsernamePasswordAuthenticationToken authenticationToken) {
    return authenticationToken.getDetails() instanceof HttpServletRequest request
        && SUBSCRIPTION_VALIDATION_EXEMPTED_MATCHER.matches(request);
  }

  private void validateSubscriptionRequirements(User user) {
    var userNotRequiredPaymentMethod = false;
    var userNotRestrictedBySubscriptionStatus = false;
    var userWhiteListed = userWhiteListedJpaRepository.findByUserId(user.getId()).orElse(null);
    if (userWhiteListed != null) {
      userNotRequiredPaymentMethod =
          userWhiteListed.getScopes().contains(PAYMENT_METHOD_NOT_REQUIRED);
      userNotRestrictedBySubscriptionStatus =
          userWhiteListed.getScopes().contains(SUBSCRIPTION_VALIDATION_NOT_REQUIRED);
    }
    if (!user.isPaymentMethodExists() && !userNotRequiredPaymentMethod) {
      userSubscriptionEligibleJpaRepository
          .findByUserId(user.getId())
          .ifPresent(
              value -> {
                throw new NoPaymentMethodFoundException(
                    "User.id="
                        + user.getId()
                        + " does not have any payment method."
                        + " Add a new one through billing portal redirection");
              });
    }
    var userSubscription = subscriptionService.getSubscriptionByUserId(user.getId());
    if (!userSubscription.hasValidSubscription()
        && !userNotRestrictedBySubscriptionStatus
        && !userHasSpendableCredits(user)) {
      throw new UserSubscriptionExpiredException(
          "User.id=" + user.getId() + " does not have a valid subscription or free trial expired");
    }
  }

  private boolean userHasSpendableCredits(User user) {
    return creditService.getCreditBalance(user.getId()).getSpendableCredits() > 0;
  }

  @Override
  public User retrieveUserWithoutLegalFileCheck(HttpServletRequest request) {
    try {
      return bearerAuthenticator.retrieveUserWithoutLegalFileCheck(request);
    } catch (AuthenticationException ignored) {
      try {
        return apiKeyAuthenticator.retrieveUserWithoutLegalFileCheck(request);
      } catch (AuthenticationException e) {
        throw new ForbiddenException("Either api key or bearer token is not valid");
      }
    }
  }

  private void checkLegalFiles(List<LegalFile> legalFiles, User user) {
    if (!legalFiles.isEmpty()) {
      StringBuilder exceptionMessageBuilder = new StringBuilder();
      legalFiles.forEach(
          legalFile -> {
            if (!legalFile.isApproved()) {
              exceptionMessageBuilder
                  .append("User.")
                  .append(user.getId())
                  .append(" has not approved the legal file ")
                  .append(legalFile.getName());
            }
          });
      String exceptionMessage = exceptionMessageBuilder.toString();
      if (!exceptionMessage.isEmpty()) {
        throw new UnapprovedLegalFileException(exceptionMessage);
      }
    }
  }
}
