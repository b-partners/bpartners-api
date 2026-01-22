package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserAnalysisApiKeyRequested;
import app.bpartners.api.endpoint.event.model.UserOnboarded;
import app.bpartners.api.model.*;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.customer.UserCustomerConverter;
import app.bpartners.api.service.event.UserOnboardedService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
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
    var userMock = mock(User.class);
    var accountMock = mock(Account.class);
    var accountHolderMock = mock(AccountHolder.class);
    when(userCustomerConverterMock.apply(userMock)).thenReturn(mock(Customer.class));

    var emailRecipient = "recipient@email.com";
    var emailSubject = "subject";
    var event =
        UserOnboarded.builder()
            .onboardedUser(
                OnboardedUser.builder()
                    .onboardedUser(userMock)
                    .onboardedAccount(accountMock)
                    .onboardedAccountHolder(accountHolderMock)
                    .build())
            .subject(emailSubject)
            .recipientEmail(emailRecipient)
            .build();

    assertDoesNotThrow(() -> subject.accept(event));

    verify(subscriptionServiceMock).createOrLinkUserSubscription(userMock);
    verify(engineMock).parseTemplateResolver(any(String.class), any(Context.class));
    verify(mailerMock).sendEmail(eq(emailRecipient), any(), eq(emailSubject), any(), any());

    var eventCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock).accept(eventCaptor.capture());

    var captured = (List<UserAnalysisApiKeyRequested>) eventCaptor.getValue();
    assertEquals(1, captured.size());
    assertEquals(new UserAnalysisApiKeyRequested(userMock), captured.get(0));
  }
}
