package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.stream.IntStream;
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
  void accept_splits_users_into_chains() {
    var size = AllAnalysisApiKeyCheckTriggeredService.CHAIN_SIZE;
    var users =
        IntStream.range(0, size + 1)
            .mapToObj(i -> new User().toBuilder().id("u" + i).build())
            .toList();
    when(userRepository.findAll()).thenReturn(users);

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    var ids = users.stream().map(User::getId).toList();
    verify(eventProducer)
        .accept(
            List.of(
                new AnalysisApiKeyCheckTriggered("u0", ids.subList(1, size)),
                new AnalysisApiKeyCheckTriggered("u" + size, List.of())));
  }

  @Test
  void accept_without_users_sends_nothing() {
    when(userRepository.findAll()).thenReturn(List.of());

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    verifyNoInteractions(eventProducer);
  }

  @Test
  void accept_rethrows_when_listing_users_fails() {
    when(userRepository.findAll()).thenThrow(new RuntimeException("db down"));

    var event = new AllAnalysisApiKeyCheckTriggered();

    assertThrows(RuntimeException.class, () -> subject.accept(event));
    verifyNoInteractions(eventProducer);
  }

  @Test
  void accept_rethrows_when_first_send_fails() {
    when(userRepository.findAll()).thenReturn(List.of(new User().toBuilder().id("a").build()));
    doThrow(new RuntimeException("too big")).when(eventProducer).accept(any());

    var event = new AllAnalysisApiKeyCheckTriggered();

    assertThrows(RuntimeException.class, () -> subject.accept(event));
  }
}
