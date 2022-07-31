package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import app.bpartners.api.endpoint.rest.model.Whoami;

@RestController
public class WhoamiController {

  @GetMapping("/whoami")
  public Whoami hello(@AuthenticationPrincipal Principal principal) {
    Whoami whoami = new Whoami();
    whoami.setId(principal.getUserId());
    whoami.setBearer(principal.getBearer());
    whoami.setRole(Whoami.RoleEnum.valueOf(principal.getRole()));
    return whoami;
  }
}
