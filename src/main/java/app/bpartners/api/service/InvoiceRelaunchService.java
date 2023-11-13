package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserInvoiceRelaunchConf;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.InvoiceRelaunchValidator;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserInvoiceRelaunchConfRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import app.bpartners.api.service.utils.TemplateResolverUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
public class InvoiceRelaunchService {
  public static final String MAIL_TEMPLATE = "mail";
  private final UserInvoiceRelaunchConfRepository repository;
  private final InvoiceRelaunchRepository invoiceRelaunchRepository;
  private final InvoiceRelaunchValidator invoiceRelaunchValidator;
  private final InvoiceRepository invoiceRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final InvoiceRelaunchConfService relaunchConfService;
  private final AccountHolderService holderService;
  private final EventProducer eventProducer;
  private final PrincipalProvider auth;
  private final FileService fileService;
  private final AttachmentService attachmentService;
  private final EventConf eventConf;
  private final InvoicePdfUtils pdfUtils = new InvoicePdfUtils();
  private final SesService sesService;

  private static String getDefaultSubject(Invoice invoice) {
    return "Votre " + getStatusValue(invoice.getStatus())
        + ", portant la référence " + invoice.getRef() + ", au nom de "
        + invoice.getCustomer().getFirstName().toUpperCase() + " "
        + invoice.getCustomer().getLastName().toUpperCase();
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

  public UserInvoiceRelaunchConf getByIdUser(String idUser) {
    return repository.getByIdUser(idUser);
  }

  public UserInvoiceRelaunchConf saveConf(
      String idUser, UserInvoiceRelaunchConf relaunchConf) {
    return repository.save(idUser, relaunchConf);
  }

  @Transactional
  public void restartLastRelaunch(List<String> invoiceIds) {
    invoiceIds.forEach(invoiceId -> {
      List<InvoiceRelaunch> invoiceRelaunches =
          invoiceRelaunchRepository.getByInvoiceId(invoiceId, null, Pageable.ofSize(MAX_SIZE));
      InvoiceRelaunch invoiceRelaunch = invoiceRelaunches.stream()
          .sorted(Comparator.comparing(InvoiceRelaunch::getCreationDatetime).reversed())
          .toList()
          .get(0);

      String newEmailObject = fixEmailObject(invoiceRelaunch);

      relaunchInvoiceManually(invoiceId,
          List.of(newEmailObject),
          List.of(invoiceRelaunch.getEmailBody()),
          invoiceRelaunch.getAttachments(),
          true);
    });
  }

  private String fixEmailObject(InvoiceRelaunch invoiceRelaunch) {
    String emailObject = invoiceRelaunch.getEmailObject();
    String uniquePrefix = removeDuplicateBrackets(emailObject);
    int lastPrefixIndex = emailObject.lastIndexOf(uniquePrefix);
    String objectWithoutPrefix = emailObject.substring(lastPrefixIndex + uniquePrefix.length());
    return uniquePrefix + objectWithoutPrefix;
  }

  @Transactional
  public InvoiceRelaunch relaunchInvoiceManually(String invoiceId,
                                                 List<String> emailObjectList,
                                                 List<String> emailBodyList,
                                                 List<Attachment> attachments,
                                                 boolean fromScratch) {
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
    if (invoice.getArchiveStatus().equals(DISABLED)) {
      throw new BadRequestException("Invoice." + invoice.getId() + " is already DISABLED.");
    }
    invoiceRelaunchValidator.accept(invoice);

    boolean isUserRelaunched = true;
    AccountHolder accountHolder =
        holderService.getDefaultByAccountId(invoice.getActualAccount().getId());

    InvoiceRelaunch invoiceRelaunch =
        invoiceRelaunchRepository.save(
            invoice, emailObject, emailBody,
            isUserRelaunched);
    attachments.forEach(
        attachment -> uploadAttachment(invoice.getUser().getId(), attachment));
    List<Attachment> attachmentList =
        attachmentService.saveAll(attachments, invoiceRelaunch.getId());
    invoiceRelaunch.setAttachments(attachmentList);

    String subject = emailObject == null
        ? getDefaultSubject(invoice) : emailObject;
    String recipient = invoice.getCustomer().getEmail();
    String concerned = invoice.getUser().getDefaultHolder().getEmail();
    String invisibleConcerned = eventConf.getAdminEmail();
    String attachmentName = invoice.getRef() + PDF_EXTENSION;
    String htmlBody = emailBody(emailBody, invoice, accountHolder, fromScratch);
    InvoiceRelaunchSavedService.relaunchInvoiceAction(
        recipient,
        concerned,
        invisibleConcerned,
        subject,
        htmlBody,
        attachmentName,
        new ArrayList<>(attachmentList),
        invoice,
        accountHolder,
        invoice.getUser().getLogoFileId(),
        fileService,
        pdfUtils,
        sesService
    );
    /*
    /!\ Relaunch invoice synchronously for now
    eventProducer.accept(List.of(
        getTypedInvoiceRelaunched(
            invoiceRelaunch.getInvoice(),
            accountHolder,
            emailObject,
            emailBody,
            attachments,
            fromScratch)));*/

    return invoiceRelaunch;
  }

  private void uploadAttachment(String idUser, Attachment attachment) {
    FileInfo fileInfo = fileService.upload(
        randomUUID().toString(),
        ATTACHMENT,
        idUser,
        attachment.getContent()
    );
    attachment.setFileId(fileInfo.getId());
  }

  private Attachment deleteAttachmentContent(Attachment attachment) {
    //Clone attachment
    return Attachment.builder()
        .name(attachment.getName())
        .fileId(attachment.getFileId())
        .content(null)
        .build();
  }

  @Scheduled(cron = Scheduled.CRON_DISABLED)
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
            List<InvoiceRelaunch> invoiceRelaunches =
                getRelaunchesByInvoiceId(invoice.getId(), null,
                    new PageFromOne(1),
                    new BoundedPageSize(MAX_SIZE));
            int size = invoiceRelaunches.size();
            boolean notReachedMaxRehearse = size < conf.getRehearsalNumber();
            if (notReachedMaxRehearse) {
              //TODO: relaunch invoice with attachments
              relaunchInvoiceManually(invoice.getId(), List.of(), List.of(), List.of(), false);
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
      String invoiceId, String type,
      PageFromOne page, BoundedPageSize pageSize) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    Pageable pageable = PageRequest.of(pageValue, pageSizeValue);
    List<InvoiceRelaunch> invoiceRelaunches =
        invoiceRelaunchRepository.getByInvoiceId(invoiceId, type, pageable);
    invoiceRelaunches.forEach(invoiceRelaunch -> {
      String newEmailObject = fixEmailObject(invoiceRelaunch);
      invoiceRelaunch.setEmailObject(newEmailObject);
    });
    return invoiceRelaunches;
  }

