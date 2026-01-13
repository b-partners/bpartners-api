package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.exception.UnapprovedLegalFileException;
import app.bpartners.api.endpoint.rest.security.exception.UserSubscriptionExpiredException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.jpa.UserApiKeyFullAuthorizationJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.LegalFileService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Primary
@Component
@AllArgsConstructor
public class UsernamePasswordAuthenticatorFacade implements UsernamePasswordAuthenticator {
  private final BearerAuthenticator bearerAuthenticator;
  private final ApiKeyAuthenticator apiKeyAuthenticator;
  private final LegalFileService legalFileService;
  private final SubscriptionService subscriptionService;
  private final UserApiKeyFullAuthorizationJpaRepository userApiKeyFullAuthorizationJpaRepository;
  private final UserSubscriptionEligibleJpaRepository userSubscriptionEligibleJpaRepository;

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
    var userNotWhiteListed =
        userApiKeyFullAuthorizationJpaRepository.findByIdUser(user.getId()).isEmpty();
    if (!user.isPaymentMethodExists() && userNotWhiteListed) {
      userSubscriptionEligibleJpaRepository
          .findByUserId(user.getId())
          .ifPresent(
              value -> {
                if (!value.hasFreeTrialPeriodActive()) {
                  throw new ForbiddenException(
                      "User.id="
                          + user.getId()
                          + " does not have any payment method."
                          + " Add a new one through billing portal redirection");
                }
              });
    }
    var userSubscription = subscriptionService.getSubscriptionByUserId(user.getId());
    if (!userSubscription.hasValidSubscription() && userNotWhiteListed) {
      throw new UserSubscriptionExpiredException(
          "User.id=" + user.getId() + " does not have a valid subscription or free trial expired");
    }

    return principal;
  }

  @Override
  public User retrieveUserWithoutLegalFileCheck(HttpServletRequest request) {
    try {
      return bearerAuthenticator.retrieveUserWithoutLegalFileCheck(request);
    } catch (AuthenticationException ignored) {
      return apiKeyAuthenticator.retrieveUserWithoutLegalFileCheck(request);
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
