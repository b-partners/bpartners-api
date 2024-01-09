package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.InvoiceRefreshService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class InvoiceRefreshController {
  private final InvoiceRefreshService invoiceRefreshService;

  @PostMapping("/invoicesRefresh")
  public void invoiceRefresh() {
    invoiceRefreshService.refreshInvoices();
  }
}
