package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AnalysisApiKeyCheckTriggeredServiceTest {
  UserAnalysisApiKeyRepository repository = mock();
  AnalysisApiKeyApi api = mock();
  EventProducer eventProducer = mock();
  UserRepository userRepository = mock();
  AnalysisApiKeyCheckTriggeredService subject =
      new AnalysisApiKeyCheckTriggeredService(repository, api, eventProducer, userRepository);

  @Test
  void accept_checks_only_enabled_keys() {
    var enabled = UserAnalysisApiKey.builder().id("1").apiKey("enabled-key").enabled(true).build();
    var disabled = UserAnalysisApiKey.builder().id("2").apiKey("old-key").enabled(false).build();
    when(repository.getAllByUserId("user")).thenReturn(List.of(enabled, disabled));
    when(api.isRecognized("enabled-key")).thenReturn(false);

    subject.accept(new AnalysisApiKeyCheckTriggered("user"));

    verify(api).isRecognized("enabled-key");
    verify(api, never()).isRecognized("old-key");
  }

  @Test
  void accept_without_enabled_key_does_not_call_geo_jobs() {
    when(repository.getAllByUserId("user")).thenReturn(List.of());

    subject.accept(new AnalysisApiKeyCheckTriggered("user"));

    verifyNoInteractions(api);
  }

  @Test
  void accept_triggers_next_user_even_if_check_fails() {
    when(repository.getAllByUserId("a")).thenThrow(new RuntimeException("boom"));

    subject.accept(new AnalysisApiKeyCheckTriggered("a", List.of("b", "c")));

    verify(eventProducer).accept(List.of(new AnalysisApiKeyCheckTriggered("b", List.of("c"))));
  }

  @Test
  void accept_recognized_key_sends_no_event_when_last_user() {
    var key = UserAnalysisApiKey.builder().id("1").apiKey("key").enabled(true).build();
    when(repository.getAllByUserId("user")).thenReturn(List.of(key));
    when(api.isRecognized("key")).thenReturn(true);

    subject.accept(new AnalysisApiKeyCheckTriggered("user"));

    verifyNoInteractions(eventProducer);
  }

  @Test
  void accept_rethrows_when_triggering_next_fails() {
    doThrow(new RuntimeException("sqs down")).when(eventProducer).accept(any());

    var event = new AnalysisApiKeyCheckTriggered("a", List.of("b"));

    assertThrows(RuntimeException.class, () -> subject.accept(event));
  }

  @Test
  void accept_with_null_next_user_ids_ends_chain() {
    subject.accept(new AnalysisApiKeyCheckTriggered("a", null));

    verifyNoInteractions(eventProducer);
  }

  @Test
  void accept_at_chunk_end_triggers_next_chunk_from_offset() {
    var ids = IntStream.range(0, 2500).mapToObj(i -> "u" + i).toList();
    when(userRepository.findAllIds()).thenReturn(ids);

    subject.accept(new AnalysisApiKeyCheckTriggered("u999", List.of(), 1000));

    verify(eventProducer)
        .accept(List.of(new AnalysisApiKeyCheckTriggered("u1000", ids.subList(1001, 2000), 2000)));
  }

  @Test
  void accept_last_chunk_has_no_next_offset() {
    var ids = IntStream.range(0, 1500).mapToObj(i -> "u" + i).toList();
    when(userRepository.findAllIds()).thenReturn(ids);

    subject.accept(new AnalysisApiKeyCheckTriggered("u999", List.of(), 1000));

    verify(eventProducer)
        .accept(List.of(new AnalysisApiKeyCheckTriggered("u1000", ids.subList(1001, 1500), null)));
  }

  @Test
  void accept_keeps_offset_while_chaining_inside_chunk() {
    subject.accept(new AnalysisApiKeyCheckTriggered("a", List.of("b"), 1000));

    verify(eventProducer).accept(List.of(new AnalysisApiKeyCheckTriggered("b", List.of(), 1000)));
  }

  @Test
  void accept_with_offset_past_the_end_sends_nothing() {
    when(userRepository.findAllIds()).thenReturn(List.of("a"));

    subject.accept(new AnalysisApiKeyCheckTriggered("a", List.of(), 1000));

    verifyNoInteractions(eventProducer);
  }
}
