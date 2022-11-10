package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import app.bpartners.api.service.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.RelaunchType.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.RelaunchType.PROPOSAL;

@Component
@AllArgsConstructor
public class InvoiceRelaunchMapper {
  private final InvoiceMapper invoiceMapper;
  private final InvoiceService invoiceService;

  public InvoiceRelaunch toDomain(HInvoiceRelaunch entity) {
    Invoice invoice = invoiceService.getById(entity.getInvoice().getId());
    return InvoiceRelaunch.builder()
        .id(entity.getId())
        .type(entity.getType())
        .invoice(invoice)
        .accountId(entity.getInvoice().getIdAccount())
        .isUserRelaunched(entity.isUserRelaunched())
        .creationDatetime(entity.getCreationDatetime())
        .build();
  }

  public HInvoiceRelaunch toEntity(Invoice invoice) {
    HInvoiceRelaunch toSave = HInvoiceRelaunch.builder()
        .invoice(invoiceMapper.toEntity(invoice))
        .isUserRelaunched(true)
        .type(PROPOSAL)
        .build();
    if (invoice.getStatus().equals(InvoiceStatus.CONFIRMED)) {
      toSave.setType(CONFIRMED);
    }
    return toSave;
  }

}
