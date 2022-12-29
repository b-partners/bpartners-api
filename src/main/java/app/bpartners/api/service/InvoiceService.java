package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedInvoiceCrupdated;
import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.validator.InvoiceValidator;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
public class InvoiceService {
  public static final String INVOICE_TEMPLATE = "invoice";
  public static final String DRAFT_TEMPLATE = "draft";
  public static final String DRAFT_REF_PREFIX = "DRAFT-";
  private final InvoiceRepository repository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final AccountHolderService holderService;
  private final PrincipalProvider auth;
  private final InvoiceValidator validator;
  private final EventProducer eventProducer;
  private final InvoicePdfUtils pdfUtils = new InvoicePdfUtils();
  private final FileService fileService;

  public List<Invoice> getInvoices(String accountId, PageFromOne page, BoundedPageSize pageSize,
                                   InvoiceStatus status) {
    int pageValue = page.getValue() - 1;
    int pageSizeValue = pageSize.getValue();
    List<Invoice> invoices = repository.findAllByAccountId(accountId, pageValue, pageSizeValue);
    if (status != null) {
      invoices = repository.findAllByAccountIdAndStatus(accountId, status,
          pageValue,
          pageSizeValue);
    }
    return invoices;
  }

  public Invoice getById(String invoiceId) {
    return repository.getById(invoiceId);
  }

  @Transactional
  public Invoice crupdateInvoice(Invoice toCrupdate) {
    validator.accept(toCrupdate);

    Invoice invoice = repository.crupdate(toCrupdate);
    String accountId = invoice.getAccount().getId();
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(accountId);
    processPdfGeneration(pdfUtils, accountHolder, userLogoFileId(), invoice, accountId);
    eventProducer.accept(List.of(toTypedEvent(invoice, accountHolder)));

    return invoice;
  }

  private TypedInvoiceCrupdated toTypedEvent(Invoice invoice, AccountHolder accountHolder) {
    return new TypedInvoiceCrupdated(InvoiceCrupdated.builder()
        .invoice(invoice)
        .accountHolder(accountHolder)
        .logoFileId(userLogoFileId())
        .build());
  }

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }

  @Transactional
  public void processPdfGeneration(
      InvoicePdfUtils pdfUtils, AccountHolder accountHolder, String logoFileId,
      Invoice invoice, String accountId) {
    String fileId =
        invoice.getFileId() == null ? randomUUID() + PDF_EXTENSION : invoice.getFileId();
    //download logo if exist
    byte[] logoAsBytes = fileService.downloadOptionalFile(LOGO, accountId, logoFileId);
    //generate invoice pdf file
    byte[] fileAsBytes =
        invoice.getStatus().equals(CONFIRMED) || invoice.getStatus().equals(PAID)
            ? pdfUtils.generatePdf(invoice, accountHolder, logoAsBytes, INVOICE_TEMPLATE)
            : pdfUtils.generatePdf(invoice, accountHolder, logoAsBytes, DRAFT_TEMPLATE);
    //upload generated pdf file
    FileInfo fileInfo = fileService.upload(fileId, INVOICE, accountId, fileAsBytes, null);
    //set new invoice fileID
    invoice.setFileId(fileInfo.getId());
    //persist new invoice fileID
    try {
      invoiceJpaRepository.save(HInvoice.builder()
          .id(invoice.getId())
          .fileId(fileInfo.getId())
          .comment(invoice.getComment())
          .ref(invoice.getRealReference())
          .title(invoice.getTitle())
          .idAccount(invoice.getAccount().getId())
          .sendingDate(invoice.getSendingDate())
          .updatedAt(Instant.now())
          .toPayAt(invoice.getToPayAt())
          .createdDatetime(invoice.getCreatedAt())
          .status(invoice.getStatus())
          .toBeRelaunched(invoice.isToBeRelaunched())
          .metadataString(new ObjectMapper().writeValueAsString(invoice.getMetadata()))
          .build());
    } catch (JsonProcessingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}