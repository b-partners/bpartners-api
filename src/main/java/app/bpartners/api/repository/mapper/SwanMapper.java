package app.bpartners.api.repository.mapper;

import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.repository.swan.response.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class SwanMapper {

  public Token toRest(TokenResponse tokenResponse) {
    Token token = new Token();
    token.setAccessToken(tokenResponse.getAccessToken());
    token.setRefreshToken(tokenResponse.getRefreshToken());
    return token;
  }
}
