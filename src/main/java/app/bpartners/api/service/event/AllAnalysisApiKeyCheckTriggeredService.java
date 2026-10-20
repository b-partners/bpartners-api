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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AllAnalysisApiKeyCheckTriggeredService
    implements Consumer<AllAnalysisApiKeyCheckTriggered> {
  // ids are ~40 bytes each, 1000 per chain keeps events far below the 256KB EventBridge/SQS limit
  static final int CHAIN_SIZE = 1000;
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AllAnalysisApiKeyCheckTriggered event) {
    List<String> userIds = userRepository.findAll().stream().map(User::getId).toList();
    if (userIds.isEmpty()) {
      return;
    }

    var events = new ArrayList<AnalysisApiKeyCheckTriggered>();
    for (int i = 0; i < userIds.size(); i += CHAIN_SIZE) {
      var chain = userIds.subList(i, Math.min(i + CHAIN_SIZE, userIds.size()));
      events.add(
          new AnalysisApiKeyCheckTriggered(chain.getFirst(), chain.subList(1, chain.size())));
    }
    eventProducer.accept(events);
  }
}
