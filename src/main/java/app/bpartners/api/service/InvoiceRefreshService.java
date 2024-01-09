package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.repository.implementation.InvoiceRepositoryImpl;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;

@Service
@AllArgsConstructor
@Slf4j
@EnableTransactionManagement
public class InvoiceRefreshService {
  private final InvoiceService invoiceService;
  private final UserService userService;
  private final PaymentRequestRepository paymentRequestRepository;
  private final InvoiceRepositoryImpl invoiceRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;

  @Transactional
  public void refreshInvoices() {
    List<User> users = userService.findAll().stream()
        .filter(user -> user.getStatus() == EnableStatus.ENABLED)
        .toList();
    AtomicInteger successful = new AtomicInteger();
    AtomicInteger failed = new AtomicInteger();
    users.forEach(user -> {
          List<Invoice> invoices = invoiceService.getInvoices(user.getId(),
              new PageFromOne(MIN_PAGE),
              new BoundedPageSize(MAX_SIZE),
              List.of(InvoiceStatus.PAID),
              ArchiveStatus.ENABLED,
              null,
              List.of());
          List<Invoice> unpaidRegInvoices = invoices.stream()
              .filter(invoice ->
              {
                boolean updatedAtDeploy =
                    invoice.getUpdatedAt().isAfter(Instant.parse("2024-01-04T16:00:32.203Z"))
                        && invoice.getUpdatedAt().isBefore(Instant.parse("2024-01-04T16:10:32.203Z"));
                PaymentTypeEnum paymentType = invoice.getPaymentType();
                List<PaymentRequest> payments = paymentType == PaymentTypeEnum.CASH
                    ? paymentRequestRepository.findAllByReference(invoice.getRealReference())
                    : List.of();
                return
                    updatedAtDeploy &&
                        (paymentType == PaymentTypeEnum.IN_INSTALMENT
                            && invoice.getPaymentRegulations().stream()
                            .anyMatch(p -> p.getPaymentRequest().getStatus() == UNPAID)
                            || paymentType == PaymentTypeEnum.CASH
                            && !payments.isEmpty() && payments.stream()
                            .allMatch(paymentRequest -> paymentRequest.getStatus() == UNPAID));
              })
              .toList();

          unpaidRegInvoices.forEach(invoice -> {
            try {
              invoice.setStatus(InvoiceStatus.CONFIRMED);

              HInvoice invoiceEntity = invoiceJpaRepository.getById(invoice.getId());
              invoiceEntity.setStatus(InvoiceStatus.CONFIRMED);
              invoiceJpaRepository.save(invoiceEntity);

              invoiceRepository.processAsPdf(invoice);
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
