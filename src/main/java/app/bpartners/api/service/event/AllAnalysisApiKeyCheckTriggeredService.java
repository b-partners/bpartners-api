package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AllAnalysisApiKeyCheckTriggeredService
    implements Consumer<AllAnalysisApiKeyCheckTriggered> {
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AllAnalysisApiKeyCheckTriggered event) {
    List<AnalysisApiKeyCheckTriggered> events =
        userRepository.findAll().stream()
            .map(user -> new AnalysisApiKeyCheckTriggered(user.getId()))
            .toList();

    eventProducer.accept(events);
  }
}
