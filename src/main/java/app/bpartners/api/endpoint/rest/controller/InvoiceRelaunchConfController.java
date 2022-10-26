package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRelaunchRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunchConf;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunchConf;
import app.bpartners.api.service.InvoiceRelaunchService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class InvoiceRelaunchConfController {
  private final InvoiceRelaunchService service;
  private final InvoiceRelaunchRestMapper mapper;

  @GetMapping("/accounts/{aId}/invoiceRelaunchConf")
  public InvoiceRelaunchConf getInvoiceRelaunch(@PathVariable("aId") String accountId) {
    return mapper.toRest(service.getByAccountId(accountId));
  }

  @PutMapping("/accounts/{aId}/invoiceRelaunchConf")
  public InvoiceRelaunchConf relaunchInvoice(
      @PathVariable("aId") String accountId,
      @RequestBody CreateInvoiceRelaunchConf toCreate) {
    return mapper.toRest(
        service.save(mapper.toDomain(toCreate), accountId));
  }
}
