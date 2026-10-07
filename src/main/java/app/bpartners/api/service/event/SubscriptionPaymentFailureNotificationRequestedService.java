package app.bpartners.api.service.event;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.event.model.SubscriptionPaymentFailureNotificationRequested;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.user.UserService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentFailureNotificationRequestedService
    implements Consumer<SubscriptionPaymentFailureNotificationRequested> {
  public static final String SUBSCRIPTION_PAYMENT_FAILED_MAIL_TEMPLATE =
      "subscription_payment_failed_mail";
  private static final String TECH_RECIPIENT = "tech@birdia.fr";
  private final UserService userService;
  private final SesService mailer;
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;

  @Override
  public void accept(SubscriptionPaymentFailureNotificationRequested event) {
    var user = userService.getUserByIdWithoutPaymentMethod(event.getUserId());
    var recipient = user.getEmail();
    if (recipient == null || recipient.isBlank()) {
      log.warn(
          "User(id={}) has no email address, payment failure of Stripe Invoice(id={}) not notified",
          event.getUserId(),
          event.getStripeInvoiceId());
      return;
    }
    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            SUBSCRIPTION_PAYMENT_FAILED_MAIL_TEMPLATE, mailContext(user.getName(), event));
    try {
      mailer.sendEmail(recipient, null, mailSubject(), htmlBody, List.of(), TECH_RECIPIENT);
    } catch (IOException | MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    log.info(
        "Subscription payment failure of Stripe Invoice(id={}) notified to User(id={})",
        event.getStripeInvoiceId(),
        event.getUserId());
  }

  private String mailSubject() {
    return "[BIRDIA] Le prélèvement de votre abonnement n'a pas abouti";
  }

  private Context mailContext(
      String customerName, SubscriptionPaymentFailureNotificationRequested event) {
    var context = new Context();
    context.setVariable("customerName", customerName);
    context.setVariable("subscriptionPlan", planNameOf(event));
    context.setVariable("amountWithVat", euroOf(event.getAmountInCentsWithVat()));
    context.setVariable(
        "billedPeriod",
        frenchPeriodOf(event.getPeriodStartDatetime(), event.getPeriodEndDatetime()));
    context.setVariable("nextPaymentAttempt", frenchDateOf(event.getNextPaymentAttemptDatetime()));
    context.setVariable("paymentUrl", event.getPaymentUrl());
    return context;
  }

  private String planNameOf(SubscriptionPaymentFailureNotificationRequested event) {
    return event.getPlanName() == null ? SubscriptionPayment.DEFAULT_LABEL : event.getPlanName();
  }

  private String frenchPeriodOf(Instant periodStart, Instant periodEnd) {
    if (periodStart == null || periodEnd == null) {
      return null;
    }
    return customDateFormatter.formatFrenchDate(periodStart)
        + " au "
        + customDateFormatter.formatFrenchDate(periodEnd);
  }

  private String frenchDateOf(Instant instant) {
    return instant == null ? null : customDateFormatter.formatFrenchDate(instant);
  }

  private String euroOf(Long amountInCents) {
    return amountInCents == null
        ? null
        : String.format(Locale.FRANCE, "%.2f", amountInCents / 100d);
  }
}
