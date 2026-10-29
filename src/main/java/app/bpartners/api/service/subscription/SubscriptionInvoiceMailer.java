package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.service.EmailInvoiceResolver;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import javax.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionInvoiceMailer {
  public static final String SUBSCRIPTION_INVOICE_MAIL_TEMPLATE = "subscription_invoice_mail";
  public static final String CREDIT_NOTE_MAIL_TEMPLATE = "subscription_credit_note_mail";
  public static final String TECH_RECIPIENT = "tech@birdia.fr";

  private final S3Service s3Service;
  private final FileWriter fileWriter;
  private final SesService mailer;
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;
  private final EmailInvoiceResolver emailInvoiceResolver;
  private final SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository;

  public String subscriberRecipientOf(Invoice invoice) {
    return emailInvoiceResolver.apply(invoice);
  }

  public boolean send(
      Invoice invoice, SubscriptionPayment subscriptionPayment, String template, String recipient) {
    return deliver(invoice, () -> mailContext(invoice, subscriptionPayment), template, recipient);
  }

  public boolean sendWithSummary(
      Invoice invoice, SubscriptionMailSummary summary, String template, String recipient) {
    return deliver(invoice, () -> mailContext(invoice, summary), template, recipient);
  }

  public boolean sendCreditNote(
      Invoice invoice, SubscriptionMailSummary summary, String recipient) {
    return deliver(
        invoice,
        () -> mailContext(invoice, summary),
        CREDIT_NOTE_MAIL_TEMPLATE,
        recipient,
        creditNoteMailSubject(invoice),
        List::of);
  }

  private boolean deliver(
      Invoice invoice, Supplier<Context> context, String template, String recipient) {
    return deliver(
        invoice, context, template, recipient, mailSubject(invoice), () -> attachmentsOf(invoice));
  }

  private boolean deliver(
      Invoice invoice,
      Supplier<Context> context,
      String template,
      String recipient,
      String subject,
      Supplier<List<Attachment>> attachments) {
    if (recipient == null) {
      log.warn(
          "Invoice(id={}) has no configured nor customer email address, "
              + "subscription invoice mail not sent",
          invoice.getId());
      return false;
    }
    var htmlBody = templateResolverEngine.parseTemplateResolver(template, context.get());
    try {
      mailer.sendEmail(recipient, TECH_RECIPIENT, subject, htmlBody, attachments.get());
    } catch (IOException | MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    log.info(
        "Subscription Invoice(id={}, ref={}) sent to {} with {} in copy",
        invoice.getId(),
        invoice.getRef(),
        recipient,
        TECH_RECIPIENT);
    return true;
  }

  public String mailSubject(Invoice invoice) {
    return "[BIRDIA] Votre facture d'abonnement " + invoice.getRef() + " est disponible";
  }

  public String creditNoteMailSubject(Invoice invoice) {
    return "Votre facture d'avoir " + invoice.getRef() + " est disponible sur votre dashboard";
  }

  private List<Attachment> attachmentsOf(Invoice invoice) {
    var invoiceFile =
        s3Service.downloadFile(INVOICE, invoice.getFileId(), invoice.getUser().getId());
    return List.of(
        Attachment.builder()
            .name(invoice.getRef())
            .content(fileWriter.writeAsByte(invoiceFile))
            .build());
  }

  private Context mailContext(Invoice invoice, SubscriptionPayment subscriptionPayment) {
    var context = baseContextOf(invoice);
    context.setVariable("subscriptionPlan", subscriptionPlanOf(invoice, subscriptionPayment));
    context.setVariable(
        "billingInterval",
        billingIntervalLabelOf(
            subscriptionPayment == null ? null : subscriptionPayment.getBillingInterval()));
    context.setVariable("billedPeriod", billedPeriodOf(invoice, subscriptionPayment));
    return context;
  }

  private Context mailContext(Invoice invoice, SubscriptionMailSummary summary) {
    var context = baseContextOf(invoice);
    context.setVariable(
        "subscriptionPlan",
        summary.planName() == null ? SubscriptionPayment.DEFAULT_LABEL : summary.planName());
    context.setVariable("billingInterval", billingIntervalLabelOf(summary.billingInterval()));
    context.setVariable("billedPeriod", frenchPeriodOf(summary.periodStart(), summary.periodEnd()));
    return context;
  }

  private Context baseContextOf(Invoice invoice) {
    var context = new Context();
    context.setVariable("customerName", invoice.getCustomer().getName());
    context.setVariable("invoiceReference", invoice.getRef());
    context.setVariable("paymentDate", paymentDateOf(invoice));
    context.setVariable("amountWithoutVat", euroOf(invoice.getTotalPriceWithoutVat()));
    context.setVariable("amountWithVat", euroOf(invoice.getTotalPriceWithVat()));
    return context;
  }

  private String paymentDateOf(Invoice invoice) {
    if (invoice.getSendingDate() != null) {
      return customDateFormatter.formatFrenchDate(invoice.getSendingDate());
    }
    return invoice.getCreatedAt() == null
        ? ""
        : customDateFormatter.formatFrenchDate(invoice.getCreatedAt());
  }

  private String subscriptionPlanOf(Invoice invoice, SubscriptionPayment subscriptionPayment) {
    if (subscriptionPayment != null) {
      return subscriptionPayment.planName();
    }
    return invoice.getProducts().isEmpty()
        ? SubscriptionPayment.DEFAULT_LABEL
        : invoice.getProducts().getFirst().getDescription();
  }

  private String billingIntervalLabelOf(BillingInterval billingInterval) {
    if (billingInterval == null) {
      return null;
    }
    return switch (billingInterval) {
      case YEARLY -> "Annuel";
      case MONTHLY -> "Mensuel";
    };
  }

  private String billedPeriodOf(Invoice invoice, SubscriptionPayment subscriptionPayment) {
    var invoicedPeriod = subscriptionInvoicePeriodRepository.findByInvoiceId(invoice.getId());
    if (invoicedPeriod.isPresent()) {
      return frenchPeriodOf(
          invoicedPeriod.get().getPeriodStartDatetime(),
          invoicedPeriod.get().getPeriodEndDatetime());
    }
    if (subscriptionPayment == null) {
      return null;
    }
    return frenchPeriodOf(
        subscriptionPayment.invoicedPeriodStartOrPeriodStart(),
        subscriptionPayment.invoicedPeriodEndOrPeriodEnd());
  }

  private String frenchPeriodOf(LocalDate periodStart, LocalDate periodEnd) {
    if (periodStart == null || periodEnd == null) {
      return null;
    }
    return customDateFormatter.formatFrenchDate(periodStart)
        + " au "
        + customDateFormatter.formatFrenchDate(periodEnd);
  }

  private String frenchPeriodOf(Instant periodStart, Instant periodEnd) {
    if (periodStart == null || periodEnd == null) {
      return null;
    }
    return customDateFormatter.formatFrenchDate(periodStart)
        + " au "
        + customDateFormatter.formatFrenchDate(periodEnd);
  }

  private String euroOf(Fraction amount) {
    return String.format(Locale.FRANCE, "%.2f", amount.getCentsAsDecimal());
  }
}
