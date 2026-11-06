package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyCreation;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyCreationServiceTest {
  UserRepository userRepositoryMock = mock();
  UserAnalysisApiKeyService analysisApiKeyServiceMock = mock();
  UserAnalysisApiKeyCreationService subject =
      new UserAnalysisApiKeyCreationService(userRepositoryMock, analysisApiKeyServiceMock);

  private User userWith(UserAnalysisApiKey... keys) {
    var user =
        User.builder()
            .id(randomUUID().toString())
            .apiKey(randomUUID().toString())
            .analysisApiKeys(new ArrayList<>(List.of(keys)))
            .build();
    when(userRepositoryMock.getById(user.getId())).thenReturn(user);
    return user;
  }

  @Test
  void adds_the_created_key() {
    var user = userWith();
    var created = UserAnalysisApiKey.builder().apiKey("new").enabled(true).build();
    when(analysisApiKeyServiceMock.getAnalysisApiKey(user)).thenReturn(created);

    subject.accept(new UserAnalysisApiKeyCreation(user.getId()));

    assertEquals(List.of(created), user.getAnalysisApiKeys());
    verify(userRepositoryMock).save(user);
  }

  @Test
  void does_not_add_a_key_the_user_already_has() {
    var existing = UserAnalysisApiKey.builder().apiKey("same").enabled(true).build();
    var user = userWith(existing);
    when(analysisApiKeyServiceMock.getAnalysisApiKey(user))
        .thenReturn(UserAnalysisApiKey.builder().apiKey("same").enabled(true).build());

    subject.accept(new UserAnalysisApiKeyCreation(user.getId()));

    assertEquals(List.of(existing), user.getAnalysisApiKeys());
    verify(userRepositoryMock, never()).save(any());
  }
}
