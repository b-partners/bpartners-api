package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRestMapper;
import app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.InvoiceService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class InvoiceController {
  private final InvoiceRestMapper mapper;
  private final InvoiceService service;

  @PutMapping("/accounts/{id}/invoices/{iId}")
  public Invoice crupdateInvoice(
      @PathVariable("id") String accountId,
      @PathVariable("iId") String invoiceId,
      @RequestBody CrupdateInvoice crupdateInvoice) {
    app.bpartners.api.model.Invoice toCrupdate =
        mapper.toDomain(accountId, invoiceId, crupdateInvoice);
    return mapper.toRest(service.crupdateInvoice(toCrupdate));
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
      @RequestParam(name = "status", required = false) InvoiceStatus status) {
    return service.getInvoices(accountId, page, pageSize, status).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{aId}/invoices/{iId}/paymentRegulations")
  public Invoice crupdateInvoicePayments(
      @PathVariable("aId") String accountId,
      @PathVariable("iId") String invoiceId,
      @RequestParam("status") InvoiceStatus status,
      @RequestBody List<CreatePaymentRegulation> paymentRegulations
  ) {
    return mapper.toRest(service.crupdatePayments(accountId, invoiceId, status,
        paymentRegulations));
  }

}
