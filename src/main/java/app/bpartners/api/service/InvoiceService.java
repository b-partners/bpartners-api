package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedInvoiceCrupdated;
import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import java.util.List;

import app.bpartners.api.repository.jpa.model.HInvoice;
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
  private final InvoiceJpaRepository jpaRepository;
  private final AccountHolderService holderService;
  private final EventProducer eventProducer;

  public List<Invoice> getInvoices(String accountId, PageFromOne page,
                                   BoundedPageSize pageSize, InvoiceStatus status) {
    int pageValue = page.getValue() - 1;
    int pageSizeValue = pageSize.getValue();
    if (status != null) {
      return repository.findAllByAccountIdAndStatus(accountId, status, pageValue, pageSizeValue);
    }
    return repository.findAllByAccountId(accountId, pageValue, pageSizeValue);
  }

  public Invoice getById(String invoiceId) {
    return repository.getById(invoiceId);
  }

  public static String getRealReference(String ref) {
    if (ref == null) {
      return null;
    }
    if (ref.contains(DRAFT_REF_PREFIX)) {
      return ref.replace(DRAFT_REF_PREFIX, "");
    } else if (ref.contains(PROPOSAL_REF_PREFIX)) {
      return ref.replace(PROPOSAL_REF_PREFIX, "");
    }
    return ref;
  }
  public boolean hasAvailableReference(String accountId, String invoiceId, String reference, InvoiceStatus status) {
    if (reference == null) {
      return true;
    }
    List<HInvoice> actual = jpaRepository.findByIdAccountAndRefAndStatus(
        accountId, getRealReference(reference), status);
    if(!actual.isEmpty() && !actual.get(0).getId().equals(invoiceId)){
      return false;
    }else {
      return true;
    }
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Invoice crupdateInvoice(Invoice toCrupdate) {
    if (!accountHolder(toCrupdate).isSubjectToVat()) {
      toCrupdate.getProducts().forEach(
          product -> product.setVatPercent(new Fraction())
      );
    }
    Invoice invoice = repository.crupdate(toCrupdate);

    eventProducer.accept(List.of(toTypedEvent(invoice)));

    return invoice;
  }

  private TypedInvoiceCrupdated toTypedEvent(Invoice invoice) {
    return new TypedInvoiceCrupdated(InvoiceCrupdated.builder()
        .invoice(invoice)
        .accountHolder(null) //todo: use account holder service when async is set
        .logoFileId(null) //todo: use principalProvider when async is set again
        .build());
  }

  private AccountHolder accountHolder(Invoice toCrupdate) {
    return holderService.getAccountHolderByAccountId(toCrupdate.getAccount().getId());
  }
}