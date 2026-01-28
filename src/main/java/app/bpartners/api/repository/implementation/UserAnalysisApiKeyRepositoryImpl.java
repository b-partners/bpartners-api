package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.mapper.UserApiKeyMapper;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.UserAnalysisApiKeyJpaRepository;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class UserAnalysisApiKeyRepositoryImpl implements UserAnalysisApiKeyRepository {
  private final UserAnalysisApiKeyJpaRepository jpaRepository;
  private final UserRepository userRepository;
  private final UserApiKeyMapper mapper;

  @Override
  public List<UserAnalysisApiKey> getAllByUserId(String userId) {
    var user = userRepository.getById(userId);
    return jpaRepository.findAllByUserId(userId).stream()
        .map(entity -> mapper.toDomain(entity, user))
        .toList();
  }

  @Override
  public UserAnalysisApiKey getByApiKey(String apiKey) {
    HUserAnalysisApiKey hApiKey = jpaRepository.getByApiKey(apiKey);
    if (hApiKey == null) {
      return null;
    }
    User user = userRepository.getById(hApiKey.getUserId());
    return mapper.toDomain(hApiKey, user);
  }

  @Override
  public UserAnalysisApiKey save(UserAnalysisApiKey toSave) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(toSave)), toSave.getUser());
  }
}
