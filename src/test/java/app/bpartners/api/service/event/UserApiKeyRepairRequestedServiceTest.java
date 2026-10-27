package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyCreation;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyDeactivation;
import app.bpartners.api.endpoint.event.model.UserApiKeyRepairRequested;
import app.bpartners.api.endpoint.event.model.UserApiKeySync;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserApiKeyRepairRequestedServiceTest {
  UserRepository userRepositoryMock = mock();
  AnalysisApiKeyApi analysisApiKeyApiMock = mock();
  UserApiKeySyncService apiKeySyncMock = mock();
  UserAnalysisApiKeyCreationService analysisCreationMock = mock();
  UserAnalysisApiKeyDeactivationService analysisDeactivationMock = mock();
  UserApiKeyRepairRequestedService subject =
      new UserApiKeyRepairRequestedService(
          userRepositoryMock,
          analysisApiKeyApiMock,
          apiKeySyncMock,
          analysisCreationMock,
          analysisDeactivationMock);

  private static UserAnalysisApiKey enabledKey() {
    return UserAnalysisApiKey.builder()
        .id(randomUUID().toString())
        .apiKey(randomUUID().toString())
        .enabled(true)
        .build();
  }

  @Test
  void syncs_creates_then_deactivates_only_the_unknown_keys() {
    var known = enabledKey();
    var orphan = enabledKey();
    var user = User.builder().id("user_id").analysisApiKeys(List.of(known, orphan)).build();
    when(userRepositoryMock.getById("user_id")).thenReturn(user);
    when(analysisApiKeyApiMock.isRecognized(known.getApiKey())).thenReturn(true);
    when(analysisApiKeyApiMock.isRecognized(orphan.getApiKey())).thenReturn(false);

    subject.accept(new UserApiKeyRepairRequested("user_id"));

    var order = inOrder(apiKeySyncMock, analysisCreationMock, analysisDeactivationMock);
    order.verify(apiKeySyncMock).accept(new UserApiKeySync("user_id"));
    order.verify(analysisCreationMock).accept(new UserAnalysisApiKeyCreation("user_id"));
    order
        .verify(analysisDeactivationMock)
        .accept(new UserAnalysisApiKeyDeactivation("user_id", orphan.getId()));
    verify(analysisDeactivationMock, never())
        .accept(new UserAnalysisApiKeyDeactivation("user_id", known.getId()));
  }

  @Test
  void deactivates_nothing_when_geo_jobs_knows_all_keys() {
    var known = enabledKey();
    var user = User.builder().id("user_id").analysisApiKeys(List.of(known)).build();
    when(userRepositoryMock.getById("user_id")).thenReturn(user);
    when(analysisApiKeyApiMock.isRecognized(known.getApiKey())).thenReturn(true);

    subject.accept(new UserApiKeyRepairRequested("user_id"));

    verify(analysisDeactivationMock, never()).accept(any());
  }
}
