package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.model.Code;
import app.bpartners.api.endpoint.rest.model.PhoneNumber;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@AllArgsConstructor
@RestController
public class AuthenticationController {
  private SwanConf swanConf;
  private SwanComponent swanComponent;

  @PostMapping("/auth")
  public RedirectView redirectToSwan(@RequestBody(required = false) PhoneNumber phoneNumber) {
    if (phoneNumber == null) {
      throw new BadRequestException("Phone number is mandatory");
    }
    return new RedirectView(
        "https://oauth.swan.io/oauth2/auth?response_type=code&client_id="
            + swanConf.getClientId() + "&redirect_uri=" + swanConf.getRedirectUri()
            + "&scope=openid%20offline%20idverified&state=12341234"
            + "&phoneNumber=" + phoneNumber.getPhoneNumber());
  }

  @PostMapping("/token")
  public Token generateToken(@RequestBody(required = false) Code code) {
    if (code == null) {
      throw new BadRequestException("Code is mandatory");
    }
    return swanComponent.getTokenByCode(code.getCode());
  }
}
