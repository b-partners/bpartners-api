package app.bpartners.api.repository.google.sheets.mapper;

import app.bpartners.api.repository.google.generic.CredentialMapper;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import com.google.api.client.auth.oauth2.StoredCredential;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SheetCredentialMapper implements CredentialMapper<HSheetStoredCredential> {
  @Override
  public StoredCredential toStoredCredential(HSheetStoredCredential entity) {
    if (entity == null) {
      return null;
    }
    StoredCredential storedCredential = new StoredCredential();
    storedCredential.setAccessToken(entity.getAccessToken());
    storedCredential.setRefreshToken(entity.getRefreshToken());
    storedCredential.setExpirationTimeMilliseconds(entity.getExpirationTimeMilliseconds());
    return storedCredential;
  }

  @Override
  public HSheetStoredCredential toEntity(
      String idUser, StoredCredential storedCredential, Instant createdAt) {
    return (HSheetStoredCredential) HSheetStoredCredential.builder()
        .idUser(idUser)
        .accessToken(storedCredential.getAccessToken())
        .refreshToken(storedCredential.getRefreshToken())
        .expirationTimeMilliseconds(storedCredential.getExpirationTimeMilliseconds())
        .creationDatetime(createdAt)
        .build();
  }
}
