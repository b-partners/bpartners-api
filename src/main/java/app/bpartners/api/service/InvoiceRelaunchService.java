package app.bpartners.api.service;

import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.repository.InvoiceRelaunchRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceRelaunchService {
  private final InvoiceRelaunchRepository repository;

  public InvoiceRelaunch getByAccountId(String accountId) {
    return repository.getByAccountId(accountId);
  }

  public InvoiceRelaunch save(InvoiceRelaunch invoiceRelaunch, String accountId) {
    return repository.save(invoiceRelaunch, accountId);
  }
}
