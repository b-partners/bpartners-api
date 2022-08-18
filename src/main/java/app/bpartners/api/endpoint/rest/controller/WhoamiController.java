package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class WhoamiController {
  private final UserRepository userRepository;
  private final UserRestMapper userRestMapper;

  @GetMapping("/whoami")
  public Whoami whoami(@AuthenticationPrincipal Principal principal) {
    Whoami whoami = new Whoami();
    whoami.setUser(
        userRestMapper.toRest(userRepository.getUserById(principal.getUserId())));
    whoami.setBearerToken(principal.getBearer());
    return whoami;
  }
}