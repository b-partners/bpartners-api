package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyRequested;
import app.bpartners.api.endpoint.event.model.UserOnboardedNotificationRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.service.user.UserAnalysisApiKeyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserAnalysisApiKeyRequestedServiceTest {
  UserRepositoryImpl userRepositoryMock = mock();
  UserAnalysisApiKeyService serviceMock = mock();
  EventProducer eventProducerMock = mock();
  UserAnalysisApiKeyRequestedService subject =
      new UserAnalysisApiKeyRequestedService(userRepositoryMock, serviceMock, eventProducerMock);

  @Test
  void does_not_throws_exception_and_persist_analysis_key_through_user_repository() {
    String userIdentifier = randomUUID().toString();
    List<UserAnalysisApiKey> userAnalysisApiKeysMock = mock();
    UserAnalysisApiKey apiKeyMock = mock();
    var generatedAnalysisApiKey = randomUUID().toString();
    var userMockWithApiKey = mock(User.class);
    var userMock = mock(User.class);
    var userMockBuilder = mock(User.UserBuilder.class);
    var userMockBuilderWithApiKey = mock(User.UserBuilder.class);
    var eventMock = mock(UserAnalysisApiKeyRequested.class);
    when(userMock.getId()).thenReturn(userIdentifier);
    when(userMock.toBuilder()).thenReturn(userMockBuilder);
    when(userMockBuilder.apiKey(generatedAnalysisApiKey)).thenReturn(userMockBuilderWithApiKey);

    when(userRepositoryMock.save(userMock)).thenReturn(userMock);
    when(userAnalysisApiKeysMock.add(apiKeyMock)).thenReturn(true);
    when(apiKeyMock.getApiKey()).thenReturn(generatedAnalysisApiKey);
    when(userMock.getAnalysisApiKeys()).thenReturn(userAnalysisApiKeysMock);
    when(eventMock.getUser()).thenReturn(userMock);
    when(userMock.toBuilder()).thenReturn(userMockBuilder);
    when(userMockBuilder.build()).thenReturn(userMock);
    when(userMockBuilderWithApiKey.build()).thenReturn(userMockWithApiKey);
    when(serviceMock.getAnalysisApiKey(userMock)).thenReturn(apiKeyMock);

    assertDoesNotThrow(() -> subject.accept(eventMock));

    verify(eventMock).getUser();
    verify(serviceMock).getAnalysisApiKey(userMock);
    verify(userRepositoryMock).save(userMockWithApiKey);
    verify(userMock).addUserAnalysisApiKey(apiKeyMock);
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(listCaptor.capture());
    var userOnboardedNotificationRequested =
        (UserOnboardedNotificationRequested) listCaptor.getValue().getFirst();
    assertEquals(
        new UserOnboardedNotificationRequested(userIdentifier), userOnboardedNotificationRequested);
  }
}
