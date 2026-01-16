package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyRequestedServiceTest {
  UserRepositoryImpl userRepositoryMock = mock();
  UserAnalysisApiKeyService serviceMock = mock();
  UserAnalysisApiKeyRequestedService subject =
      new UserAnalysisApiKeyRequestedService(userRepositoryMock, serviceMock);

  @Test
  void accept_ok() {
    List<UserAnalysisApiKey> userAnalysisApiKeysMock = mock();
    UserAnalysisApiKey apiKeyMock = mock();
    User userMock = mock();
    User.UserBuilder userMockBuilder = mock();
    UserAnalysisApiKeyRequested eventMock = mock();

    when(userRepositoryMock.save(userMock)).thenReturn(userMock);
    when(userAnalysisApiKeysMock.add(apiKeyMock)).thenReturn(true);
    when(userMock.getAnalysisApiKeys()).thenReturn(userAnalysisApiKeysMock);
    when(eventMock.getUser()).thenReturn(userMock);
    when(userMock.toBuilder()).thenReturn(userMockBuilder);
    when(userMockBuilder.build()).thenReturn(userMock);
    when(serviceMock.getAnalysisApiKey(userMock)).thenReturn(apiKeyMock);

    assertDoesNotThrow(() -> subject.accept(eventMock));

    verify(userMock).getAnalysisApiKeys();
    verify(eventMock).getUser();
    verify(serviceMock).getAnalysisApiKey(userMock);
    verify(userRepositoryMock).save(userMock);
    verify(userAnalysisApiKeysMock).add(apiKeyMock);
  }
}
