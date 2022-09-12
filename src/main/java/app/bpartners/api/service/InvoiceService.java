package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.InvoiceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceService {
  private final InvoiceRepository repository;
  private final AccountService accountService;

  private final ProductService productService;

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

  private Invoice computeInvoiceValues(Invoice invoice) {
    List<Product> products = invoice.getProducts().stream()
        .map(productService::resetProductRelatedInfo)
        .collect(Collectors.toUnmodifiableList());
    return Invoice.builder()
        .id(invoice.getId())
        .ref(invoice.getRef())
        .products(products)
        .grossAmount(productService.computeGrossAmount(products))
        .totalAmount(computeTotalAmount(invoice, products))
        .invoiceDate(invoice.getInvoiceDate())
        .toPayAt(invoice.getToPayAt())
        .invoiceDate(checkInvoiceDate(invoice.getInvoiceDate()))
        .build();
  }

  private int computeTotalAmount(Invoice invoice, List<Product> products) {
    int grossAmount = productService.computeGrossAmount(products);
    int globalReduction = 0;
    int amountReduction = invoice.getAmountReduction();
    int percentageReduction = invoice.getPercentageReduction();
    if (percentageReduction != 0) {
      globalReduction = grossAmount * (100 + invoice.getPercentageReduction() / 100);
    }
    if (amountReduction != 0) {
      globalReduction = amountReduction;
      if (amountReduction < 0) {
        throw new BadRequestException("Amount reduction can not be above the gross amount");
      }
    }
    int taxableAmount = grossAmount - globalReduction;
    int vatAmount = taxableAmount * invoice.getVat() / 100;
    return taxableAmount - vatAmount;
  }

  private LocalDate checkInvoiceDate(LocalDate invoiceDate) {
    LocalDate today = LocalDate.now();
    if (invoiceDate.isAfter(today)) {
      throw new BadRequestException(
          "Invoice date should be equals or before today " + today.toString());
    }
    return invoiceDate;
  }
}
