package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.UserApiKeyUpdate;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import org.junit.jupiter.api.Test;

class UserApiKeyUpdateServiceTest {
  UserRepository userRepositoryMock = mock();
  UserApiKeyUpdateService subject = new UserApiKeyUpdateService(userRepositoryMock);

  @Test
  void generates_dashboard_key_when_missing() {
    var user = User.builder().id(randomUUID().toString()).build();
    when(userRepositoryMock.getById(user.getId())).thenReturn(user);

    subject.accept(new UserApiKeyUpdate(user.getId()));

    assertNotNull(user.getApiKey());
    verify(userRepositoryMock).save(user);
  }

  @Test
  void keeps_existing_dashboard_key() {
    var user = User.builder().id(randomUUID().toString()).apiKey("key").build();
    when(userRepositoryMock.getById(user.getId())).thenReturn(user);

    subject.accept(new UserApiKeyUpdate(user.getId()));

    verify(userRepositoryMock, never()).save(any());
  }
}
