package app.bpartners.api.repository.mapper;

import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.model.Token;
import app.bpartners.api.repository.swan.response.TokenResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SwanMapper {
  public SwanUser graphQLToRest(app.bpartners.api.repository.swan.schema.SwanUser graphqlUser) {
    SwanUser user = new SwanUser();
    user.setSwanId(graphqlUser.id);
    user.setFirstName(graphqlUser.firstName);
    user.setLastName(graphqlUser.lastName);
    user.setMobilePhoneNumber(graphqlUser.mobilePhoneNumber);
    user.setIdentificationStatus(graphqlUser.identificationStatus);
    user.setBirthDate(graphqlUser.birthDate);
    user.setNationalityCCA3(graphqlUser.nationalityCCA3);
    user.setIdVerified(graphqlUser.idVerified);
    return user;
  }

  public List<SwanUser> graphQLToRest(
      List<app.bpartners.api.repository.swan.schema.SwanUser> graphqlUsers) {
    return graphqlUsers.stream()
        .map(this::graphQLToRest)
        .collect(Collectors.toUnmodifiableList());
  }

  public Token graphQLToRest(TokenResponse graphQLToken) {
    Token token = new Token();
    token.setIdToken(graphQLToken.id_token);
    token.setAccessToken(graphQLToken.access_token);
    token.setRefreshToken(graphQLToken.refresh_token);
    token.setScope(graphQLToken.scope);
    token.setTokenType(graphQLToken.token_type);
    token.setExpiresIn(graphQLToken.expires_in);
    return token;
  }
}
