package app.bpartners.api.repository.google.calendar.mapper;

import app.bpartners.api.repository.jpa.model.HCalendarStoredCredential;
import com.google.api.client.auth.oauth2.StoredCredential;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class StoredCredentialMapper {
  public StoredCredential toStoredCredential(HCalendarStoredCredential entity) {
    if (entity == null) {
      return null;
    }
    StoredCredential storedCredential = new StoredCredential();
    storedCredential.setAccessToken(entity.getAccessToken());
    storedCredential.setRefreshToken(entity.getRefreshToken());
    storedCredential.setExpirationTimeMilliseconds(entity.getExpirationTimeMilliseconds());
    return storedCredential;
  }

  public HCalendarStoredCredential toEntity(
      String idUser, StoredCredential storedCredential, Instant createdAt) {
    return HCalendarStoredCredential.builder()
        .idUser(idUser)
        .accessToken(storedCredential.getAccessToken())
        .refreshToken(storedCredential.getRefreshToken())
        .expirationTimeMilliseconds(storedCredential.getExpirationTimeMilliseconds())
        .creationDatetime(createdAt)
        .build();
  }
}
