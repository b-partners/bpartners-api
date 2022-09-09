package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.InvoiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repository;
  private final AccountService accountService;

  public Invoice getById(String accountId, String invoiceId) {
    //TODO: put in validator
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    return repository.getById(invoiceId);
  }

  public Invoice crupdateInvoice(String accountId, Invoice toCrupdate) {
    //TODO: put in validator
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    return repository.crupdate(toCrupdate);
  }
}
