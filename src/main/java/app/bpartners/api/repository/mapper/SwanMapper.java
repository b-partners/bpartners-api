package app.bpartners.api.repository.mapper;

import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.repository.swan.response.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class SwanMapper {

  public Token toRest(TokenResponse tokenResponse) {
    Token token = new Token();
    token.setIdToken(tokenResponse.getId_token());
    token.setAccessToken(tokenResponse.getAccess_token());
    token.setRefreshToken(tokenResponse.getRefresh_token());
    token.setScope(tokenResponse.getScope());
    token.setTokenType(tokenResponse.getToken_type());
    token.setExpiresIn(tokenResponse.getExpires_in());
    return token;
  }
}
