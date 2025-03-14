package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.UsernamePasswordAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class WhoamiController {
  private final UserRestMapper userRestMapper;
  private UsernamePasswordAuthenticator authenticator;

  @GetMapping("/whoami")
  public Whoami whoami(HttpServletRequest request) {
    var authUser = authenticator.retrieveUserWithoutLegalFileCheck(request);
    return new Whoami().user(userRestMapper.toRest(authUser));
  }
}
