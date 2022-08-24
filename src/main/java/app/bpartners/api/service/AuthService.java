package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.AuthParams;
import app.bpartners.api.endpoint.rest.model.RedirectionComponent;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.TokenParams;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
  private final SwanConf swanConf;
  private final SwanComponent swanComponent;

  public RedirectionComponent generateAuthUrl(AuthParams params) {
    RedirectionComponent redirectionComponent = new RedirectionComponent();
    redirectionComponent.setRedirectionUrl(
        swanConf.getAuthProviderUrl() + "?response_type=code&client_id="
            + swanConf.getClientId() + "&redirect_uri=" + params.getSuccessUrl()
            + "&scope=openid%20offline%20idverified&state=12341234"
            + "&phoneNumber=" + params.getPhoneNumber());
    redirectionComponent.setSuccessUrl(params.getSuccessUrl());
    redirectionComponent.setFailureUrl(params.getFailureUrl());
    return redirectionComponent;
  }

  public Token generateTokenUrl(TokenParams params) {
    if (params.getCode() == null) {
      throw new BadRequestException("Code is mandatory");
    }
    return swanComponent.getTokenByCode(params.getCode(), params.getSuccessUrl());
  }
}
