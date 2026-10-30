package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.UsernamePasswordAuthenticator;
import app.bpartners.api.service.subscription.PrimaryUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class WhoamiController {
  private final UserRestMapper userRestMapper;
  private UsernamePasswordAuthenticator authenticator;
  private PrimaryUserResolver primaryUserResolver;

  @GetMapping("/whoami")
  public Whoami whoami(HttpServletRequest request) {
    var authUser = authenticator.retrieveUserWithoutLegalFileCheck(request);
    var primaryUserId = primaryUserResolver.resolve(authUser.getId());
    return new Whoami()
        .user(userRestMapper.toRest(authUser))
        .primaryUserId(primaryUserId.equals(authUser.getId()) ? null : primaryUserId);
  }
}
