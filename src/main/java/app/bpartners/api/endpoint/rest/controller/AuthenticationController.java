package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.CreateToken;
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
  //TODO: rename ALL params parameters to a more explicit name!
  public Redirection redirectUrlToSwan(@RequestBody AuthInitiation params) {
    return authService.generateAuthUrl(params);
  }

  @PostMapping("/token")
  public Token generateToken(@RequestBody(required = false) CreateToken params) {
    return authService.generateTokenUrl(params);
  }
}
