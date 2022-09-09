package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRestMapper;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.service.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class InvoiceController {
  private final InvoiceRestMapper mapper;
  private final InvoiceService service;

  @PutMapping("/accounts/{aId}/invoices/{iId}")
  public Invoice crupdateInvoice(
      @PathVariable("aId") String accountId,
      @PathVariable("iId") String invoiceId,
      @RequestBody CrupdateInvoice crupdateInvoice) {
    app.bpartners.api.model.Invoice toCrupdate = mapper.toDomain(accountId, invoiceId,
        crupdateInvoice);
    return mapper.toRest(service.crupdateInvoice(accountId, toCrupdate));
  }

  @GetMapping("/accounts/{aId}/invoices/{iId}")
  public Invoice getInvoice(
      @PathVariable("aId") String accountId,
      @PathVariable("iId") String invoiceId) {
    return mapper.toRest(service.getById(accountId, invoiceId));
  }
}
