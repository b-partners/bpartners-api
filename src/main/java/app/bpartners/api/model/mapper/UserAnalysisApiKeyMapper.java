package app.bpartners.api.model.mapper;

import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserAnalysisApiKeyMapper {

  public UserAnalysisApiKey toDomain(HUserAnalysisApiKey entityHUserAnalysisApiKey) {
    return UserAnalysisApiKey.builder()
        .id(entityHUserAnalysisApiKey.getId())
        .apiKey(entityHUserAnalysisApiKey.getApiKey())
        .creationDatetime(entityHUserAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(entityHUserAnalysisApiKey.getExpirationDatetime())
        .enabled(entityHUserAnalysisApiKey.isEnabled())
        .build();
  }

  public HUserAnalysisApiKey toEntity(UserAnalysisApiKey userAnalysisApiKey) {
    return HUserAnalysisApiKey.builder()
        .id(userAnalysisApiKey.getId())
        .apiKey(userAnalysisApiKey.getApiKey())
        .creationDatetime(userAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(userAnalysisApiKey.getExpirationDatetime())
        .enabled(userAnalysisApiKey.isEnabled())
        .build();
  }
}
