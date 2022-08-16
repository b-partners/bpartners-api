package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.RedirectionParams;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.TokenParams;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
public class AuthenticationController {
  private final SwanConf swanConf;
  private final SwanComponent swanComponent;

  @PostMapping("/auth")
  public String redirectUrlToSwan(@RequestBody RedirectionParams redirection) {
    if (redirection.getPhoneNumber() == null) {
      throw new BadRequestException("Phone number is mandatory");
    }
    return
        "https://oauth.swan.io/oauth2/auth?response_type=code&client_id="
            + swanConf.getClientId() + "&redirect_uri=" + swanConf.getRedirectUri()
            + "&scope=openid%20offline%20idverified&state=12341234"
            + "&phoneNumber=" + redirection.getPhoneNumber();
  }

  @PostMapping("/token")
  public Token generateToken(@RequestBody(required = false) TokenParams tokenParams) {
    if (tokenParams.getCode() == null) {
      throw new BadRequestException("Code is mandatory");
    }
    return swanComponent.getTokenByCode(tokenParams.getCode());
  }
}
