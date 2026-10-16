package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
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
  private final EventProducer eventProducer;

  @Override
  public void accept(AnalysisApiKeyCheckTriggered event) {
    try {
      check(event.getUserId());
    } catch (RuntimeException e) {
      // not rethrown: a retry would trigger the next user again
      log.error(PREFIX + "Unable to check User(id={}) analysis api keys", event.getUserId(), e);
    }
    triggerNext(event);
  }

  private void triggerNext(AnalysisApiKeyCheckTriggered event) {
    var next = event.getNextUserIds();
    if (next == null || next.isEmpty()) {
      return;
    }
    eventProducer.accept(
        List.of(new AnalysisApiKeyCheckTriggered(next.getFirst(), next.subList(1, next.size()))));
  }

  private void check(String userId) {
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
