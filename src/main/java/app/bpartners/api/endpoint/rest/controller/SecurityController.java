package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.OauthToken;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class SecurityController {

  @GetMapping(value = "/auth")
  public RedirectView redirectToSwanAuth() {
    return new RedirectView(SwanConf.getAuthUrl());
  }

  //After successfully authenticated to Swan Auth, the redirect URI refers to this endpoint /whoami
  @GetMapping(value = "/whoami")
  public OauthToken whoami(@RequestParam String code) {
    return SwanComponent.getTokenByCode(code);
  }
}
