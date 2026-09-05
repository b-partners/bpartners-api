package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceCreated;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
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
public class CreditOperationInvoiceCreatedService
    implements Consumer<CreditOperationInvoiceCreated> {
  public static final String CREDIT_PURCHASE_INVOICE_MAIL_TEMPLATE = "credit_purchase_invoice_mail";
  private static final String TECH_RECIPIENT = "tech@birdia.fr";
  private final InvoiceRepository invoiceRepository;
  private final CreditPurchaseRepository creditPurchaseRepository;
  private final S3Service s3Service;
  private final FileWriter fileWriter;
  private final SesService mailer;
  private final TemplateResolverEngine templateResolverEngine;
  private final CustomDateFormatter customDateFormatter;
  private final EmailInvoiceResolver emailInvoiceResolver;

  @Override
  public void accept(CreditOperationInvoiceCreated event) {
    var invoice = invoiceRepository.findById(event.getInvoiceId());
    if (invoice == null) {
      log.warn("No Invoice.id={} to send by mail, skipping", event.getInvoiceId());
      return;
    }
    var recipient = emailInvoiceResolver.apply(invoice);
    if (recipient == null) {
      log.warn(
          "Invoice(id={}) customer has no email address, credit purchase invoice mail not sent",
          invoice.getId());
      return;
    }
    var creditPurchase =
        creditPurchaseRepository.findById(event.getCreditPurchaseId()).orElse(null);

    var htmlBody =
        templateResolverEngine.parseTemplateResolver(
            CREDIT_PURCHASE_INVOICE_MAIL_TEMPLATE, mailContext(invoice, creditPurchase));
    try {
      mailer.sendEmail(
          recipient, TECH_RECIPIENT, mailSubject(invoice), htmlBody, attachmentsOf(invoice));
    } catch (IOException | MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    log.info(
        "Credit purchase Invoice(id={}, ref={}) sent to Customer(id={}) with {} in copy",
        invoice.getId(),
        invoice.getRef(),
        invoice.getCustomer().getId(),
        TECH_RECIPIENT);
  }

  private String mailSubject(Invoice invoice) {
    return "[BIRDIA] Votre facture d'achat de crédits " + invoice.getRef() + " est disponible";
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

  private Context mailContext(Invoice invoice, CreditPurchase creditPurchase) {
    var context = new Context();
    context.setVariable("customerName", invoice.getCustomer().getName());
    context.setVariable("invoiceReference", invoice.getRef());
    context.setVariable("purchaseDate", purchaseDateOf(invoice));
    context.setVariable("credits", creditsOf(invoice, creditPurchase));
    context.setVariable("unitPriceWithoutVat", euroOf(unitPriceWithoutVatOf(invoice)));
    context.setVariable("amountWithoutVat", euroOf(invoice.getTotalPriceWithoutVat()));
    context.setVariable("amountWithVat", euroOf(invoice.getTotalPriceWithVat()));
    return context;
  }

  private String purchaseDateOf(Invoice invoice) {
    if (invoice.getSendingDate() != null) {
      return customDateFormatter.formatFrenchDate(invoice.getSendingDate());
    }
    return invoice.getCreatedAt() == null
        ? ""
        : customDateFormatter.formatFrenchDate(invoice.getCreatedAt());
  }

  private Fraction unitPriceWithoutVatOf(Invoice invoice) {
    if (invoice.getProducts().isEmpty()) {
      return new Fraction();
    }
    var unitPrice = invoice.getProducts().getFirst().getUnitPrice();
    return unitPrice == null ? new Fraction() : unitPrice;
  }

  private long creditsOf(Invoice invoice, CreditPurchase creditPurchase) {
    if (creditPurchase != null && creditPurchase.getCredits() != null) {
      return creditPurchase.getCredits();
    }
    if (invoice.getProducts().isEmpty()) {
      return 0L;
    }
    var quantity = invoice.getProducts().getFirst().getQuantity();
    return quantity == null ? 0L : quantity;
  }

  private String euroOf(Fraction amount) {
    return String.format(Locale.FRANCE, "%.2f", amount.getCentsAsDecimal());
  }
}
