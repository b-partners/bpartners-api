package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProductService {
  private final ProductRepository repository;
  private final InvoiceRepository invoiceRepository;
  private final AccountService accountService;     //TODO: remove when SelfMatcher is set

  public List<Product> getProductsByAccount(String accountId, String description, Boolean unique) {
    //TODO: remove when SelfMatcher is set
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    if (description != null) {
      return repository.findByIdAccountAndDescription(accountId, description);
    }
    if (unique == null) {
      throw new BadRequestException("Query parameter `unique` is mandatory.");
    }
    return repository.findByIdAccount(accountId, unique);
  }

  public List<Product> createProducts(String accountId, String invoiceId, List<Product> toCreate) {
    //TODO: remove when SelfMatcher is set
    Account authenticatedAccount = accountService.getAccounts().get(0);
    if (!authenticatedAccount.getId().equals(accountId)) {
      throw new ForbiddenException();
    }
    Invoice associatedInvoice = invoiceRepository.getById(invoiceId);
    if (associatedInvoice == null) {
      throw new NotFoundException("Invoice." + invoiceId + " does not exist");
    }
    return repository.saveAll(accountId, toCreate.stream()
        .map(product -> {
          product.setInvoice(associatedInvoice);
          return product;
        })
        .collect(Collectors.toUnmodifiableList()));
  }
}
