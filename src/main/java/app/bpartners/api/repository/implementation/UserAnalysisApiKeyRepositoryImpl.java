package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.mapper.UserAnalysisApiKeyMapper;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.repository.jpa.UserAnalysisApiKeyJpaRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class UserAnalysisApiKeyRepositoryImpl implements UserAnalysisApiKeyRepository {
  private final UserAnalysisApiKeyJpaRepository jpaRepository;
  private final UserAnalysisApiKeyMapper mapper;

  @Override
  public List<UserAnalysisApiKey> getAllByUserId(String userId) {
    return jpaRepository.findAllByUserId(userId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public UserAnalysisApiKey save(UserAnalysisApiKey userAnalysisApiKey) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(userAnalysisApiKey)));
  }
}
