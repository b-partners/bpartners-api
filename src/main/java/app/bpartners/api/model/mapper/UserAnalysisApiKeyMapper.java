package app.bpartners.api.model.mapper;

import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserAnalysisApiKeyMapper {
  private final UserMapper userMapper;

  public UserAnalysisApiKey toDomain(HUserAnalysisApiKey entityHUserAnalysisApiKey) {
    return UserAnalysisApiKey.builder()
        .id(entityHUserAnalysisApiKey.getId())
        .user(userMapper.toDomain(entityHUserAnalysisApiKey.getUser()))
        .apiKey(entityHUserAnalysisApiKey.getApiKey())
        .creationDatetime(entityHUserAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(entityHUserAnalysisApiKey.getExpirationDatetime())
        .build();
  }

  public HUserAnalysisApiKey toEntity(UserAnalysisApiKey userAnalysisApiKey) {
    return HUserAnalysisApiKey.builder()
        .id(userAnalysisApiKey.getId())
        .user(userMapper.toEntity(userAnalysisApiKey.getUser()))
        .apiKey(userAnalysisApiKey.getApiKey())
        .creationDatetime(userAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(userAnalysisApiKey.getExpirationDatetime())
        .build();
  }
}
