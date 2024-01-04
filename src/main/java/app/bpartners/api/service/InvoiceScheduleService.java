package app.bpartners.api.service;

import static app.bpartners.api.model.PageFromOne.MIN_PAGE;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.User;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceScheduleService {
  private final UserService userService;
  private final InvoiceService invoiceService;

  void refreshInvoices() {
    List<User> users = userService.findAll();
    for (User u : users) {
      List<Invoice> invoices =
          invoiceService.getInvoices(
              u.getId(),
              new PageFromOne(MIN_PAGE),
              new BoundedPageSize(BoundedPageSize.MAX_SIZE),
              List.of(InvoiceStatus.CONFIRMED),
              ArchiveStatus.ENABLED,
              null,
              List.of());
      for (Invoice invoice : invoices) {
        if (invoice.getPaymentRegulations().stream()
            .anyMatch(
                payment -> {
                  PaymentRequest paymentRequest = payment.getPaymentRequest();
                  return paymentRequest.getStatus() == PaymentStatus.UNPAID;
                })) {
          try {
            invoiceService.crupdateInvoice(invoice);
            log.info("Invoice(id=" + invoice.getId() + ") refreshed");
          } catch (Exception e) {
            log.error(e.getMessage());
          }
        }
      }
    }
  }
}
