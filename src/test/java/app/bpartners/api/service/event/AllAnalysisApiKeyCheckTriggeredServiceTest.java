package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
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
    when(userRepository.findAllIds()).thenReturn(List.of("a", "b"));

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    verify(eventProducer).accept(List.of(new AnalysisApiKeyCheckTriggered("a", List.of("b"))));
  }

  @Test
  void accept_sends_only_first_chunk_with_offset_of_next() {
    var size = AnalysisApiKeyCheckTriggered.CHUNK_SIZE;
    var ids = IntStream.range(0, size + 1).mapToObj(i -> "u" + i).toList();
    when(userRepository.findAllIds()).thenReturn(ids);

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    verify(eventProducer)
        .accept(List.of(new AnalysisApiKeyCheckTriggered("u0", ids.subList(1, size), size)));
  }

  @Test
  void accept_without_users_sends_nothing() {
    when(userRepository.findAllIds()).thenReturn(List.of());

    subject.accept(new AllAnalysisApiKeyCheckTriggered());

    verifyNoInteractions(eventProducer);
  }

  @Test
  void accept_rethrows_when_listing_users_fails() {
    when(userRepository.findAllIds()).thenThrow(new RuntimeException("db down"));
    var event = new AllAnalysisApiKeyCheckTriggered();

    assertThrows(RuntimeException.class, () -> subject.accept(event));
    verifyNoInteractions(eventProducer);
  }

  @Test
  void accept_rethrows_when_first_send_fails() {
    when(userRepository.findAllIds()).thenReturn(List.of("a"));
    doThrow(new RuntimeException("too big")).when(eventProducer).accept(any());
    var event = new AllAnalysisApiKeyCheckTriggered();

    assertThrows(RuntimeException.class, () -> subject.accept(event));
  }
}
