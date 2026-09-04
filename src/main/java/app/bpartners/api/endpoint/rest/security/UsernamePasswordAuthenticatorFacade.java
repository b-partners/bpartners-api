package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.exception.UnapprovedLegalFileException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
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

    return principal;
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
