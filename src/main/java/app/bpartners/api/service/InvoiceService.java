package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.InvoiceRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class InvoiceService {
  public static final String INVOICE_TEMPLATE = "invoice";
  public static final String DRAFT_TEMPLATE = "draft";
  public static final String DRAFT_REF_PREFIX = "BROUILLON-";
  public static final String PROPOSAL_REF_PREFIX = "DEVIS-";
  private final InvoiceRepository repository;
  private final AccountHolderService holderService;

  public List<Invoice> getInvoices(
      String accountId, PageFromOne page, BoundedPageSize pageSize, InvoiceStatus status) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    if (status != null) {
      return repository.findAllByAccountIdAndStatus(accountId, status, pageValue, pageSizeValue);
    }
    return repository.findAllByAccountId(accountId, pageValue, pageSizeValue);
  }

  public Invoice getById(String invoiceId) {
    return repository.getById(invoiceId);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Invoice crupdateInvoice(Invoice toCrupdate) {
    subjectToVat(toCrupdate);
    return repository.crupdate(toCrupdate);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Invoice crupdateInvoiceProducts(String accountId, String invoiceId, InvoiceStatus status,
                                         List<InvoiceProduct> products) {
    if (products.isEmpty()) {
      throw new BadRequestException("It should contain at least one product.");
    }
    Invoice toBeCrupdated = repository.crupdateInvoiceProducts(accountId, invoiceId, status,
        products);
    subjectToVat(toBeCrupdated);
    return repository.crupdate(toBeCrupdated);
  }

  private void subjectToVat(Invoice toCrupdate) {
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(toCrupdate.getAccount().getId());
    if (!accountHolder.isSubjectToVat()) {
      toCrupdate.getProducts().forEach(
          product -> product.setVatPercent(new Fraction()));
    }
  }
}