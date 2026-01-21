package app.bpartners.api.model.mapper;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.jpa.model.HUserAnalysisApiKey;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserApiKeyMapper {

  // TODO: set inside specific Rest Mapper
  public app.bpartners.api.endpoint.rest.model.UserApiKey toRest(
      UserAnalysisApiKey userAnalysisApiKey) {
    return new app.bpartners.api.endpoint.rest.model.UserApiKey()
        .enabled(userAnalysisApiKey.isEnabled())
        .type(ANALYSIS)
        .creationDatetime(userAnalysisApiKey.getCreationDatetime())
        .expirationDatetime(userAnalysisApiKey.getExpirationDatetime())
        .key(userAnalysisApiKey.getApiKey());
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
