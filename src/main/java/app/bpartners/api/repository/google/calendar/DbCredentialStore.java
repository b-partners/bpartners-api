package app.bpartners.api.repository.google.calendar;

import app.bpartners.api.repository.google.calendar.mapper.StoredCredentialMapper;
import app.bpartners.api.repository.jpa.CalendarStoredCredentialJpaRep;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DbCredentialStore implements DataStore<StoredCredential> {
  public static final MemoryDataStoreFactory STORE_FACTORY =
      MemoryDataStoreFactory.getDefaultInstance();
  private final CalendarStoredCredentialJpaRep jpaRep;
  private final StoredCredentialMapper mapper;

  @Override
  public DataStoreFactory getDataStoreFactory() {
    return STORE_FACTORY;
  }

  @Override
  public String getId() {
    return DbCredentialStore.class.getName();
  }

  @Override
  public int size() throws IOException {
    return jpaRep.findAll().size();
  }

  @Override
  public boolean isEmpty() throws IOException {
    return size() == 0;
  }

  @Override
  public boolean containsKey(String userId) {
    return jpaRep.existsByIdUser(userId);

  }

  @Override
  public boolean containsValue(StoredCredential value) {
    //TODO: find accessToken AND refreshToken AND expirationTimeMilliseconds
    return jpaRep.existsByAccessTokenAndRefreshTokenAndExpirationTimeMilliseconds(
        value.getAccessToken(), value.getRefreshToken(), value.getExpirationTimeMilliseconds()
    );
  }

  @Override
  public Set<String> keySet() {
    return values().stream()
        .map(StoredCredential::toString)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<StoredCredential> values() {
    return jpaRep.findAll().stream()
        .map(mapper::toStoredCredential)
        .collect(Collectors.toList());
  }

  @Override
  public StoredCredential get(String userId) {
    return mapper.toStoredCredential(jpaRep.findByIdUser(userId));
  }

  @Override
  public DataStore<StoredCredential> set(String userId, StoredCredential value) {
    Instant createdAt = Instant.now();
    var entity = mapper.toEntity(userId, value, createdAt);
    jpaRep.save(entity);
    return this;
  }

  @Override
  public DataStore<StoredCredential> clear() {
    //We do _NOT_ need to clear database
    return this;
  }

  @Override
  public DataStore<StoredCredential> delete(String userId) {
    jpaRep.deleteByIdUser(userId);
    return this;
  }
}
