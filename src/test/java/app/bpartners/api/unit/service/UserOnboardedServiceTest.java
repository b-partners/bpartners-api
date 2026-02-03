package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyRequested;
import app.bpartners.api.endpoint.event.model.UserOnboarded;
import app.bpartners.api.model.*;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.customer.UserCustomerConverter;
import app.bpartners.api.service.event.UserOnboardedService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class UserOnboardedServiceTest {
  SesService mailerMock = mock();
  TemplateResolverEngine engineMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  UserCustomerConverter userCustomerConverterMock = mock();
  EventProducer eventProducerMock = mock();
  UserOnboardedService subject =
      new UserOnboardedService(
          mailerMock,
          engineMock,
          subscriptionServiceMock,
          userCustomerConverterMock,
          eventProducerMock);

  @SneakyThrows
  @Test
  void notify_email_and_register_user_subscription() {
    var user = User.builder().email("random" + randomUUID() + "@mail.com").build();
    var accountMock = mock(Account.class);
    var accountHolderMock = mock(AccountHolder.class);
    when(userCustomerConverterMock.apply(user)).thenReturn(mock(Customer.class));
    when(subscriptionServiceMock.createOrLinkUserSubscription(any()))
        .thenAnswer(
            invocationOnMock -> new UserSubscription(invocationOnMock.getArgument(0), List.of()));

    var emailRecipient = "recipient@email.com";
    var emailSubject = "subject";
    var event =
        UserOnboarded.builder()
            .onboardedUser(
                OnboardedUser.builder()
                    .onboardedUser(user)
                    .onboardedAccount(accountMock)
                    .onboardedAccountHolder(accountHolderMock)
                    .build())
            .subject(emailSubject)
            .recipientEmail(emailRecipient)
            .build();

    assertDoesNotThrow(() -> subject.accept(event));

    var userCaptor = ArgumentCaptor.forClass(User.class);
    verify(subscriptionServiceMock).createOrLinkUserSubscription(userCaptor.capture());
    verify(engineMock).parseTemplateResolver(any(String.class), any(Context.class));
    verify(mailerMock).sendEmail(eq(emailRecipient), any(), eq(emailSubject), any(), any());

    var capturedUserWithApiKey = userCaptor.getValue();
    assertNull(user.getApiKey());
    assertNotNull(capturedUserWithApiKey.getApiKey());

    var eventCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(eventCaptor.capture());

    var captured = (List<UserAnalysisApiKeyRequested>) eventCaptor.getValue();
    assertEquals(1, captured.size());
    assertEquals(new UserAnalysisApiKeyRequested(capturedUserWithApiKey), captured.getFirst());
  }
}
