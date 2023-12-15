package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccessToken;
import app.bpartners.api.repository.jpa.model.HCalendarStoredCredential;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenMapper {
  public AccessToken toDomain(HSheetStoredCredential entity) {
    return getAccessToken(
        entity.getExpirationTimeMilliseconds(),
        entity.getCreationDatetime(),
        entity.getId(),
        entity.getAccessToken(),
        entity.getRefreshToken());
  }

  public AccessToken toDomain(HCalendarStoredCredential entity) {
    return getAccessToken(
        entity.getExpirationTimeMilliseconds(),
        entity.getCreationDatetime(),
        entity.getId(),
        entity.getAccessToken(),
        entity.getRefreshToken());
  }

  private AccessToken getAccessToken(
      Long expirationTimeMilliseconds,
      Instant creationDatetime2,
      String id,
      String accessToken,
      String refreshToken) {
    Instant creationDatetime = creationDatetime2;
    Long creationTimeMilliseconds = creationDatetime.toEpochMilli();
    Instant expirationDatetime = Instant.ofEpochMilli(expirationTimeMilliseconds);
    return AccessToken.builder()
        .id(id)
        .value(accessToken)
        .refreshToken(refreshToken)
        .creationDatetime(creationDatetime)
        .expirationTimeInSeconds((expirationTimeMilliseconds - creationTimeMilliseconds) / 1_000)
        .expirationDatetime(expirationDatetime)
        .build();
  }
}
