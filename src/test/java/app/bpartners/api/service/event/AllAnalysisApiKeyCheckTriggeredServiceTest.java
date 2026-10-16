package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllAnalysisApiKeyCheckTriggeredServiceTest {
  UserRepository userRepository = mock();
  EventProducer eventProducer = mock();
  AllAnalysisApiKeyCheckTriggeredService subject =
      new AllAnalysisApiKeyCheckTriggeredService(userRepository, eventProducer);

  @Test
  void accept_sends_one_event_per_user() {
    var users =
        List.of(
            new User().toBuilder().id(randomUUID().toString()).build(),
            new User().toBuilder().id(randomUUID().toString()).build());
    when(userRepository.findAll()).thenReturn(users);

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    verify(eventProducer)
        .accept(
            users.stream().map(user -> new AnalysisApiKeyCheckTriggered(user.getId())).toList());
  }
}
