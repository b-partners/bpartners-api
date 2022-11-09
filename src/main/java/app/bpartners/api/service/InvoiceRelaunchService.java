package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.InvoiceRelaunchValidator;
import app.bpartners.api.repository.InvoiceRelaunchConfRepository;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.InvoiceRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;

@Service
@AllArgsConstructor
public class InvoiceRelaunchService {
  private final InvoiceRelaunchConfRepository repository;
  private final InvoiceRelaunchRepository invoiceRelaunchRepository;
  private final InvoiceRelaunchValidator invoiceRelaunchValidator;
  private final InvoiceRepository invoiceRepository;
  private final AccountHolderService holderService;
  private final EventProducer eventProducer;
  private final PrincipalProvider auth;

  public InvoiceRelaunchConf getByAccountId(String accountId) {
    return repository.getByAccountId(accountId);
  }

  public InvoiceRelaunchConf saveConf(String accountId, InvoiceRelaunchConf invoiceRelaunchConf) {
    return repository.save(invoiceRelaunchConf, accountId);
  }

  public InvoiceRelaunch relaunchInvoice(String invoiceId, String subject, String message) {
    Invoice invoice = invoiceRepository.getById(invoiceId);
    invoiceRelaunchValidator.accept(invoice);
    InvoiceRelaunch invoiceRelaunch = invoiceRelaunchRepository.save(invoice);
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(invoice.getAccount().getId());
    eventProducer.accept(
        List.of(getTypedInvoiceRelaunched(
            invoiceRelaunch.getInvoice(), accountHolder, subject, message)));
    return invoiceRelaunch;
  }

  public List<InvoiceRelaunch> getRelaunchesByInvoiceId(
      String invoiceId,
      PageFromOne page,
      BoundedPageSize pageSize, String type) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return invoiceRelaunchRepository.getByInvoiceId(invoiceId, type, pageable);
  }

  //TODO: set it again in template resolver when the load style baseUrl is set
  private String emailBody(String emailMessage, AccountHolder accountHolder) {
    return "<html>\n"
        + "    <body style=\"font-family: 'Gill Sans'\">\n"
        + "        <h2 style=color:#8d2158;>" + accountHolder.getName() + "</h2>\n"
        + emailMessage //TODO: avoid the SQL injection
        + "        <p>Bien à vous et merci pour votre confiance.</p>\n"
        + "    </body>\n"
        + "</html>";
  }

  //TODO: persist the default email message and get it instead
  private String defaultEmailMessage(String type, Invoice invoice) {
    return "        <p>Bonjour,</p>\n"
        + "        <p>\n"
        + "            Retrouvez-ci joint votre " + type + " enregistré à la référence "
        + invoice.getRef() + "\n"
        + "        </p>\n";
  }

  private TypedInvoiceRelaunchSaved getTypedInvoiceRelaunched(
      Invoice invoice, AccountHolder accountHolder, String subject, String emailMessage) {
    String type = getStatusValue(invoice.getStatus());
    subject = subject == null
        ? type + " " + invoice.getRef() :
        subject;    //TODO: if invoice has already been relaunched then change this
    emailMessage = emailMessage == null
        ? defaultEmailMessage(type.toLowerCase(), invoice) : emailMessage;
    String recipient = invoice.getInvoiceCustomer().getEmail();

    return toTypedEvent(
        recipient, subject, emailBody(emailMessage, accountHolder), subject + PDF_EXTENSION,
        invoice, accountHolder);
  }

  private TypedInvoiceRelaunchSaved toTypedEvent(String recipient, String subject, String emailBody,
                                                 String attachmentName, Invoice invoice,
                                                 AccountHolder accountHolder) {
    return new TypedInvoiceRelaunchSaved(InvoiceRelaunchSaved.builder()
        .subject(subject)
        .recipient(recipient)
        .htmlBody(emailBody)
        .attachmentName(attachmentName)
        .invoice(invoice)
        .accountHolder(accountHolder)
        .logoFileId(userLogoFileId())
        .build());
  }

  private String getStatusValue(InvoiceStatus status) {
    if (status.equals(PROPOSAL) || status.equals(DRAFT)) {
      return "Devis";
    }
    if (status.equals(CONFIRMED) || status.equals(PAID)) {
      return "Facture";
    }
    throw new BadRequestException("Unknown status : " + status);
  }

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }
}
