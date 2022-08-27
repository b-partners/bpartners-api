package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.CreateToken;
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

  public Redirection generateAuthUrl(AuthInitiation authInitiation) {
    Redirection redirection = new Redirection();

    redirection.setRedirectionUrl(
        swanConf.getAuthProviderUrl() + "?response_type=code&client_id="
            + swanConf.getClientId() + "&redirect_uri=" + authInitiation.getRedirectionStatusUrls().getSuccessUrl()
            + "&scope=openid%20offline%20idverified&state=12341234" //TODO: state
            + "&phoneNumber=" + authInitiation.getPhone());
    redirection.setRedirectionStatusUrls(authInitiation.getRedirectionStatusUrls());

    return redirection;
  }

  public Token generateTokenUrl(CreateToken params) {
    if (params.getCode() == null) { //TODO: why not in a validator
      throw new BadRequestException("Code is mandatory");
    }
    return swanComponent.getTokenByCode(params.getCode(), params.getRedirectionStatusUrls().getSuccessUrl());
  }
}
