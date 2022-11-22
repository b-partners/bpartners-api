package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.exception.UnapprovedLegalFileException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.User;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.LegalFileService;
import app.bpartners.api.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@Component
@AllArgsConstructor
public class AuthProvider extends AbstractUserDetailsAuthenticationProvider {

  private final SwanComponent swanComponent;
  private final UserService userService;
  private final AccountService accountService;
  private final LegalFileService legalFileService;

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken token) {
    // nothing
  }

  @Override
  protected UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    String bearer = getBearer(usernamePasswordAuthenticationToken);
    if (bearer == null) {
      throw new UsernameNotFoundException("Bad credentials"); // NOSONAR
    }
    String swanUserId = swanComponent.getSwanUserIdByToken(bearer);
    if (swanUserId == null) {
      throw new UsernameNotFoundException("Bad credentials"); // NOSONAR
    }
    User user = userService.getUserByIdAndBearer(swanUserId, bearer);
    LegalFile legalFile = legalFileService.getLatestLegalFileByUserId(user.getId());
    if (!legalFile.isApproved()) {
      throw new UnapprovedLegalFileException(
          "User." + user.getId() + " has not approved the legal file " + legalFile.getName());
    }
    Account account = accountService.getAccountByBearer(bearer);
    return new Principal(user, account, bearer);
  }

  private String getBearer(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String) || !((String) tokenObject).startsWith(BEARER_PREFIX)) {
      return null;
    }
    return ((String) tokenObject).substring(BEARER_PREFIX.length()).trim();
  }

  public static Principal getPrincipal() {
    SecurityContext context = SecurityContextHolder.getContext();
    Authentication authentication = context.getAuthentication();
    return (Principal) authentication.getPrincipal();
  }
}
