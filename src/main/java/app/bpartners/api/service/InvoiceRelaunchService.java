package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AccountInvoiceRelaunchConf;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.InvoiceRelaunchValidator;
import app.bpartners.api.repository.AccountInvoiceRelaunchConfRepository;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.service.utils.TemplateResolverUtils;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;

@Service
@AllArgsConstructor
public class InvoiceRelaunchService {
  public static final String MAIL_TEMPLATE = "mail";
  private final AccountInvoiceRelaunchConfRepository repository;
  private final InvoiceRelaunchRepository invoiceRelaunchRepository;
  private final InvoiceRelaunchValidator invoiceRelaunchValidator;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final InvoiceRelaunchConfService relaunchConfService;
  private final AccountHolderService holderService;
  private final EventProducer eventProducer;
  private final PrincipalProvider auth;

  private static String getDefaultSubject(Invoice invoice) {
    return "Votre " + getStatusValue(invoice.getStatus())
        + ", portant la référence " + invoice.getRef() + ", au nom de "
        + invoice.getCustomer().getName().toUpperCase();
  }

  private static String getSubject(AccountHolder accountHolder, String subject) {
    return "[" + accountHolder.getName() + "] " + subject;
  }

  private static String getStatusValue(InvoiceStatus status) {
    if (status.equals(PROPOSAL) || status.equals(DRAFT)) {
      return "devis";
    }
    if (status.equals(CONFIRMED) || status.equals(PAID)) {
      return "facture";
    }
    throw new BadRequestException("Unknown status : " + status);
  }

  //TODO: generalize this so the persist object is the really sent object
  private static String getDefaultEmailPrefix(AccountHolder accountHolder) {
    return "[" + accountHolder.getName() + "] ";
  }

  public AccountInvoiceRelaunchConf getByAccountId(String accountId) {
    return repository.getByAccountId(accountId);
  }

  public AccountInvoiceRelaunchConf saveConf(
      String accountId,
      AccountInvoiceRelaunchConf accountInvoiceRelaunchConf
  ) {
    return repository.save(accountInvoiceRelaunchConf, accountId);
  }

  public InvoiceRelaunch relaunchInvoiceManually(
      String invoiceId, List<String> emailObjectList, List<String> emailBodyList) {
    String emailObject = null;
    if (!emailObjectList.isEmpty()) {
      emailObject = emailObjectList.get(0) == null ? emailObjectList.get(1) :
          emailObjectList.get(0);
    }
    String emailBody = null;
    if (!emailBodyList.isEmpty()) {
      emailBody = emailBodyList.get(0) == null ? emailBodyList.get(1) : emailBodyList.get(0);
    }

    Invoice invoice = invoiceRepository.getById(invoiceId);
    invoiceRelaunchValidator.accept(invoice);

    boolean isUserRelaunched = true;
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(invoice.getAccount().getId());

    InvoiceRelaunch invoiceRelaunch =
        invoiceRelaunchRepository.save(
            invoice, getDefaultEmailPrefix(accountHolder) + emailObject, emailBody,
            isUserRelaunched);

    eventProducer.accept(
        List.of(getTypedInvoiceRelaunched(
            invoiceRelaunch.getInvoice(), accountHolder, emailObject, emailBody)));

    return invoiceRelaunch;
  }

  @Scheduled(cron = "0 0 10 * * *")
  public void relaunch() {
    //TODO : Transactional
    //TODO: next version will persist mailbody.
    LocalDate now = LocalDate.now();
    invoiceJpaRepository.findAllByToBeRelaunched(true).forEach(
        invoice -> {
          InvoiceRelaunchConf conf = relaunchConfService.findByIdInvoice(invoice.getId());
          boolean equalDate =
              now.isEqual(invoice.getSendingDate().plusDays(conf.getDelay()));
          if (equalDate) {
            int size =
                getRelaunchesByInvoiceId(
                    invoice.getId(),
                    new PageFromOne(1),
                    new BoundedPageSize(500),
                    null
                ).size();
            boolean notReachedMaxRehearse = size < conf.getRehearsalNumber();
            if (notReachedMaxRehearse) {
              relaunchInvoiceManually(invoice.getId(), List.of(), List.of());
              if (size + 1 == conf.getRehearsalNumber()) {
                invoice.setToBeRelaunched(false);
                invoiceJpaRepository.save(invoice);
              }
            }
          }
        }
    );
  }

  public List<InvoiceRelaunch> getRelaunchesByInvoiceId(
      String invoiceId,
      PageFromOne page,
      BoundedPageSize pageSize, String type) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return invoiceRelaunchRepository.getByInvoiceId(invoiceId, type, pageable);
  }

  private String emailBody(String customEmailBody, Invoice invoice, AccountHolder accountHolder) {
    return TemplateResolverUtils.parseTemplateResolver(MAIL_TEMPLATE,
        configureContext(invoice, accountHolder, customEmailBody));
  }

  private TypedInvoiceRelaunchSaved getTypedInvoiceRelaunched(
      Invoice invoice, AccountHolder accountHolder, String subject, String customEmailBody) {
    //TODO: if invoice has already been relaunched then change this
    subject = subject == null ? getDefaultSubject(invoice) : subject;
    String recipient = invoice.getCustomer().getEmail();

    return toTypedEvent(
        recipient, getSubject(accountHolder, subject),
        emailBody(customEmailBody, invoice, accountHolder),
        invoice.getRef() + PDF_EXTENSION, invoice, accountHolder);
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

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }

  private User authenticatedUser() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser();
  }

  private Context configureContext(
      Invoice invoice, AccountHolder accountHolder, String customEmailBody) {
    Context context = new Context();

    context.setVariable("invoice", invoice);
    context.setVariable("user", authenticatedUser());
    context.setVariable("type", getStatusValue(invoice.getStatus()));
    context.setVariable("customEmailBody", customEmailBody);
    context.setVariable("accountHolder", accountHolder);

    return context;
  }
}
