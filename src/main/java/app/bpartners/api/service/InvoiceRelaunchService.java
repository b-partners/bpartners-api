package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.CustomEmail;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.InvoiceRelaunchConfRepository;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;

@Service
@EnableScheduling
@AllArgsConstructor
public class InvoiceRelaunchService {
  private final InvoiceRelaunchConfRepository repository;
  private final InvoiceRelaunchRepository invoiceRelaunchRepository;
  private final InvoiceService invoiceService;

  public InvoiceRelaunchConf getByAccountId(String accountId) {
    return repository.getByAccountId(accountId);
  }

  public InvoiceRelaunchConf saveInvoiceRelaunchConf(InvoiceRelaunchConf invoiceRelaunchConf,
                                                     String accountId) {
    return repository.save(invoiceRelaunchConf, accountId);
  }

  public InvoiceRelaunch relaunchInvoice(String invoiceId, CustomEmail customEmail) {
    invoiceService.sendInvoice(invoiceId, customEmail.getSubject(), customEmail.getBody());
    return invoiceRelaunchRepository.save(invoiceId);
  }

  public InvoiceRelaunch relaunchInvoice(String invoiceId) {
    invoiceService.sendInvoice(invoiceId, null, null);
    return invoiceRelaunchRepository.save(invoiceId);
  }

  public List<InvoiceRelaunch> getRelaunchByInvoiceIdAndCriteria(
      String invoiceId,
      Boolean isUserRelaunched,
      PageFromOne page,
      BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return invoiceRelaunchRepository.getInvoiceRelaunchesByInvoiceIdAndCriteria(invoiceId,
        isUserRelaunched, pageable);
  }

  @Scheduled(cron = "0 0 6-18 * * *")
  private void thread() {
    invoiceService.getAllInvoices().forEach(
        invoice -> {
          InvoiceRelaunchConf relaunchConf = getByAccountId(invoice.getAccount().getId());
          boolean canBeRelaunched = getRelaunchByInvoiceIdAndCriteria(
              invoice.getAccount().getId(), false, new PageFromOne(1), new BoundedPageSize(500)
          ).isEmpty();
          if (
              canBeRelaunched
                  &&
                  (invoice.getStatus().equals(PROPOSAL)
                      ||
                      invoice.getStatus().equals(CONFIRMED)
                  )
          ) {
            LocalDate date = LocalDate.ofInstant(invoice.getUpdatedAt(), ZoneId.systemDefault());
            if (invoice.getStatus().equals(PROPOSAL)) {
              date = date.plusDays(relaunchConf.getDraftRelaunch());
            } else if (invoice.getStatus().equals(CONFIRMED)) {
              date = date.plusDays(relaunchConf.getUnpaidRelaunch());
            }
            if (LocalDate.now().isEqual(date)) {
              //TODO: check if the default subject and email message is override
              relaunchInvoice(invoice.getId());
            }
          }
        }
    );
  }
}
