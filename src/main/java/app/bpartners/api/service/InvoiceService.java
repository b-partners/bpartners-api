package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repository;
  private final ProductRepository productRepository;
  private final AccountService accountService;    //TODO: remove when SelfMatcher is set

  public Invoice getById(String accountId, String invoiceId) {
    //TODO: remove when SelfMatcher is set
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    return refreshValues(repository.getById(invoiceId));
  }

  public Invoice crupdateInvoice(String accountId, Invoice toCrupdate) {
    //TODO: remove when SelfMatcher is set
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    return refreshValues(repository.crupdate(toCrupdate));
  }

  private Invoice refreshValues(Invoice invoice) {
    return Invoice.builder()
        .id(invoice.getId())
        .customer(invoice.getCustomer())
        .account(invoice.getAccount())
        .status(invoice.getStatus())
        .sendingDate(invoice.getSendingDate())
        .vat(invoice.getVat())
        .title(invoice.getTitle())
        .toPayAt(invoice.getToPayAt())
        .ref(invoice.getRef())
        .products(productRepository.findRecentByIdAccountAndInvoice(invoice.getAccount().getId(),
            invoice.getId()))
        .build();
  }
}