  private String emailBody(String customEmailBody,
                           Invoice invoice,
                           AccountHolder accountHolder,
                           boolean fromScratch) {
    Context context = new Context();

    context.setVariable("invoice", invoice);
    context.setVariable("user", invoice.getUser());
    context.setVariable("type", getStatusValue(invoice.getStatus()));
    context.setVariable("customEmailBody", customEmailBody);
    context.setVariable("accountHolder", accountHolder);
    context.setVariable("isFromScratch", fromScratch);

    return TemplateResolverUtils.parseTemplateResolver(MAIL_TEMPLATE, context);
  }

  private TypedInvoiceRelaunchSaved getTypedInvoiceRelaunched(
      Invoice invoice,
      AccountHolder accountHolder,
      String subject,
      String customEmailBody,
      List<Attachment> attachments,
      boolean fromScratch) {
    //TODO: if invoice has already been relaunched then change this
    subject = subject == null ? getDefaultSubject(invoice) : subject;
    String recipient = invoice.getCustomer().getEmail();

    return toTypedEvent(
        recipient,
        subject,
        emailBody(customEmailBody, invoice, accountHolder, fromScratch),
        invoice.getRef() + PDF_EXTENSION,
        invoice,
        accountHolder,
        attachments.stream()
            .map(this::deleteAttachmentContent)
            .toList()
    );
  }

  private TypedInvoiceRelaunchSaved toTypedEvent(String recipient,
                                                 String subject,
                                                 String emailBody,
                                                 String attachmentName,
                                                 Invoice invoice,
                                                 AccountHolder accountHolder,
                                                 List<Attachment> attachments) {
    return new TypedInvoiceRelaunchSaved(InvoiceRelaunchSaved.builder()
        .subject(subject)
        .recipient(recipient)
        .htmlBody(emailBody)
        .attachmentName(attachmentName)
        .invoice(invoice)
        .accountHolder(accountHolder)
        .logoFileId(userLogoFileId())
        .attachments(attachments)
        .build());
  }

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }

  private User authenticatedUser() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser();
  }

  private String removeDuplicateBrackets(String input) {
    String regex = "\\[(.+?)]";
    Matcher matcher = Pattern.compile(regex).matcher(input);
    Set<String> uniqueMatches = new LinkedHashSet<>();

    while (matcher.find()) {
      uniqueMatches.add(matcher.group(1));
    }

    StringBuilder result = new StringBuilder();
    boolean first = true;

    for (String match : uniqueMatches) {
      if (!first) {
        result.append(' ');
      }
      result.append('[').append(match).append(']');
      first = false;
    }

    return result.toString().trim();
  }
}
