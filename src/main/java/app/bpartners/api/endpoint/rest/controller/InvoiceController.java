package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRestMapper;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceReference;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.UpdateInvoiceArchivedStatus;
import app.bpartners.api.endpoint.rest.model.UpdatePaymentRegMethod;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.InvoiceReferenceValidator;
import app.bpartners.api.endpoint.rest.validator.UpdatePaymentRegValidator;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.InvoiceService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
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
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    app.bpartners.api.model.Invoice domain = mapper.toDomain(idUser, invoiceId, crupdateInvoice);
    return mapper.toRest(service.crupdateInvoice(domain));
  }

  @GetMapping("/accounts/{id}/invoices/{iId}")
  public Invoice getInvoice(
      @PathVariable("id") String accountId,
      @PathVariable("iId") String invoiceId) {
    return mapper.toRest(service.getById(invoiceId));
  }

  @GetMapping("/accounts/{aId}/invoices")
  public List<Invoice> getInvoices(
      @PathVariable(name = "aId") String accountId,
      @RequestParam(name = "page", required = false) PageFromOne page,
      @RequestParam(name = "pageSize", required = false) BoundedPageSize pageSize,
      @RequestParam(name = "status", required = false) InvoiceStatus status,
      @RequestParam(name = "statusList", required = false) List<InvoiceStatus> statusList,
      @RequestParam(name = "archiveStatus", required = false) ArchiveStatus archiveStatus) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    if (status != null && (statusList == null || statusList.isEmpty())) {
      log.warn("DEPRECATED: GET /accounts/{aId}/invoices query_param=status is still used");
      statusList = new ArrayList<>();
      statusList.add(status);
    }
    return service.getInvoices(idUser, page, pageSize, statusList, archiveStatus).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{aId}/invoices/archive")
  public List<Invoice> archiveInvoices(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<UpdateInvoiceArchivedStatus> toArchive) {
    List<ArchiveInvoice> archiveInvoices = toArchive.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return service.archiveInvoices(archiveInvoices).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/accounts/{aId}/invoices/{iId}/duplication")
  public Invoice duplicateInvoice(@PathVariable String aId,
                                  @PathVariable String iId,
                                  @RequestBody(required = false)
                                  InvoiceReference invoiceReference) {
    referenceValidator.accept(invoiceReference);
    return mapper.toRest(service.duplicateAsDraft(iId, invoiceReference.getNewReference()));
  }
}
