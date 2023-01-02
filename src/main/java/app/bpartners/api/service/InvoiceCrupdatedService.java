package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class InvoiceCrupdatedService implements Consumer<InvoiceCrupdated> {
  public static final String INVOICE_TEMPLATE = "invoice";
  public static final String DRAFT_TEMPLATE = "draft";
  private final InvoiceService invoiceService;

  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Override
  public void accept(InvoiceCrupdated invoiceCrupdated) {
    InvoicePdfUtils pdfUtils = new InvoicePdfUtils();
    AccountHolder accountHolder = invoiceCrupdated.getAccountHolder();
    String logoFileId = invoiceCrupdated.getLogoFileId();
    Invoice invoice = invoiceCrupdated.getInvoice();
    String accountId = invoice.getAccount().getId();

    //invoiceService.processPdfGeneration(pdfUtils, accountHolder, logoFileId, invoice, accountId);
  }
}
