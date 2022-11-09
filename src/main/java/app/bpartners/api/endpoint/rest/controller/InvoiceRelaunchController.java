package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRelaunchRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.endpoint.rest.model.InvoiceRelaunch;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.InvoiceRelaunchService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class InvoiceRelaunchController {
  private final InvoiceRelaunchService service;
  private final InvoiceRelaunchRestMapper mapper;

  @GetMapping(value = "/accounts/{aId}/invoices/{iId}/relaunches")
  public List<InvoiceRelaunch> getRelaunches(
      @PathVariable("aId") String accountId,
      @PathVariable("iId") String invoiceId,
      @RequestParam(name = "page", defaultValue = "1") PageFromOne page,
      @RequestParam(value = "pageSize", defaultValue = "10") BoundedPageSize pageSize,
      @RequestParam(name = "type", required = false) String type
  ) {
    return service.getRelaunchesByInvoiceId(invoiceId, page, pageSize, type)
        .stream().map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping(value = "/accounts/{aId}/invoices/{iId}/relaunch")
  public InvoiceRelaunch relaunchInvoice(
      @PathVariable("aId") String accountId,
      @PathVariable("iId") String invoiceId,
      @RequestBody CreateInvoiceRelaunch createInvoiceRelaunch) {
    return mapper.toRest(
        service.relaunchInvoice(invoiceId, createInvoiceRelaunch.getSubject(),
            createInvoiceRelaunch.getMessage()));
  }
}
