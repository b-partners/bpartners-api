package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.repository.UserRepository;
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
  private static final String FAILURE_PREFIX = "[AAKC F] ";
  private final UserAnalysisApiKeyRepository userAnalysisApiKeyRepository;
  private final AnalysisApiKeyApi analysisApiKeyApi;
  private final EventProducer eventProducer;
  private final UserRepository userRepository;

  @Override
  public void accept(AnalysisApiKeyCheckTriggered event) {
    var remaining = event.getNextUserIds() == null ? 0 : event.getNextUserIds().size();
    log.info(PREFIX + "Checking User(id={}), {} users remaining", event.getUserId(), remaining);
    try {
      check(event.getUserId());
    } catch (RuntimeException e) {
      log.error(
          FAILURE_PREFIX + "Unable to check User(id={}) analysis api keys", event.getUserId(), e);
    }
    triggerNext(event);
  }

  private void triggerNext(AnalysisApiKeyCheckTriggered event) {
    var next = event.getNextUserIds();
    var offset = event.getNextChunkOffset();
    if (next != null && !next.isEmpty()) {
      send(new AnalysisApiKeyCheckTriggered(next.getFirst(), next.subList(1, next.size()), offset));
    } else if (offset != null) {
      var ids = userRepository.findAllIds();
      if (offset < ids.size()) {
        send(AnalysisApiKeyCheckTriggered.ofChunk(ids, offset));
      }
    } else {
      log.info(PREFIX + "End of chain after User(id={})", event.getUserId());
    }
  }

  private void send(AnalysisApiKeyCheckTriggered next) {
    try {
      eventProducer.accept(List.of(next));
    } catch (RuntimeException e) {
      log.error(
          FAILURE_PREFIX + "Unable to trigger next check User(id={}), {} users left unchecked",
          next.getUserId(),
          next.getNextUserIds().size() + 1,
          e);
      throw e;
    }
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
            FAILURE_PREFIX + "User(id={}) analysis key(id={}) is NOT recognized by geo-jobs",
            userId,
            key.getId());
      }
    }
  }
}
