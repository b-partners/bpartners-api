package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.security.AuthProvider.getAuthenticatedUserId;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.InvoicesSummaryRestMapper;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceReference;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.InvoicesSummary;
import app.bpartners.api.endpoint.rest.model.PreSignedURL;
import app.bpartners.api.endpoint.rest.model.UpdateInvoiceArchivedStatus;
import app.bpartners.api.endpoint.rest.model.UpdatePaymentRegMethod;
import app.bpartners.api.endpoint.rest.validator.InvoiceReferenceValidator;
import app.bpartners.api.endpoint.rest.validator.UpdatePaymentRegValidator;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.InvoiceSummaryService;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class InvoiceController {
  private final InvoiceRestMapper mapper;
  private final InvoiceService service;
  private final InvoiceReferenceValidator referenceValidator;
  private final UpdatePaymentRegValidator paymentValidator;
  private final InvoicesSummaryRestMapper summaryRestMapper;
  private final InvoiceSummaryService summaryService;

  @GetMapping("/accounts/{aId}/invoices/exportLink")
  public PreSignedURL getInvoicesExportLink(
      @PathVariable("aId") String accountId,
      @RequestParam(name = "statusList", required = false) List<InvoiceStatus> statusList,
      @RequestParam(name = "archiveStatus", required = false) ArchiveStatus archiveStatus,
      @RequestParam(value = "from", required = false) LocalDate from,
      @RequestParam(value = "to", required = false) LocalDate to) {
    var preSignedLink =
        service.generateInvoicesExportLink(accountId, statusList, archiveStatus, from, to);
    return new PreSignedURL()
        .value(preSignedLink.getValue())
        .updatedAt(preSignedLink.getUpdatedAt())
        .expirationDelay(preSignedLink.getExpirationDelay());
  }

  @GetMapping("accounts/{aId}/invoicesSummary")
  public InvoicesSummary getInvoicesSummary(@PathVariable("aId") String accountId) {
    return summaryRestMapper.toRest(
        summaryService.getOrComputeLatestSummary(getAuthenticatedUserId()));
  }

  @PutMapping("/accounts/{id}/invoices/{iId}/paymentRegulations/{pId}/paymentMethod")
  public Invoice updatePaymentMethod(
      @PathVariable("id") String accountId,
      @PathVariable("iId") String invoiceId,
      @PathVariable("pId") String paymentId,
      @RequestBody(required = false) UpdatePaymentRegMethod updatePaymentRegMethod) {
    paymentValidator.accept(updatePaymentRegMethod);
    return mapper.toRest(
        service.updatePaymentStatus(invoiceId, paymentId, updatePaymentRegMethod.getMethod()));
  }

  @PutMapping("/accounts/{id}/invoices/{iId}")
  public Invoice crupdateInvoice(
      @PathVariable("id") String accountId,
      @PathVariable("iId") String invoiceId,
      @RequestBody CrupdateInvoice crupdateInvoice) {
    String idUser = getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    app.bpartners.api.model.Invoice domain = mapper.toDomain(idUser, invoiceId, crupdateInvoice);
    return mapper.toRest(retryCrupdate(domain));
  }

  @GetMapping("/accounts/{id}/invoices/{iId}")
  public Invoice getInvoice(
      @PathVariable("id") String accountId, @PathVariable("iId") String invoiceId) {
    return mapper.toRest(service.getById(invoiceId));
  }

  @GetMapping("/accounts/{aId}/invoices")
  public List<Invoice> getInvoices(
      @PathVariable(name = "aId") String accountId,
      @RequestParam(name = "page", required = false) PageFromOne page,
      @RequestParam(name = "pageSize", required = false) BoundedPageSize pageSize,
      @RequestParam(name = "status", required = false) InvoiceStatus status,
      @RequestParam(name = "statusList", required = false) List<InvoiceStatus> statusList,
      @RequestParam(name = "archiveStatus", required = false) ArchiveStatus archiveStatus,
      @RequestParam(name = "title", required = false) String title,
      @RequestParam(name = "filters", required = false) List<String> filters) {
    String idUser = getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    if (status != null && (statusList == null || statusList.isEmpty())) {
      log.warn("DEPRECATED: GET /accounts/{aId}/invoices query_param=status is still used");
      statusList = new ArrayList<>();
      statusList.add(status);
    }
    return service
        .getInvoices(idUser, page, pageSize, statusList, archiveStatus, title, filters)
        .stream()
        .map(mapper::toRest)
        .toList();
  }

  @PutMapping("/accounts/{aId}/invoices/archive")
  public List<Invoice> archiveInvoices(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<UpdateInvoiceArchivedStatus> toArchive) {
    List<ArchiveInvoice> archiveInvoices = toArchive.stream().map(mapper::toDomain).toList();
    return service.archiveInvoices(archiveInvoices).stream().map(mapper::toRest).toList();
  }

  @PostMapping("/accounts/{aId}/invoices/{iId}/duplication")
  public Invoice duplicateInvoice(
      @PathVariable String aId,
      @PathVariable String iId,
      @RequestBody(required = false) InvoiceReference invoiceReference) {
    referenceValidator.accept(invoiceReference);
    return mapper.toRest(service.duplicateAsDraft(iId, invoiceReference.getNewReference()));
  }

  // After refactoring invoice, remove
  @SneakyThrows
  private app.bpartners.api.model.Invoice retryCrupdate(app.bpartners.api.model.Invoice invoice) {
    int retries = 5;
    while (retries > 0) {
      try {
        return service.crupdateInvoice(invoice);
      } catch (Exception e) {
        Random random = new Random();
        Thread.sleep(Duration.ofSeconds((long) (1 + random.nextDouble() * 2)));
        retries--;
        if (retries == 0) {
          throw e;
        }
      }
    }
    throw new RuntimeException("Unable to crupdate invoice " + invoice);
  }
}
