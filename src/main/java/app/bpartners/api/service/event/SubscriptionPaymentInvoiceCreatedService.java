package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceCreated;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.EmailInvoiceResolver;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
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
public class SubscriptionPaymentInvoiceCreatedService
    implements Consumer<SubscriptionPaymentInvoiceCreated> {
  public static final String SUBSCRIPTION_INVOICE_MAIL_TEMPLATE = "subscription_invoice_mail";
  private static final String TECH_RECIPIENT = "tech@birdia.fr";
  private final InvoiceRepository invoiceRepository;
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final S3Service s3Service;
  private final FileWriter fileWriter;
  private final SesService mailer;
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;
  private final EmailInvoiceResolver emailInvoiceResolver;

  @Override
  public void accept(SubscriptionPaymentInvoiceCreated event) {
    var invoice = invoiceRepository.findById(event.getInvoiceId());
    if (invoice == null) {
      log.warn("No Invoice.id={} to send by mail, skipping", event.getInvoiceId());
      return;
    }
    var recipient = emailInvoiceResolver.apply(invoice);
    if (recipient == null) {
      log.warn(
          "Invoice(id={}) has no configured nor customer email address, "
              + "subscription invoice mail not sent",
          invoice.getId());
      return;
    }
    var subscriptionPayment =
        subscriptionPaymentRepository.findById(event.getSubscriptionPaymentId()).orElse(null);

    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            SUBSCRIPTION_INVOICE_MAIL_TEMPLATE, mailContext(invoice, subscriptionPayment));
    try {
      mailer.sendEmail(
          recipient, TECH_RECIPIENT, mailSubject(invoice), htmlBody, attachmentsOf(invoice));
    } catch (IOException | MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    log.info(
        "Subscription Invoice(id={}, ref={}) sent to Customer(id={}) with {} in copy",
        invoice.getId(),
        invoice.getRef(),
        invoice.getCustomer().getId(),
        TECH_RECIPIENT);
  }

  private String mailSubject(Invoice invoice) {
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
    context.setVariable("billingInterval", billingIntervalLabelOf(subscriptionPayment));
    context.setVariable("billedPeriod", billedPeriodOf(subscriptionPayment));
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

  private String billingIntervalLabelOf(SubscriptionPayment subscriptionPayment) {
    if (subscriptionPayment == null || subscriptionPayment.getBillingInterval() == null) {
      return null;
    }
    return switch (subscriptionPayment.getBillingInterval()) {
      case YEARLY -> "Annuelle";
      case MONTHLY -> "Mensuelle";
    };
  }

  private String billedPeriodOf(SubscriptionPayment subscriptionPayment) {
    if (subscriptionPayment == null
        || subscriptionPayment.getPeriodStartDatetime() == null
        || subscriptionPayment.getPeriodEndDatetime() == null) {
      return null;
    }
    return customDateFormatter.formatFrenchDate(subscriptionPayment.getPeriodStartDatetime())
        + " au "
        + customDateFormatter.formatFrenchDate(subscriptionPayment.getPeriodEndDatetime());
  }

  private String euroOf(Fraction amount) {
    return String.format(Locale.FRANCE, "%.2f", amount.getCentsAsDecimal());
  }
}
