package app.bpartners.api.repository.mapper;

import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.repository.swan.response.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class SwanMapper {

  public Token toRest(TokenResponse tokenResponse) {
    Token token = new Token();
    token.setIdToken(tokenResponse.id_token);
    token.setAccessToken(tokenResponse.access_token);
    token.setRefreshToken(tokenResponse.refresh_token);
    token.setScope(tokenResponse.scope);
    token.setTokenType(tokenResponse.token_type);
    token.setExpiresIn(tokenResponse.expires_in);
    return token;
  }
}
