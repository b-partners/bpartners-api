package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AllAnalysisApiKeyCheckTriggeredService
    implements Consumer<AllAnalysisApiKeyCheckTriggered> {
  private static final String PREFIX = "[AAKC] ";
  private static final String FAILURE_PREFIX = "[AAKC F] ";
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AllAnalysisApiKeyCheckTriggered event) {
    log.info(PREFIX + "AllAnalysisApiKeyCheckTriggered received");
    List<String> userIds;
    try {
      userIds = userRepository.findAllIds();
    } catch (RuntimeException e) {
      log.error(FAILURE_PREFIX + "Unable to list users to check", e);
      throw e;
    }
    log.info(PREFIX + "{} users will be checked", userIds.size());
    if (userIds.isEmpty()) {
      return;
    }

    try {
      eventProducer.accept(List.of(AnalysisApiKeyCheckTriggered.ofChunk(userIds, 0)));
    } catch (RuntimeException e) {
      log.error(
          FAILURE_PREFIX + "Unable to send first AnalysisApiKeyCheckTriggered ({} users)",
          userIds.size(),
          e);
      throw e;
    }
    log.info(
        PREFIX + "First AnalysisApiKeyCheckTriggered chunk sent ({} users in total)",
        userIds.size());
  }
}
