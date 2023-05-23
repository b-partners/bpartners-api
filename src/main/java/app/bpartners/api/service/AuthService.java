package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.endpoint.rest.model.Whoami;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
  private final CognitoComponent cognitoComponent;
  private final UserService userService;

  private final UserRestMapper userRestMapper;

  public Redirection generateAuthUrl(AuthInitiation authInitiation) {
    Redirection redirection = new Redirection();
    String encodedRedirectionUrl = "";
    redirection.setRedirectionUrl(encodedRedirectionUrl);
    redirection.setRedirectionStatusUrls(authInitiation.getRedirectionStatusUrls());
    return redirection;
  }

  public Token generateTokenUrl(CreateToken toCreate) {
    Token createdToken = cognitoComponent.getTokenByCode(
        toCreate.getCode(),
        toCreate.getRedirectionStatusUrls().getSuccessUrl());
    if (createdToken == null) {
      throw new BadRequestException("Code is invalid, expired, revoked or the redirectUrl "
          + toCreate.getRedirectionStatusUrls().getSuccessUrl()
          + " does not match in the authorization request");
    }
    Whoami whoami = new Whoami()
        .user(userRestMapper.toRest(userService.getUserByToken(createdToken.getAccessToken())));
    createdToken.setWhoami(whoami);
    return createdToken;
  }
}
