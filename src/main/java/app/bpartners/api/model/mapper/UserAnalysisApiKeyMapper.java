package app.bpartners.api.model.mapper;

import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserAnalysisApiKeyMapper {

  public UserAnalysisApiKey toDomain(HUserAnalysisApiKey entityHUserAnalysisApiKey) {
    return new UserAnalysisApiKey()
        .toBuilder()
            .id(entityHUserAnalysisApiKey.getId())
            .userId(entityHUserAnalysisApiKey.getUserId())
            .apiKey(entityHUserAnalysisApiKey.getApiKey())
            .creationDatetime(entityHUserAnalysisApiKey.getCreationDatetime())
            .expirationDatetime(entityHUserAnalysisApiKey.getExpirationDatetime())
            .build();
  }

  public HUserAnalysisApiKey toEntity(UserAnalysisApiKey userAnalysisApiKey) {
    return new HUserAnalysisApiKey()
        .toBuilder()
            .id(userAnalysisApiKey.getId())
            .userId(userAnalysisApiKey.getUserId())
            .apiKey(userAnalysisApiKey.getApiKey())
            .creationDatetime(userAnalysisApiKey.getCreationDatetime())
            .expirationDatetime(userAnalysisApiKey.getExpirationDatetime())
            .build();
  }
}
