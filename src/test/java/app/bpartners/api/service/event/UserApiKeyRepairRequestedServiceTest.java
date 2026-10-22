package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.UserApiKeyRepairRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserApiKeyRepairRequestedServiceTest {
  UserRepository userRepositoryMock = mock();
  UserAnalysisApiKeyService analysisApiKeyServiceMock = mock();
  AnalysisApiKeyApi analysisApiKeyApiMock = mock();
  UserApiKeyRepairRequestedService subject =
      new UserApiKeyRepairRequestedService(
          userRepositoryMock, analysisApiKeyServiceMock, analysisApiKeyApiMock);

  private static UserAnalysisApiKey enabledKey(String value) {
    return UserAnalysisApiKey.builder()
        .id(randomUUID().toString())
        .apiKey(value)
        .enabled(true)
        .build();
  }

  private User userWith(String dashboardKey, UserAnalysisApiKey... keys) {
    var user =
        User.builder()
            .id(randomUUID().toString())
            .apiKey(dashboardKey)
            .analysisApiKeys(new ArrayList<>(List.of(keys)))
            .build();
    when(userRepositoryMock.getById(user.getId())).thenReturn(user);
    return user;
  }

  private User savedUser() {
    var captor = ArgumentCaptor.forClass(User.class);
    verify(userRepositoryMock).save(captor.capture());
    return captor.getValue();
  }

  @Test
  void does_nothing_when_keys_are_fine() {
    var key = enabledKey(randomUUID().toString());
    var user = userWith(randomUUID().toString(), key);
    when(analysisApiKeyApiMock.isRecognized(key.getApiKey())).thenReturn(true);

    subject.accept(new UserApiKeyRepairRequested(user.getId()));

    verify(userRepositoryMock, never()).save(any());
    verify(analysisApiKeyServiceMock, never()).getAnalysisApiKey(any());
  }

  @Test
  void creates_analysis_key_when_user_has_none() {
    var user = userWith(randomUUID().toString());
    var created = enabledKey(randomUUID().toString());
    when(analysisApiKeyServiceMock.getAnalysisApiKey(user)).thenReturn(created);

    subject.accept(new UserApiKeyRepairRequested(user.getId()));

    assertEquals(List.of(created), savedUser().getAnalysisApiKeys());
  }

  @Test
  void generates_dashboard_key_when_missing() {
    var user = userWith(null);
    when(analysisApiKeyServiceMock.getAnalysisApiKey(user))
        .thenReturn(enabledKey(randomUUID().toString()));

    subject.accept(new UserApiKeyRepairRequested(user.getId()));

    assertNotNull(savedUser().getApiKey());
  }

  @Test
  void adds_the_known_key_and_disables_the_orphan() {
    var orphan = enabledKey(randomUUID().toString());
    var user = userWith(randomUUID().toString(), orphan);
    var known = enabledKey(randomUUID().toString());
    when(analysisApiKeyApiMock.isRecognized(orphan.getApiKey())).thenReturn(false);
    when(analysisApiKeyServiceMock.getAnalysisApiKey(user)).thenReturn(known);

    subject.accept(new UserApiKeyRepairRequested(user.getId()));

    var saved = savedUser();
    assertEquals(2, saved.getAnalysisApiKeys().size());
    assertFalse(orphan.isEnabled());
    assertNotNull(orphan.getExpirationDatetime());
    assertTrue(known.isEnabled());
  }

  @Test
  void disables_only_the_orphan_when_another_key_is_known() {
    var known = enabledKey(randomUUID().toString());
    var orphan = enabledKey(randomUUID().toString());
    var user = userWith(randomUUID().toString(), known, orphan);
    when(analysisApiKeyApiMock.isRecognized(known.getApiKey())).thenReturn(true);
    when(analysisApiKeyApiMock.isRecognized(orphan.getApiKey())).thenReturn(false);

    subject.accept(new UserApiKeyRepairRequested(user.getId()));

    savedUser();
    verify(analysisApiKeyServiceMock, never()).getAnalysisApiKey(any());
    assertTrue(known.isEnabled());
    assertFalse(orphan.isEnabled());
  }

  @Test
  void changes_nothing_when_geo_jobs_cannot_be_reached() {
    var key = enabledKey(randomUUID().toString());
    var user = userWith(randomUUID().toString(), key);
    when(analysisApiKeyApiMock.isRecognized(key.getApiKey()))
        .thenThrow(new IllegalStateException("geo-jobs down"));

    var event = new UserApiKeyRepairRequested(user.getId());

    assertThrows(IllegalStateException.class, () -> subject.accept(event));

    verify(userRepositoryMock, never()).save(any());
    assertTrue(key.isEnabled());
  }
}
