package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.validator.TokenValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
  private final SwanConf swanConf;
  private final SwanComponent swanComponent;
  private final TokenValidator tokenValidator;
  private final UserService userService;

  private final UserRestMapper userRestMapper;

  public Redirection generateAuthUrl(AuthInitiation authInitiation) {
    Redirection redirection = new Redirection();

    redirection.setRedirectionUrl(
        swanConf.getAuthProviderUrl() + "?response_type=code&client_id="
            + swanConf.getClientId() + "&redirect_uri="
            + authInitiation.getRedirectionStatusUrls().getSuccessUrl()
            + "&scope=openid%20offline%20idverified&state=" + authInitiation.getState()
            + "&phoneNumber=" + authInitiation.getPhone());
    redirection.setRedirectionStatusUrls(authInitiation.getRedirectionStatusUrls());

    return redirection;
  }

  public Token generateTokenUrl(CreateToken toCreate) {
    tokenValidator.accept(toCreate);
    String redirectUrl = toCreate.getRedirectionStatusUrls().getSuccessUrl();
    Token createdToken = swanComponent.getTokenByCode(toCreate.getCode(), redirectUrl);
    Whoami whoami = new Whoami()
        .user(userRestMapper.toRest(userService.getUserByToken(createdToken.getAccessToken())));
    createdToken.setWhoami(whoami);
    return createdToken;
  }
}
