package app.bpartners.api.unit.service;

import static app.bpartners.api.service.event.SubscriptionPaymentFailureNotificationRequestedService.SUBSCRIPTION_PAYMENT_FAILED_MAIL_TEMPLATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.SubscriptionPaymentFailureNotificationRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.SubscriptionPaymentFailureNotificationRequestedService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.Instant;
import javax.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;

class SubscriptionPaymentFailureNotificationRequestedServiceTest {
  static final String USER_ID = "user_id";
  static final String USER_EMAIL = "jean@dupont.fr";
  static final String TECH_RECIPIENT = "tech@birdia.fr";
  static final String PAYMENT_URL = "https://pay.stripe.com/in_failed";

  UserService userService = mock();
  SesService sesService = mock();
  TemplateResolverEngine templateResolverEngine = new TemplateResolverEngine();
  SubscriptionPaymentFailureNotificationRequestedService subject =
      new SubscriptionPaymentFailureNotificationRequestedService(
          userService, sesService, templateResolverEngine, new CustomDateFormatter());

  private SubscriptionPaymentFailureNotificationRequested givenEvent() {
    return SubscriptionPaymentFailureNotificationRequested.builder()
        .userId(USER_ID)
        .stripeInvoiceId("in_failed")
        .planName("Pro")
        .amountInCentsWithVat(12_000L)
        .periodStartDatetime(Instant.parse("2026-10-01T00:00:00Z"))
        .periodEndDatetime(Instant.parse("2026-10-31T21:59:59Z"))
        .nextPaymentAttemptDatetime(Instant.parse("2026-10-08T10:00:00Z"))
        .paymentUrl(PAYMENT_URL)
        .build();
  }

  private void givenSubscriber(String email) {
    when(userService.getUserByIdWithoutPaymentMethod(USER_ID))
        .thenReturn(
            User.builder().id(USER_ID).firstName("Jean").lastName("Dupont").email(email).build());
  }

  @Test
  void a_payment_failure_is_mailed_to_the_subscriber() throws MessagingException, IOException {
    givenSubscriber(USER_EMAIL);
    var mailSubject = ArgumentCaptor.forClass(String.class);
    var htmlBody = ArgumentCaptor.forClass(String.class);

    subject.accept(givenEvent());

    verify(sesService)
        .sendEmail(
            eq(USER_EMAIL),
            isNull(),
            mailSubject.capture(),
            htmlBody.capture(),
            anyList(),
            eq(TECH_RECIPIENT));
    assertEquals(
        "[BIRDIA] Le prélèvement de votre abonnement n'a pas abouti", mailSubject.getValue());
    var html = htmlBody.getValue();
    assertTrue(html.contains("Jean Dupont"));
    assertTrue(html.contains("Pro"));
    assertTrue(html.contains("120,00 €"));
    assertTrue(html.contains("01/10/2026 au 31/10/2026"));
    assertTrue(html.contains("08/10/2026"));
    assertTrue(html.contains(PAYMENT_URL));
  }

  @Test
  void a_subscriber_without_email_is_not_mailed() throws MessagingException, IOException {
    givenSubscriber(null);

    subject.accept(givenEvent());

    verify(sesService, never())
        .sendEmail(anyString(), any(), anyString(), anyString(), anyList(), anyString());
  }

  @Test
  void a_payment_failure_without_next_attempt_nor_payment_url_is_still_mailed()
      throws MessagingException, IOException {
    givenSubscriber(USER_EMAIL);
    var htmlBody = ArgumentCaptor.forClass(String.class);

    subject.accept(
        givenEvent().toBuilder().nextPaymentAttemptDatetime(null).paymentUrl(null).build());

    verify(sesService)
        .sendEmail(
            eq(USER_EMAIL), isNull(), anyString(), htmlBody.capture(), anyList(), anyString());
    var html = htmlBody.getValue();
    assertTrue(html.contains("Merci de régulariser votre règlement"));
    assertFalse(html.contains("pay.stripe.com"));
  }

  @Test
  void the_mail_template_renders_without_any_resolved_detail() {
    var html =
        templateResolverEngine.parseTemplateResolver(
            SUBSCRIPTION_PAYMENT_FAILED_MAIL_TEMPLATE, new Context());

    assertTrue(html.contains("BIRDIA"));
  }
}
