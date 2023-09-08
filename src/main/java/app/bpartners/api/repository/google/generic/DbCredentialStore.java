package app.bpartners.api.repository.google.generic;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class DbCredentialStore<E, J extends CredentialJpaRepository<E>, M extends CredentialMapper<E>>
    implements DataStore<StoredCredential> {
  public static final MemoryDataStoreFactory STORE_FACTORY =
      MemoryDataStoreFactory.getDefaultInstance();
  J repository;
  M mapper;

  public DbCredentialStore(J repository, M mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

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
    return repository.findAll().size();
  }

  @Override
  public boolean isEmpty() throws IOException {
    return size() == 0;
  }

  @Override
  public boolean containsKey(String key) {
    return repository.existsByIdUser(key);
  }

  @Override
  public boolean containsValue(StoredCredential value) {
    return repository.existsByAccessTokenAndRefreshTokenAndExpirationTimeMilliseconds(
        value.getAccessToken(), value.getRefreshToken(), value.getExpirationTimeMilliseconds()
    );
  }

  @Override
  public Set<String> keySet() throws IOException {
    return values().stream()
        .map(StoredCredential::toString)
        .collect(Collectors.toSet());
  }

  @Override
  public Collection<StoredCredential> values() throws IOException {
    return repository.findAll().stream()
        .map(mapper::toStoredCredential)
        .collect(Collectors.toList());
  }

  @Override
  public StoredCredential get(String idUser) throws IOException {
    List<E> allTokens =
        repository.findAllByIdUserOrderByCreationDatetimeDesc(idUser);
    return allTokens == null || allTokens.isEmpty() ? null
        : mapper.toStoredCredential(allTokens.get(0));
  }

  @Override
  public DataStore<StoredCredential> set(String idUser, StoredCredential value) throws IOException {
    Instant createdAt = Instant.now();
    var entity = mapper.toEntity(idUser, value, createdAt);
    repository.save(entity);
    return this;
  }

  @Override
  public DataStore<StoredCredential> clear() {
    //We do _NOT_ need to clear database
    return this;
  }

  @Override
  public DataStore<StoredCredential> delete(String idUser) throws IOException {
    repository.deleteByIdUser(idUser);
    return this;
  }
}
