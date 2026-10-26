package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyUpdate;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyUpdateServiceTest {
  UserRepository userRepositoryMock = mock();
  AnalysisApiKeyApi analysisApiKeyApiMock = mock();
  UserAnalysisApiKeyUpdateService subject =
      new UserAnalysisApiKeyUpdateService(userRepositoryMock, analysisApiKeyApiMock);

  private static UserAnalysisApiKey enabledKey(String value) {
    return UserAnalysisApiKey.builder()
        .id(randomUUID().toString())
        .apiKey(value)
        .enabled(true)
        .build();
  }

  private User userWith(UserAnalysisApiKey... keys) {
    var user =
        User.builder()
            .id(randomUUID().toString())
            .analysisApiKeys(new ArrayList<>(List.of(keys)))
            .build();
    when(userRepositoryMock.getById(user.getId())).thenReturn(user);
    return user;
  }

  @Test
  void disables_only_the_orphan_key() {
    var known = enabledKey("known");
    var orphan = enabledKey("orphan");
    var user = userWith(known, orphan);
    when(analysisApiKeyApiMock.isRecognized("known")).thenReturn(true);
    when(analysisApiKeyApiMock.isRecognized("orphan")).thenReturn(false);

    subject.accept(new UserAnalysisApiKeyUpdate(user.getId()));

    assertTrue(known.isEnabled());
    assertFalse(orphan.isEnabled());
    assertNotNull(orphan.getExpirationDatetime());
    verify(userRepositoryMock).save(user);
  }

  @Test
  void does_nothing_when_keys_are_known() {
    var known = enabledKey("known");
    var user = userWith(known);
    when(analysisApiKeyApiMock.isRecognized("known")).thenReturn(true);

    subject.accept(new UserAnalysisApiKeyUpdate(user.getId()));

    verify(userRepositoryMock, never()).save(any());
  }

  @Test
  void changes_nothing_when_geo_jobs_cannot_be_reached() {
    var key = enabledKey("key");
    var user = userWith(key);
    when(analysisApiKeyApiMock.isRecognized("key")).thenThrow(new IllegalStateException("down"));
    var event = new UserAnalysisApiKeyUpdate(user.getId());

    assertThrows(IllegalStateException.class, () -> subject.accept(event));

    verify(userRepositoryMock, never()).save(any());
    assertTrue(key.isEnabled());
  }
}
