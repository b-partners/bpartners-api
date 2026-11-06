package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyDeactivation;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyDeactivationServiceTest {
  UserRepository userRepositoryMock = mock();
  UserAnalysisApiKeyDeactivationService subject =
      new UserAnalysisApiKeyDeactivationService(userRepositoryMock);

  private static UserAnalysisApiKey enabledKey() {
    return UserAnalysisApiKey.builder()
        .id(randomUUID().toString())
        .apiKey(randomUUID().toString())
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
  void deactivates_only_the_given_key() {
    var target = enabledKey();
    var other = enabledKey();
    var user = userWith(target, other);

    subject.accept(new UserAnalysisApiKeyDeactivation(user.getId(), target.getId()));

    assertFalse(target.isEnabled());
    assertNotNull(target.getExpirationDatetime());
    assertTrue(other.isEnabled());
    verify(userRepositoryMock).save(user);
  }

  @Test
  void does_nothing_when_key_is_already_deactivated() {
    var key = enabledKey();
    key.setEnabled(false);
    var user = userWith(key);

    subject.accept(new UserAnalysisApiKeyDeactivation(user.getId(), key.getId()));

    verify(userRepositoryMock, never()).save(any());
  }
}
