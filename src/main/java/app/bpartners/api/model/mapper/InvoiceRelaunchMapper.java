package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import app.bpartners.api.service.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoiceRelaunchMapper {
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final InvoiceService invoiceService;

  public InvoiceRelaunch toDomain(HInvoiceRelaunch entity) {
    Invoice invoice = invoiceService.getById(entity.getInvoice().getId());
    return InvoiceRelaunch.builder()
        .id(entity.getId())
        .invoice(invoice)
        .accountId(entity.getInvoice().getIdAccount())
        .isUserRelaunched(entity.isUserRelaunched())
        .creationDatetime(entity.getCreationDatetime())
        .build();
  }

  public HInvoiceRelaunch toEntity(String invoiceId) {
    HInvoice invoice = invoiceJpaRepository.getById(invoiceId);
    return HInvoiceRelaunch.builder()
        .invoice(invoice)
        .isUserRelaunched(true)
        .build();
  }

}
