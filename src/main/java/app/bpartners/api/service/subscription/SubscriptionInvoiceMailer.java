package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.service.EmailInvoiceResolver;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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
  public static final String SUBSCRIPTION_INVOICE_PAID_MAIL_TEMPLATE =
      "subscription_invoice_paid_mail";
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
    if (recipient == null) {
      log.warn(
          "Invoice(id={}) has no configured nor customer email address, "
              + "subscription invoice mail not sent",
          invoice.getId());
      return false;
    }
    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            template, mailContext(invoice, subscriptionPayment));
    try {
      mailer.sendEmail(
          recipient, TECH_RECIPIENT, mailSubject(invoice), htmlBody, attachmentsOf(invoice));
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
    var context = new Context();
    context.setVariable("customerName", invoice.getCustomer().getName());
    context.setVariable("invoiceReference", invoice.getRef());
    context.setVariable("paymentDate", paymentDateOf(invoice));
    context.setVariable("subscriptionPlan", subscriptionPlanOf(invoice, subscriptionPayment));
    context.setVariable("billingInterval", billingIntervalLabelOf(subscriptionPayment, false));
    context.setVariable("billingIntervalLabel", billingIntervalLabelOf(subscriptionPayment, true));
    context.setVariable("billedPeriod", billedPeriodOf(invoice, subscriptionPayment));
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

  private String billingIntervalLabelOf(
      SubscriptionPayment subscriptionPayment, boolean feminineForm) {
    if (subscriptionPayment == null || subscriptionPayment.getBillingInterval() == null) {
      return null;
    }
    return switch (subscriptionPayment.getBillingInterval()) {
      case YEARLY -> feminineForm ? "Annuelle" : "Annuel";
      case MONTHLY -> feminineForm ? "Mensuelle" : "Mensuel";
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
