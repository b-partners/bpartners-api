package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.User;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.PAID;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;

@Component
@AllArgsConstructor
@Slf4j
public class InvoiceRefreshComponent {
  private final InvoiceService invoiceService;
  private final UserService userService;

  @PostConstruct
  void refreshInvoices() {
    List<User> users = userService.findAll().stream()
        .filter(user -> user.getStatus() == EnableStatus.ENABLED)
        .toList();
    AtomicInteger successful = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    users.forEach(user -> {
          List<Invoice> invoices = invoiceService.getInvoices(user.getId(),
              new PageFromOne(MIN_PAGE),
              new BoundedPageSize(MAX_SIZE),
              List.of(InvoiceStatus.CONFIRMED),
              ArchiveStatus.ENABLED,
              null,
              List.of());
          List<Invoice> invoicePaidPaymentReg = invoices.stream()
              .filter(invoice -> invoice.getPaymentRegulations().stream()
                  .allMatch(p -> p.getPaymentRequest().getStatus() == PAID))
              .toList();

          invoicePaidPaymentReg.forEach(invoice -> {
            invoice.setStatus(InvoiceStatus.PAID);
            try {
              invoiceService.crupdateInvoice(invoice);
              successful.getAndIncrement();
              log.info("{} refreshed successfully",
                  invoice.describe());
            } catch (Exception e) {
              failed.getAndIncrement();
              log.error("An error occurred during processing for invoice {}: {}",
                  invoice.describe(),
                  e.getMessage());
            }
          });
        }
    );
    log.info("{} invoices were refreshed successfully", successful.get());
    log.info("{} invoices failed to refresh", failed.get());
  }
}
