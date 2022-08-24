package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.AuthParams;
import app.bpartners.api.endpoint.rest.model.RedirectionComponent;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.TokenParams;
import app.bpartners.api.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class AuthenticationController {
  private final AuthService authService;

  @PostMapping("/auth")
  public RedirectionComponent redirectUrlToSwan(@RequestBody AuthParams params) {
    return authService.generateAuthUrl(params);
  }

  @PostMapping("/token")
  public Token generateToken(@RequestBody(required = false) TokenParams params) {
    return authService.generateTokenUrl(params);
  }
}
