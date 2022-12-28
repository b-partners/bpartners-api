package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.RelaunchType;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.jpa.model.HInvoiceRelaunch;
import app.bpartners.api.service.InvoiceService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

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
        .emailObject(entity.getObject())
        .emailBody(entity.getEmailBody())
        .attachmentFileId(entity.getAttachmentFileId())
        .build();
  }

  public HInvoiceRelaunch toEntity(
      Invoice invoice, String object, String htmlBody, boolean userRelaunched) {
    return HInvoiceRelaunch.builder()
        .invoice(invoiceMapper.toEntity(invoice))
        .isUserRelaunched(userRelaunched)
        .type(mapRelaunchType(invoice))
        .object(object)
        .emailBody(htmlBody)
        .attachmentFileId(invoice.getFileId())
        .build();
  }

  private RelaunchType mapRelaunchType(Invoice invoice) {
    switch (invoice.getStatus()) {
      case PROPOSAL:
        return RelaunchType.PROPOSAL;
      case CONFIRMED:
        return RelaunchType.CONFIRMED;
      default:
        throw new BadRequestException(
            "Invoice." + invoice.getId() + " with status " + invoice.getStatus() + "can not be "
                + "relaunched");
    }
  }
}
