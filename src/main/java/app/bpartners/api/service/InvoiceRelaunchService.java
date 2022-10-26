package app.bpartners.api.service;

import app.bpartners.api.model.InvoiceRelaunchConf;
import app.bpartners.api.repository.InvoiceRelaunchConfRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceRelaunchService {
  private final InvoiceRelaunchConfRepository repository;

  public InvoiceRelaunchConf getByAccountId(String accountId) {
    return repository.getByAccountId(accountId);
  }

  public InvoiceRelaunchConf save(InvoiceRelaunchConf invoiceRelaunchConf, String accountId) {
    return repository.save(invoiceRelaunchConf, accountId);
  }
}
