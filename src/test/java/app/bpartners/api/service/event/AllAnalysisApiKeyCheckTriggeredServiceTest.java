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
  void accept_starts_the_chain_with_first_user() {
    var users =
        List.of(
            new User().toBuilder().id(randomUUID().toString()).build(),
            new User().toBuilder().id(randomUUID().toString()).build());
    when(userRepository.findAll()).thenReturn(users);

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    verify(eventProducer)
        .accept(
            List.of(
                new AnalysisApiKeyCheckTriggered(
                    users.get(0).getId(), List.of(users.get(1).getId()))));
  }

  @Test
  void accept_without_users_sends_nothing() {
    when(userRepository.findAll()).thenReturn(List.of());

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    verifyNoInteractions(eventProducer);
  }
}
