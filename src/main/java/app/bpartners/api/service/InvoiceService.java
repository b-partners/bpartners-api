package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
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

  public List<Invoice> getInvoices(
      String idUser, PageFromOne page, BoundedPageSize pageSize, InvoiceStatus status) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    if (status != null) {
      return repository.findAllByIdUserAndStatus(idUser, status, pageValue, pageSizeValue);
    }
    return repository.findAllByIdUser(idUser, pageValue, pageSizeValue);
  }

  public Invoice getById(String invoiceId) {
    return repository.getById(invoiceId);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Invoice crupdateInvoice(Invoice invoice) {
    if (!invoice.getActualHolder().isSubjectToVat()) {
      invoice.getProducts().forEach(
          product -> product.setVatPercent(new Fraction())
      );
    }
    return repository.crupdate(invoice);
  }

  public List<Invoice> archiveInvoices(List<Invoice> toUpdate) {
    toUpdate.forEach(repository::crupdate);
    return toUpdate;
  }
}