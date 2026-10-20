package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import java.util.ArrayList;
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
  // ids are ~40 bytes each, 1000 per chain keeps events far below the 256KB EventBridge/SQS limit
  static final int CHAIN_SIZE = 1000;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AllAnalysisApiKeyCheckTriggered event) {
    log.info(PREFIX + "AllAnalysisApiKeyCheckTriggered received");
    List<String> userIds;
    try {
      userIds = userRepository.findAll().stream().map(User::getId).toList();
    } catch (RuntimeException e) {
      log.error(FAILURE_PREFIX + "Unable to list users to check", e);
      throw e;
    }
    log.info(PREFIX + "{} users will be checked", userIds.size());
    if (userIds.isEmpty()) {
      return;
    }

    var events = new ArrayList<AnalysisApiKeyCheckTriggered>();
    for (int i = 0; i < userIds.size(); i += CHAIN_SIZE) {
      var chain = userIds.subList(i, Math.min(i + CHAIN_SIZE, userIds.size()));
      events.add(
          new AnalysisApiKeyCheckTriggered(chain.getFirst(), chain.subList(1, chain.size())));
    }

    try {
      eventProducer.accept(events);
    } catch (RuntimeException e) {
      log.error(
          FAILURE_PREFIX + "Unable to send {} AnalysisApiKeyCheckTriggered chains ({} users)",
          events.size(),
          userIds.size(),
          e);
      throw e;
    }
    log.info(PREFIX + "{} AnalysisApiKeyCheckTriggered chains sent", events.size());
  }
}
