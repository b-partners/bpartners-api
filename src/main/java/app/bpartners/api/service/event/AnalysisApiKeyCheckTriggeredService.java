package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisApiKeyCheckTriggeredService implements Consumer<AnalysisApiKeyCheckTriggered> {
  private static final String PREFIX = "[AAKC] ";
  private final UserAnalysisApiKeyRepository userAnalysisApiKeyRepository;
  private final AnalysisApiKeyApi analysisApiKeyApi;

  @Override
  public void accept(AnalysisApiKeyCheckTriggered event) {
    var userId = event.getUserId();
    List<UserAnalysisApiKey> enabledKeys =
        userAnalysisApiKeyRepository.getAllByUserId(userId).stream()
            .filter(UserAnalysisApiKey::isEnabled)
            .toList();

    if (enabledKeys.isEmpty()) {
      log.warn(PREFIX + "User(id={}) has no enabled analysis api key", userId);
      return;
    }

    for (var key : enabledKeys) {
      if (analysisApiKeyApi.isRecognized(key.getApiKey())) {
        log.info(
            PREFIX + "User(id={}) analysis key(id={}) is known by geo-jobs", userId, key.getId());
      } else {
        log.error(
            PREFIX + "User(id={}) analysis key(id={}) is NOT recognized by geo-jobs",
            userId,
            key.getId());
      }
    }
  }
}
