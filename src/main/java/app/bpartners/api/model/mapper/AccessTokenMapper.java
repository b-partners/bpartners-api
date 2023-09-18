package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccessToken;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenMapper {
  public AccessToken toDomain(HSheetStoredCredential entity) {
    Long expirationTimeMilliseconds = entity.getExpirationTimeMilliseconds();
    Instant creationDatetime = entity.getCreationDatetime();
    Long creationTimeMilliseconds = creationDatetime.toEpochMilli();
    Instant expirationDatetime = Instant.ofEpochMilli(expirationTimeMilliseconds);
    return AccessToken.builder()
        .id(entity.getId())
        .value(entity.getAccessToken())
        .refreshToken(entity.getRefreshToken())
        .creationDatetime(creationDatetime)
        .expirationTimeInSeconds(
            (expirationTimeMilliseconds - creationTimeMilliseconds) / 1_000)
        .expirationDatetime(expirationDatetime)
        .build();
  }
}
