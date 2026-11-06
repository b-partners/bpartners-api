package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyCreation;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyDeactivation;
import app.bpartners.api.endpoint.event.model.UserApiKeyRepairRequested;
import app.bpartners.api.endpoint.event.model.UserApiKeySync;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserApiKeyRepairRequestedService implements Consumer<UserApiKeyRepairRequested> {
  private final UserRepository userRepository;
  private final AnalysisApiKeyApi analysisApiKeyApi;
  private final UserApiKeySyncService userApiKeySyncService;
  private final UserAnalysisApiKeyCreationService userAnalysisApiKeyCreationService;
  private final UserAnalysisApiKeyDeactivationService userAnalysisApiKeyDeactivationService;

  @Override
  public void accept(UserApiKeyRepairRequested event) {
    var userId = event.getUserId();
    userApiKeySyncService.accept(new UserApiKeySync(userId));
    userAnalysisApiKeyCreationService.accept(new UserAnalysisApiKeyCreation(userId));

    userRepository.getById(userId).getAnalysisApiKeys().stream()
        .filter(UserAnalysisApiKey::isEnabled)
        .filter(key -> !analysisApiKeyApi.isRecognized(key.getApiKey()))
        .forEach(
            key ->
                userAnalysisApiKeyDeactivationService.accept(
                    new UserAnalysisApiKeyDeactivation(userId, key.getId())));
  }
}
