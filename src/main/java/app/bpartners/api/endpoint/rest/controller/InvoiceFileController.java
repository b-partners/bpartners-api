package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class InvoiceFileController {
  private final InvoiceService service;

  @GetMapping("/invoices/file/{iId}")
  public String getInvoice(
          @PathVariable("iId") String invoiceId,
          Model model) {
    model.addAttribute("invoice", service.getById(invoiceId));
    return "REQUIRED FILE";
  }
}
