package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisApiKeyCheckTriggeredServiceTest {
  UserAnalysisApiKeyRepository repository = mock();
  AnalysisApiKeyApi api = mock();
  EventProducer eventProducer = mock();
  AnalysisApiKeyCheckTriggeredService subject =
      new AnalysisApiKeyCheckTriggeredService(repository, api, eventProducer);

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

    assertThrows(
        RuntimeException.class,
        () -> subject.accept(new AnalysisApiKeyCheckTriggered("a", List.of("b"))));
  }

  @Test
  void accept_with_null_next_user_ids_ends_chain() {
    subject.accept(new AnalysisApiKeyCheckTriggered("a", null));

    verifyNoInteractions(eventProducer);
  }
}
