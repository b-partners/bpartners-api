package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
    var userIdentifier = randomUUID().toString();
    var dashboardApiKey = randomUUID().toString();
    var generatedAnalysisApiKey = randomUUID().toString();
    var user = User.builder().id(userIdentifier).apiKey(dashboardApiKey).build();
    var analysisApiKey = UserAnalysisApiKey.builder().apiKey(generatedAnalysisApiKey).build();
    when(serviceMock.getAnalysisApiKey(any())).thenReturn(analysisApiKey);
    when(userRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    assertDoesNotThrow(() -> subject.accept(new UserAnalysisApiKeyRequested(user)));

    var savedUserCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepositoryMock).save(savedUserCaptor.capture());
    var savedUser = savedUserCaptor.getValue();
    assertEquals(userIdentifier, savedUser.getId());
    assertEquals(List.of(analysisApiKey), savedUser.getAnalysisApiKeys());
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(listCaptor.capture());
    var userOnboardedNotificationRequested =
        (UserOnboardedNotificationRequested) listCaptor.getValue().getFirst();
    assertEquals(
        new UserOnboardedNotificationRequested(userIdentifier), userOnboardedNotificationRequested);
  }

  @Test
  void keeps_user_dashboard_api_key_when_analysis_key_is_created() {
    var dashboardApiKey = randomUUID().toString();
    var user = User.builder().id(randomUUID().toString()).apiKey(dashboardApiKey).build();
    var analysisApiKey = UserAnalysisApiKey.builder().apiKey(randomUUID().toString()).build();
    when(serviceMock.getAnalysisApiKey(any())).thenReturn(analysisApiKey);
    when(userRepositoryMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.accept(new UserAnalysisApiKeyRequested(user));

    var savedUserCaptor = ArgumentCaptor.forClass(User.class);
    verify(userRepositoryMock).save(savedUserCaptor.capture());
    assertEquals(dashboardApiKey, savedUserCaptor.getValue().getApiKey());
  }

  @Test
  void notifies_onboarding_even_when_analysis_key_creation_fails() {
    var userIdentifier = randomUUID().toString();
    var user = User.builder().id(userIdentifier).apiKey(randomUUID().toString()).build();
    when(serviceMock.getAnalysisApiKey(any())).thenThrow(new RuntimeException("api down"));

    assertDoesNotThrow(() -> subject.accept(new UserAnalysisApiKeyRequested(user)));

    verify(userRepositoryMock, never()).save(any());
    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(listCaptor.capture());
    assertEquals(
        new UserOnboardedNotificationRequested(userIdentifier),
        (UserOnboardedNotificationRequested) listCaptor.getValue().getFirst());
  }
}
