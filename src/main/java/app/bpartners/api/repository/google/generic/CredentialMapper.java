package app.bpartners.api.repository.google.generic;

import com.google.api.client.auth.oauth2.StoredCredential;
import java.time.Instant;

public interface CredentialMapper<E> {
  StoredCredential toStoredCredential(E e);

  E toEntity(String idUser, StoredCredential s, Instant createdAt);
}
