package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.validator.AuthInitiationValidator;
import app.bpartners.api.model.validator.TokenValidator;
import app.bpartners.api.service.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class AuthenticationController {
  private final AuthService authService;
  private final AuthInitiationValidator authInitiationValidator;
  private final TokenValidator tokenValidator;

  @PostMapping("/authInitiation")
  public Redirection redirectUrlToSwan(@RequestBody AuthInitiation authInitiation) {
    authInitiationValidator.accept(authInitiation);
    return authService.generateAuthUrl(authInitiation);
  }

  @PostMapping("/token")
  public Token generateToken(@RequestBody(required = false) CreateToken createToken) {
    tokenValidator.accept(createToken);
    return authService.generateTokenUrl(createToken);
  }
}
