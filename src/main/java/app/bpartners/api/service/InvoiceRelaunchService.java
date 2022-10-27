package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.InvoiceRelaunchConfRepository;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceRelaunchService {
  private final InvoiceRelaunchConfRepository repository;
  private final InvoiceRelaunchRepository invoiceRelaunchRepository;

  public InvoiceRelaunchConf getByAccountId(String accountId) {
    return repository.getByAccountId(accountId);
  }

  public InvoiceRelaunchConf save(InvoiceRelaunchConf invoiceRelaunchConf, String accountId) {
    return repository.save(invoiceRelaunchConf, accountId);
  }

  public InvoiceRelaunch save(String invoiceId) {
    return invoiceRelaunchRepository.save(invoiceId);
  }

  public List<InvoiceRelaunch> getRelaunchByInvoiceId(
      String invoiceId,
      PageFromOne page,
      BoundedPageSize pageSize) {
    Pageable pageable = PageRequest.of(page.getValue() - 1, pageSize.getValue());
    return invoiceRelaunchRepository.getInvoiceRelaunchesByInvoiceId(invoiceId, pageable);
  }

}
