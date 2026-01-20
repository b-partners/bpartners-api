package app.bpartners.api.model.mapper;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserAnalysisApiKeyMapper {

  public app.bpartners.api.endpoint.rest.model.UserAnalysisApiKey toDTO(
      UserAnalysisApiKey userAnalysisApiKey) {
    return new app.bpartners.api.endpoint.rest.model.UserAnalysisApiKey()
        .enabled(userAnalysisApiKey.isEnabled())
        .creationDatetime(userAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(userAnalysisApiKey.getExpirationDatetime())
        .apiKey(userAnalysisApiKey.getApiKey());
  }

  public UserAnalysisApiKey toDomain(HUserAnalysisApiKey entityHUserAnalysisApiKey, User user) {
    return UserAnalysisApiKey.builder()
        .id(entityHUserAnalysisApiKey.getId())
        .user(user)
        .apiKey(entityHUserAnalysisApiKey.getApiKey())
        .creationDatetime(entityHUserAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(entityHUserAnalysisApiKey.getExpirationDatetime())
        .enabled(entityHUserAnalysisApiKey.isEnabled())
        .build();
  }

  public HUserAnalysisApiKey toEntity(UserAnalysisApiKey userAnalysisApiKey) {
    return HUserAnalysisApiKey.builder()
        .id(userAnalysisApiKey.getId())
        .userId(userAnalysisApiKey.getUser().getId())
        .apiKey(userAnalysisApiKey.getApiKey())
        .creationDatetime(userAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(userAnalysisApiKey.getExpirationDatetime())
        .enabled(userAnalysisApiKey.isEnabled())
        .build();
  }
}
