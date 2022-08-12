package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserMapper;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class WhoamiController {
  private final UserMapper userMapper;

  @GetMapping("/whoami")
  public Whoami whoami(@AuthenticationPrincipal Principal principal) {
    Whoami whoami = new Whoami();
    whoami.setUser(userMapper.toRestUser(principal.getUser()));
    whoami.setBearerToken(principal.getBearer());
    return whoami;
  }
}