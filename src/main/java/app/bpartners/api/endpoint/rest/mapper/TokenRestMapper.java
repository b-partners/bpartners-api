package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.TokenValidity;
import app.bpartners.api.model.AccessToken;
import org.springframework.stereotype.Component;

@Component
public class TokenRestMapper {
  public TokenValidity toRest(AccessToken domain) {
    return new TokenValidity()
        .expirationTime(domain.getExpirationTimeInSeconds())
        .createdAt(domain.getCreationDatetime())
        .expiredAt(domain.getExpirationDatetime());
  }
}
