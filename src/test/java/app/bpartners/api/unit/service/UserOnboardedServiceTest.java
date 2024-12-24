package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.event.model.UserOnboarded;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.OnboardedUser;
import app.bpartners.api.model.User;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.UserOnboardedService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

class UserOnboardedServiceTest {
  SesService mailerMock = mock();
  TemplateResolverEngine engineMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  UserOnboardedService subject =
      new UserOnboardedService(mailerMock, engineMock, subscriptionServiceMock);

  @SneakyThrows
  @Test
  void notify_email_and_register_user_subscription() {
    var userMock = mock(User.class);
    var accountMock = mock(Account.class);
    var accountHolderMock = mock(AccountHolder.class);
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
  }
}
