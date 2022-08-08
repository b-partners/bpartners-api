package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@AllArgsConstructor
public class SecurityController {
  private final SwanComponent swanComponent;
  private final SwanConf swanConf;

  @GetMapping(value = "/auth")
  public RedirectView redirectToSwanAuth() {
    return new RedirectView(swanConf.getAuthUrl());
  }

  //After successfully authenticated to Swan Auth, the redirect URI refers to this endpoint /whoami
  @GetMapping(value = "/whoami")
  public Whoami whoami(@RequestParam String code) {
    return swanComponent.getTokenByCode(code);
  }
}
